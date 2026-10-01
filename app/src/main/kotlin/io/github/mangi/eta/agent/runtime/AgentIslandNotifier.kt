package io.github.mangi.eta.agent.runtime

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.mangi.eta.R
import org.json.JSONObject

/**
 * Eta Mod：小米焦点通知 / 超级岛（官方协议：miui.focus.param + param_v2）。
 *
 * 模版 1：大岛 A 区图文组件 1（imageTextInfoLeft，type=1）= 厂商品牌图标 + 状态大字；
 * B 区留空；小岛 smallIslandArea 仅厂商图标。状态文案 ≤4 个中文字（官方规范）。
 * islandFirstFloat=false、enableFloat=false：安静胶囊，不自动展开。
 *
 * 非小米澎湃 OS 或焦点通知权限未开时全程静默空操作，退化为不发送；
 * 所有调用方都已包 runCatching，本类内部也不再抛出。
 */
internal object AgentIslandNotifier {
    private const val CHANNEL_ID = "eta_island"
    private const val NOTIFICATION_ID = 1207
    internal const val PIC_KEY = "miui.focus.pic_eta_island"
    private const val MIN_UPDATE_INTERVAL_MS = 1_500L
    private const val FINAL_DISMISS_MS = 8_000L

    enum class State { THINKING, OUTPUT, TOOL }

    enum class Terminal { COMPLETED, FAILED, STOPPED }

    enum class SupportStatus { CHECKING, SUPPORTED, NO_PERMISSION, UNSUPPORTED }

    /** 设置页状态行用：耗时调用，须在后台线程执行。 */
    fun querySupportStatus(context: Context): SupportStatus = runCatching {
        val focusProtocol = Settings.System.getInt(
            context.contentResolver,
            "notification_focus_protocol",
            0,
        )
        if (focusProtocol < 1) return SupportStatus.UNSUPPORTED
        if (hasFocusPermission(context)) SupportStatus.SUPPORTED else SupportStatus.NO_PERMISSION
    }.getOrDefault(SupportStatus.UNSUPPORTED)

    @Volatile private var gateAllowed = false
    @Volatile private var runActive = false
    @Volatile private var lastState: State? = null
    @Volatile private var lastPostedAt = 0L
    @Volatile private var lastIconRes = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingDismiss: Runnable? = null

    /** 每次任务开始重新探测（权限可能在系统设置里被改动），探测为耗时操作，须在后台线程调用。 */
    fun onRunStarted(context: Context, iconRes: Int) {
        val appContext = context.applicationContext
        gateAllowed = probeGate(appContext)
        if (!gateAllowed) return
        cancelPendingDismiss()
        runActive = true
        lastState = null
        lastIconRes = iconRes
        post(appContext, stateText(appContext, State.THINKING), ongoing = true)
        lastState = State.THINKING
    }

    /** 状态切换立即更新；同状态刷新按 1.5s 节流。 */
    fun onRunState(context: Context, state: State) {
        if (!runActive || !gateAllowed) return
        val changed = state != lastState
        val now = SystemClock.uptimeMillis()
        if (!changed && now - lastPostedAt < MIN_UPDATE_INTERVAL_MS) return
        val appContext = context.applicationContext
        post(appContext, stateText(appContext, state), ongoing = true)
        lastState = state
    }

    /** 终态展示约 8 秒后自动撤销，避免岛屿残留；未上岛的运行（如内部压缩任务）直接忽略。 */
    fun onRunFinished(context: Context, terminal: Terminal) {
        val wasActive = runActive
        runActive = false
        if (!wasActive || !gateAllowed) return
        val appContext = context.applicationContext
        post(appContext, terminalText(appContext, terminal), ongoing = false)
        lastState = null
        cancelPendingDismiss()
        val manager = NotificationManagerCompat.from(appContext)
        val dismiss = Runnable { runCatching { manager.cancel(NOTIFICATION_ID) } }
        pendingDismiss = dismiss
        mainHandler.postDelayed(dismiss, FINAL_DISMISS_MS)
    }

