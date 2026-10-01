package io.github.mangi.eta.ui.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.parseMarkdown
import io.github.mangi.eta.agent.overlay.toolDisplayName
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.ui.app.AgentAppTheme
import io.github.mangi.eta.ui.components.StableMarkdown
import io.github.mangi.eta.ui.components.StatusError
import io.github.mangi.eta.ui.components.iconForTool
import io.github.mangi.eta.ui.model.ToolActivityStatusUi
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Eta Mod：分享为长图。
 *
 * 仿照 dsh-share 插件的思路：不是截图拼接，而是把消息内容在一棵离屏视图中重新渲染成卡片
 * （用户提问块 + 按真实时序排列的正文/工具段 + 品牌页脚），再整体绘制成一张 PNG 分享出去。
 *
 * 出图确定性来自「先组装、预解析，后渲染」：全部 Markdown 段在建视图之前解析成终态
 * （优先复用聊天页已完成的解析），渲染时内容一次性完整排版，不存在 Loading 兜底、
 * 异步换树或尺寸动画——快门按下时树必然是最终状态，同一份内容反复导出结果一致。
 */
internal object MessageShareImage {

    /** 超长内容兜底：位图高度上限（约 8 屏），超出部分裁断。 */
    private const val MAX_BITMAP_HEIGHT_PX = 12_000

    /**
     * 渲染并弹出系统分享面板。必须在主线程调用。
     *
     * @param anchor 当前界面任意 View（用于取 decorView 与 Activity 上下文）
     * @param appearance 当前外观设置，离屏卡片用同一套主题渲染，保证明暗/配色一致
     * @param turns 待导出的轮次；当前每次一轮，列表形态为多选多轮导出预留
     * @param finalParsedState 被点消息（本轮最后一段 Markdown）在聊天页已完成的解析，可为空
     */
    suspend fun shareTurnAsImage(
        anchor: View,
        appearance: AppearanceSettings,
        turns: List<ShareTurn>,
        finalParsedState: State.Success?,
        brand: String,
    ) {
        if (turns.isEmpty()) return
        val context = anchor.context
        val decorView = anchor.rootView as? ViewGroup ?: return
        val widthPx = decorView.width.takeIf { it > 0 }
            ?: context.resources.displayMetrics.widthPixels
        if (widthPx <= 0) return

        // 预解析：全部 Markdown 段在后台线程解析成终态。解析失败的段退化为纯文本渲染。
        val parsedStates = withContext(Dispatchers.Default) { parseAllSegments(turns, finalParsedState) }

        val composeView = ComposeView(context).apply {
            visibility = View.INVISIBLE
            setContent {
                AgentAppTheme(appearance = appearance, applyInterfaceScale = false) {
                    ShareTurnsCard(
                        turns = turns,
                        parsedStates = parsedStates,
                        brand = brand,
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
            // 内容在首次排版前即完整，两帧只用于让组合/排版彻底落地。
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

    /** 与聊天页同一套 GFM 解析（parser/flavour 相同），在调用线程同步完成。 */
    private fun parseAllSegments(
        turns: List<ShareTurn>,
        finalParsedState: State.Success?,
    ): Map<String, State.Success> {
        val flavour = GFMFlavourDescriptor()
        val parser = MarkdownParser(flavour)
        val result = LinkedHashMap<String, State.Success>()
        for (turn in turns) {
            for (segment in turn.segments) {
                if (segment !is ShareTurnSegment.Markdown || result.containsKey(segment.text)) continue
                val parsed = finalParsedState?.takeIf { it.content == segment.text }
                    ?: runCatching {
                        when (val state = parseMarkdown(
                            content = segment.text,
                            lookupLinks = true,
                            flavour = flavour,
                            parser = parser,
                        )) {
                            is State.Success -> state
                            is State.Error -> throw state.result
                            is State.Loading -> error("Synchronous Markdown parsing returned Loading")
                        }
                    }.getOrNull()
                if (parsed != null) result[segment.text] = parsed
            }
        }
        return result
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
 * 轮与轮之间以间距分隔，页脚为分隔线 + 居中品牌字标。
 */
@Composable
private fun ShareTurnsCard(
    turns: List<ShareTurn>,
    parsedStates: Map<String, State.Success>,
    brand: String,
) {
    // 与流式渲染一致的恒等动画：内容尺寸即最终尺寸，杜绝 animateContentSize 的中间态。
    val noTextSizeAnimation = markdownAnimations(animateTextSize = { this })
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        turns.forEachIndexed { turnIndex, turn ->
            if (turnIndex > 0) {
                Spacer(modifier = Modifier.height(28.dp))
            }
            if (!turn.userPrompt.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = turn.userPrompt,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            turn.segments.forEachIndexed { index, segment ->
                if (index > 0) {
                    val previous = turn.segments[index - 1]
                    // 连续工具行收紧为一张清单，其余段之间保持正文呼吸感。
                    val gap = if (previous is ShareTurnSegment.ToolCall &&
                        segment is ShareTurnSegment.ToolCall
                    ) {
                        2.dp
                    } else {
                        10.dp
                    }
                    Spacer(modifier = Modifier.height(gap))
                }
                when (segment) {
                    is ShareTurnSegment.Markdown -> {
                        val parsed = parsedStates[segment.text]
                        if (parsed != null) {
                            StableMarkdown(
                                content = segment.text,
                                parsedState = parsed,
                                animations = noTextSizeAnimation,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            // 预解析失败的兜底：与聊天页 error 分支一致，退化为原文纯文本。
                            Text(
                                text = segment.text,
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    is ShareTurnSegment.PlainText -> Text(
                        text = segment.text,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    is ShareTurnSegment.ToolCall -> ShareToolCallRow(segment = segment)
                }
            }
        }
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

/**
 * 工具调用段：聊天页 ToolActivityInline 的静态简化版——
 * 图标 + 参数摘要（缺省回退工具名）+ 状态标记，失败时附首行原因。
 * 不做展开、脉冲动画、命令与结果详情。
 */
@Composable
private fun ShareToolCallRow(segment: ShareTurnSegment.ToolCall) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 5.dp),
    ) {
        Icon(
            imageVector = iconForTool(segment.toolName),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = if (segment.status == ToolActivityStatusUi.Failed) {
                StatusError
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.8f)
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = segment.summary.ifBlank { toolDisplayName(segment.toolName) },
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (segment.failureLine != null) {
                Text(
                    text = segment.failureLine,
                    style = MiuixTheme.textStyles.footnote2,
                    color = StatusError,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when (segment.status) {
            ToolActivityStatusUi.Success -> Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
            )
            ToolActivityStatusUi.Failed -> Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = StatusError,
            )
            else -> Unit
        }
    }
}
