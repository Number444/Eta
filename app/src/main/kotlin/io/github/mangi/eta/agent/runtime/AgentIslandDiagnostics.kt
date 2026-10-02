package io.github.mangi.eta.agent.runtime

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.mangi.eta.R

/**
 * Eta Mod：岛通知诊断测试·第三轮（临时工具）——完整模拟目标链路，验证后再实装：
 *
 *   T20  安静胶囊「思考中」（新 LOW 频道 eta_island_run + 焦点参数 + 三件套）
 *   T21  同 id 更新「输出中」
 *   T22  同 id 更新「工具调用」
 *   T23  完成瞬间：撤销胶囊 → eta_island 频道【新 id 1208】首发「已完成」（预期：岛展开）
 *        + 同时 eta_completion 横幅（预期：横幅+声音+振动）
 *   T24  8s 后撤销完成岛（模拟生产终态自动消失）
 *
 * 验证点：进行中全程安静胶囊；完成瞬间岛展开与横幅/声/振同时出现；随后自动消失。
 */
internal object AgentIslandDiagnostics {
    private const val TAG = "EtaIslandTest"
    private const val CH_RUN_QUIET = "eta_island_run"
    private const val CH_ISLAND = "eta_island"
    private const val LEASE_ID = "island-diagnostics"
    private const val ID_RUN = 1207
    private const val ID_DONE = 1208

    // 历史测试频道，启动时一并清理
    private val LEGACY_CHANNELS = listOf(
        "eta_test_default_param",
        "eta_test_high_noparam",
        "eta_test_high_param",
        "eta_test2_low_param",
        "eta_test2_default_param",
        "eta_test2_high_param_sound",
        CH_RUN_QUIET,
    )

    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var running = false

    /** @return false = 上一轮还在跑。 */
    fun start(context: Context): Boolean {
        if (running) return false
        running = true
        val app = context.applicationContext
        // 前台服务租约保活：无前台组件时进程会被 MIUI 冻结，后续延迟步骤全部丢失。
        val leased = AgentExecutionService.acquire(app, LEASE_ID) {}
        if (!leased) Log.w(TAG, "execution lease rejected; diagnostics may freeze mid-run")
        cleanupTestChannels(app)
        Log.i(TAG, "==== diagnostics round 3 started ====")

        at(app, 5, "T20 安静胶囊 思考中（LOW 频道）") {
            postRunState(it, "思考中")
        }
        at(app, 11, "T21 同 id 更新 输出中") {
            postRunState(it, "输出中")
        }
        at(app, 17, "T22 同 id 更新 工具调用") {
            postRunState(it, "工具调用")
        }
        at(app, 23, "T23 完成：新 id 首发 已完成（eta_island）+ 横幅（eta_completion）") {
            // 撤销进行中胶囊
            runCatching { NotificationManagerCompat.from(it).cancel(ID_RUN) }
            // 岛展开：eta_island 新 id 首发（onlyAlertOnce 不影响新 id 的首次 alert）
            postIslandDone(it)
            // 横幅：走生产完成通知路径
            AgentCompletionNotifier.onTaskCompleted(
                it,
                conversationId = null,
                iconRes = R.drawable.ic_notification,
                replyText = "T23 完成横幅正文：应与岛展开同时出现",
                runId = "diag-t23",
            )
        }
        at(app, 31, "T24 撤销完成岛（模拟生产 8s 自动消失）") {
            runCatching { NotificationManagerCompat.from(it).cancel(ID_DONE) }
        }
        at(app, 34, "诊断结束") {
            AgentExecutionService.release(LEASE_ID)
            Log.i(TAG, "==== diagnostics round 3 finished ====")
            running = false
        }
        return true
    }

    // ── 内部实现 ────────────────────────────────────────────────────────

    private fun at(context: Context, atSec: Long, label: String, block: (Context) -> Unit) {
        handler.postDelayed({
            runCatching {
                Log.i(TAG, "step $label")
                block(context)
            }.onFailure { Log.w(TAG, "step failed: $label: ${it.javaClass.simpleName}: ${it.message}") }
        }, atSec * 1000)
    }

    private fun cleanupTestChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        LEGACY_CHANNELS.forEach { runCatching { manager.deleteNotificationChannel(it) } }
    }

    private fun ensureChannel(context: Context, channelId: String, importance: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(channelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(channelId, channelId, importance).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
    }

    private fun islandBuilder(context: Context, channelId: String, text: String): NotificationCompat.Builder {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        val contentIntent = PendingIntent.getActivity(
            context,
            3,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val appName = context.getString(R.string.app_name)
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(Icon.createWithResource(context, R.drawable.ic_notification))
            .setContentTitle(text)
            .setContentText(appName)
            .setTicker(text)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(text)
            .addExtras(Bundle().apply {
                putBundle("miui.focus.pics", Bundle().apply {
                    putParcelable(
                        AgentIslandNotifier.PIC_KEY,
                        Icon.createWithResource(context, R.drawable.ic_notification),
                    )
                })
                putString(
                    "miui.focus.param",
                    AgentIslandNotifier.buildFocusParam(text, appName, context.packageName),
                )
            })
    }

    /** 进行中胶囊：LOW 安静频道 + 同 id 更新，不展开。 */
    @SuppressLint("MissingPermission")
    private fun postRunState(context: Context, text: String) {
        ensureChannel(context, CH_RUN_QUIET, NotificationManager.IMPORTANCE_LOW)
        NotificationManagerCompat.from(context).notify(ID_RUN, islandBuilder(context, CH_RUN_QUIET, text).build())
    }

    /** 完成展开：eta_island（用户悬浮通知设置所在频道）+ 新 id 首发。 */
    @SuppressLint("MissingPermission")
    private fun postIslandDone(context: Context) {
        // 不 ensure：eta_island 已存在且带用户设置；不存在时按生产同规格(DEFAULT)重建
        ensureChannel(context, CH_ISLAND, NotificationManager.IMPORTANCE_DEFAULT)
        NotificationManagerCompat.from(context).notify(
            ID_DONE,
            islandBuilder(context, CH_ISLAND, context.getString(R.string.island_state_done)).build(),
        )
    }
}