    // ── 内部实现 ────────────────────────────────────────────────────────

    private fun stateText(context: Context, state: State): String = context.getString(
        when (state) {
            State.THINKING -> R.string.island_state_thinking
            State.OUTPUT -> R.string.island_state_output
            State.TOOL -> R.string.island_state_tool
        },
    )

    private fun terminalText(context: Context, terminal: Terminal): String = context.getString(
        when (terminal) {
            Terminal.COMPLETED -> R.string.island_state_done
            Terminal.FAILED -> R.string.island_state_failed
            Terminal.STOPPED -> R.string.island_state_stopped
        },
    )

    private fun probeGate(context: Context): Boolean = runCatching {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return@runCatching false
        val focusProtocol = Settings.System.getInt(
            context.contentResolver,
            "notification_focus_protocol",
            0,
        )
        if (focusProtocol < 1) return@runCatching false // 1=OS1 2=OS2 3=OS3（岛仅 OS3，焦点参数向下兼容）
        hasFocusPermission(context)
    }.getOrDefault(false)

    /** 官方查询接口（耗时）：content://miui.statusbar.notification.public / canShowFocus。 */
    private fun hasFocusPermission(context: Context): Boolean = runCatching {
        val uri = Uri.parse("content://miui.statusbar.notification.public")
        val extras = Bundle().apply { putString("package", context.packageName) }
        val result = context.contentResolver.call(uri, "canShowFocus", null, extras)
        result?.getBoolean("canShowFocus", false) == true
    }.getOrDefault(false)

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.island_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
    }

    /** 岛参数 JSON（官方 param_v2 模版 1）；抽成纯函数便于单元测试。 */
    internal fun buildFocusParam(text: String, appName: String): String =
        JSONObject().apply {
            put("param_v2", JSONObject().apply {
                put("protocol", 1)
                put("business", "agent_task")
                put("islandFirstFloat", false)
                put("enableFloat", false)
                put("updatable", true)
                // OS2 状态栏焦点信息
                put("ticker", text)
                put("tickerPic", PIC_KEY)
                // 息屏显示
                put("aodTitle", text)
                // 焦点通知内容
                put("baseInfo", JSONObject().apply {
                    put("type", 1)
                    put("title", text)
                    put("content", appName)
                })
                put("param_island", JSONObject().apply {
                    put("islandProperty", 1)
                    put("bigIslandArea", JSONObject().apply {
                        put("imageTextInfoLeft", JSONObject().apply {
                            put("type", 1)
                            put("picInfo", JSONObject().apply {
                                put("type", 1)
                                put("pic", PIC_KEY)
                            })
                            put("textInfo", JSONObject().apply {
                                put("title", text)
                                put("showHighlightColor", false)
                            })
                        })
                    })
                    put("smallIslandArea", JSONObject().apply {
                        put("picInfo", JSONObject().apply {
                            put("type", 1)
                            put("pic", PIC_KEY)
                        })
                    })
                })
            })
        }.toString()

    @SuppressLint("MissingPermission") // gate 已检查 areNotificationsEnabled
    private fun post(context: Context, text: String, ongoing: Boolean) {
        ensureChannel(context)
        lastPostedAt = SystemClock.uptimeMillis()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val iconRes = lastIconRes.takeIf { it != 0 } ?: R.drawable.ic_notification
        val appName = context.getString(R.string.app_name)
        val pics = Bundle().apply {
            putParcelable(PIC_KEY, Icon.createWithResource(context, iconRes))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(appName)
            .setContentText(text)
            .setTicker(text)
            .setContentIntent(contentIntent)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addExtras(Bundle().apply {
                putBundle("miui.focus.pics", pics)
                putString("miui.focus.param", buildFocusParam(text, appName))
            })
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun cancelPendingDismiss() {
        pendingDismiss?.let(mainHandler::removeCallbacks)
        pendingDismiss = null
    }
}
