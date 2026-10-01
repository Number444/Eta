package io.github.mangi.eta.ui.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.rememberMarkdownState
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.ui.app.AgentAppTheme
import io.github.mangi.eta.ui.components.StableMarkdown
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Eta Mod：分享为长图。
 *
 * 仿照 dsh-share 插件的思路：不是截图拼接，而是把消息内容在一棵离屏视图中重新渲染成卡片
 * （用户提问块 + Markdown 正文 + 品牌页脚），再整体绘制成一张 PNG 分享出去。
 * Compose 离屏渲染必须依附窗口才会触发组合，因此把 INVISIBLE 的 ComposeView 临时挂到
 * decorView 上（不可见、不参与用户画面），排版完成后手动 draw 到 Bitmap 再移除。
 */
object MessageShareImage {

    /** 超长内容兜底：位图高度上限（约 8 屏），超出部分裁断。 */
    private const val MAX_BITMAP_HEIGHT_PX = 12_000

    private const val MARKDOWN_PARSE_TIMEOUT_MS = 3_000L

    /**
     * 渲染并弹出系统分享面板。必须在主线程调用。
     *
     * @param anchor 当前界面任意 View（用于取 decorView 与 Activity 上下文）
     * @param appearance 当前外观设置，离屏卡片用同一套主题渲染，保证明暗/配色一致
     */
    suspend fun shareTurnAsImage(
        anchor: View,
        appearance: AppearanceSettings,
        userPrompt: String?,
        answerMarkdown: String,
        brand: String,
    ) {
        if (answerMarkdown.isBlank()) return
        val context = anchor.context
        val decorView = anchor.rootView as? ViewGroup ?: return
        val widthPx = decorView.width.takeIf { it > 0 }
            ?: context.resources.displayMetrics.widthPixels
        if (widthPx <= 0) return

        val markdownReady = mutableStateOf(false)
        val composeView = ComposeView(context).apply {
            visibility = View.INVISIBLE
            setContent {
                AgentAppTheme(appearance = appearance, applyInterfaceScale = false) {
                    ShareTurnCard(
                        userPrompt = userPrompt,
                        answerMarkdown = answerMarkdown,
                        brand = brand,
                        onMarkdownReady = { markdownReady.value = true },
                    )
                }
            }
        }
        decorView.addView(
            composeView,
            ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        try {
            composeView.awaitLayout()
            // 等 Markdown 解析完成再出图，避免拍到 loading 阶段的原文兜底；超时则直接出。
            withTimeoutOrNull(MARKDOWN_PARSE_TIMEOUT_MS) {
                snapshotFlow { markdownReady.value }.first { it }
            }
            // 解析完成后的最终排版再等两帧稳定。
            withFrameNanos { }
            withFrameNanos { }

            val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            composeView.measure(widthSpec, heightSpec)
            val measuredHeight = composeView.measuredHeight
            if (measuredHeight <= 0) return
            // 在 view 自身坐标系原点排版：直接 draw(canvas) 时内容落在位图左上角。
            composeView.layout(0, 0, widthPx, measuredHeight)
            val bitmap = Bitmap.createBitmap(
                widthPx,
                measuredHeight.coerceAtMost(MAX_BITMAP_HEIGHT_PX),
                Bitmap.Config.ARGB_8888,
            )
            composeView.draw(Canvas(bitmap))
            withContext(Dispatchers.IO) { shareBitmap(context, bitmap, brand) }
        } finally {
            decorView.removeView(composeView)
        }
    }

    private fun shareBitmap(context: android.content.Context, bitmap: Bitmap, brand: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "eta-share-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, brand))
    }

    private suspend fun View.awaitLayout() {
        if (isLaidOut) return
        suspendCancellableCoroutine { cont ->
            val listener = object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View,
                    left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int,
                ) {
                    v.removeOnLayoutChangeListener(this)
                    cont.resume(Unit)
                }
            }
            addOnLayoutChangeListener(listener)
            cont.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
        }
    }
}

/**
 * 长图卡片：与聊天界面同主题。结构仿 dsh-share——无独立头部，
 * 消息区间距分隔，页脚为分隔线 + 居中品牌字标。
 */
@Composable
private fun ShareTurnCard(
    userPrompt: String?,
    answerMarkdown: String,
    brand: String,
    onMarkdownReady: () -> Unit,
) {
    val markdownState = rememberMarkdownState(content = answerMarkdown, retainState = true)
    val parsedState = markdownState.state.collectAsState().value
    LaunchedEffect(parsedState) {
        if (parsedState is State.Success) onMarkdownReady()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        if (!userPrompt.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = userPrompt,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        StableMarkdown(
            content = answerMarkdown,
            markdownState = markdownState,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.2f)),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = brand,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
