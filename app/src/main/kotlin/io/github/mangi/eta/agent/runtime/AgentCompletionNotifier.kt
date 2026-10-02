package io.github.mangi.eta.agent.runtime

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.mangi.eta.R
import io.github.mangi.eta.data.db.EtaDatabase
import kotlinx.coroutines.runBlocking

/**
 * Eta Mod：任务完成横幅通知（heads-up + 默认提示音 + 振动）。
 *
 * 标题 = 会话名（查不到时回退「已完成」），正文 = 助手回复摘要（展开为全文）。
 * 内部压缩任务、失败、手动停止均不通知（仅 COMPLETED）；通知权限关闭时静默空操作。
 * 与 [AgentIslandNotifier] 完全独立：岛走 eta_island 安静频道，本类走 eta_completion 响铃频道。
 */
internal object AgentCompletionNotifier {
    private const val CHANNEL_ID = "eta_completion"
    private const val BASE_NOTIFICATION_ID = 3400
    private const val TAG = "EtaCompletion"
    internal const val MAX_TEXT_CHARS = 500

    fun onTaskCompleted(
        context: Context,
        conversationId: String?,
        iconRes: Int,
        replyText: String,
        runId: String,
    ) {
        runCatching {
            val appContext = context.applicationContext
            if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return
            val title = conversationId
                ?.let { id ->
                    runCatching {
                        runBlocking { EtaDatabase.get(appContext).conversationDao().conversationTitle(id) }
                    }.getOrNull()
                }
                ?.takeIf { it.isNotBlank() }
                ?: appContext.getString(R.string.island_state_done)
            val text = snippet(replyText)
            post(appContext, title, text, iconRes, runId)
        }.onFailure { Log.w(TAG, "notify failed: ${it.javaClass.simpleName}: ${it.message}") }
    }

    /** 摘要规整：压掉多余空白，超长截断（抽成纯函数便于单元测试）。 */
    internal fun snippet(text: String, maxChars: Int = MAX_TEXT_CHARS): String {
        val collapsed = text.trim().replace(Regex("\\s+"), " ")
        if (collapsed.length <= maxChars) return collapsed
        return collapsed.take(maxChars).trimEnd() + "…"
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.completion_channel_name),
                NotificationManager.IMPORTANCE_HIGH, // heads-up 横幅
            ).apply {
                // 声音 + 振动显式开启（HIGH 频道默认即带，写出来防 OEM 差异）
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                setShowBadge(true)
            },
        )
    }

    @SuppressLint("MissingPermission") // 已检查 areNotificationsEnabled
    private fun post(context: Context, title: String, text: String, iconRes: Int, runId: String) {
        ensureChannel(context)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(Icon.createWithResource(context, iconRes))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setTicker("$title：$text")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 8.0 以下系统的 heads-up
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        NotificationManagerCompat.from(context).notify(notificationId(runId), builder.build())
        // release 构建会裁掉 Log.d（proguard maximumremovedandroidloglevel=3），用 INFO 保留可观测性
        Log.i(TAG, "posted title=$title run=$runId")
    }

    /** 每次任务完成独立一条通知，互不覆盖。 */
    private fun notificationId(runId: String): Int = BASE_NOTIFICATION_ID + (runId.hashCode() and 0xFFF)
}
