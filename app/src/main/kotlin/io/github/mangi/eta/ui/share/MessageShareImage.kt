package io.github.mangi.eta.ui.share

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.mangi.eta.agent.overlay.toolDisplayName
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.ui.app.AgentAppTheme
import io.github.mangi.eta.ui.components.StatusError
import io.github.mangi.eta.ui.components.iconForTool
import io.github.mangi.eta.ui.markdown.LocalMarkdownStaticExport
import io.github.mangi.eta.ui.markdown.MarkdownContent
import io.github.mangi.eta.ui.markdown.MarkdownDocument
import io.github.mangi.eta.ui.markdown.MarkdownInlineStyle
import io.github.mangi.eta.ui.markdown.MarkdownStyle
import io.github.mangi.eta.ui.markdown.MarkdownTone
import io.github.mangi.eta.ui.markdown.StreamingGfmParserSession
import io.github.mangi.eta.ui.markdown.rememberMarkdownStyle
import io.github.mangi.eta.ui.model.ToolActivityStatusUi
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Eta Mod：分享为长图。
 *
 * 仿照 dsh-share 插件：内容在一棵 1x 离屏视图中完整排版，按 2x → 1x → 0.5x
 * 串行尝试完整 PNG，不做区域切片；所有比例都超预算或渲染失败时回退完整 Markdown。
 *
 * 出图确定性来自「先组装、预解析，后渲染」：全部 Markdown 段在卡片进入组合前用上游自建
 * 解析器（与聊天页同一套）解析成终态文档，渲染时内容一次性完整排版，不存在占位、
 * 异步换树或显现动画——快门按下时树必然是最终状态，同一份内容反复导出结果一致。
 */
internal object MessageShareImage {

    private const val MAX_IMAGE_SIDE = 32_767
    private const val MAX_IMAGE_AREA = 33_554_432L
    private val IMAGE_PIXEL_RATIOS = floatArrayOf(2f, 1f, 0.5f)

    internal data class BitmapGeometry(val width: Int, val height: Int, val scale: Float)

    /**
     * 渲染并弹出系统分享面板。必须在主线程调用。
     *
     * @param anchor 当前界面任意 View（用于取 decorView 与 Activity 上下文）
     * @param appearance 当前外观设置，离屏卡片用同一套主题渲染，保证明暗/配色一致
     * @param turns 待导出的轮次；当前每次一轮，列表形态为多选多轮导出预留
     * @param format 排版宽度预设；高度仍随内容增长
     */
    suspend fun shareTurnAsImage(
        anchor: View,
        appearance: AppearanceSettings,
        turns: List<ShareTurn>,
        brand: String,
        format: ShareImageFormat = ShareImageFormat.Phone,
    ) {
        if (turns.isEmpty()) return
        val context = anchor.context
        val decorView = anchor.rootView as? ViewGroup ?: return
        val renderContext = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                densityDpi = DisplayMetrics.DENSITY_DEFAULT
            },
        )
        val widthPx = format.widthDp.value.toInt().coerceAtLeast(1)
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val shareTimestamp = System.currentTimeMillis()
        var nextFileIndex = 0

        // 与 dsh-share 一致：布局使用 1x 基准，导出时再按像素比放大。
        var contentReady by mutableStateOf(false)
        var renderedTurns by mutableStateOf(listOf(turns.first()))
        val composeView = ComposeView(renderContext).apply {
            visibility = View.INVISIBLE
            setContent {
                AgentAppTheme(appearance = appearance, applyInterfaceScale = false) {
                    val style = rememberMarkdownStyle(MarkdownTone.Answer)
                    val documents = produceState<Map<String, MarkdownDocument>?>(
                        initialValue = null,
                        turns,
                        style.inline,
                    ) {
                        value = withContext(Dispatchers.Default) { parseAllSegments(turns, style.inline) }
                    }.value
                    if (documents != null) {
                        // 软件画布不支持 RuntimeShader，静态导出统一回退普通圆角。
                        CompositionLocalProvider(
                            LocalMarkdownStaticExport provides true,
                            LocalSquircleEnabled provides false,
                        ) {
                            ShareTurnsCard(
                                turns = renderedTurns,
                                documents = documents,
                                style = style,
                                brand = brand,
                            )
                        }
                        SideEffect { contentReady = true }
                    }
                }
            }
        }
        decorView.addView(
            composeView,
            ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        try {
            composeView.awaitLayout()
            snapshotFlow { contentReady }.first { it }

            suspend fun measurePage(page: List<ShareTurn>): Int {
                if (renderedTurns != page) {
                    contentReady = false
                    renderedTurns = page
                    snapshotFlow { contentReady }.first { it }
                }
                withFrameNanos { }
                withFrameNanos { }
                val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                composeView.measure(widthSpec, heightSpec)
                val measuredHeight = composeView.measuredHeight
                if (measuredHeight > 0) composeView.layout(0, 0, widthPx, measuredHeight)
                return measuredHeight
            }

            suspend fun renderTurn(turn: ShareTurn): File? {
                val measuredHeight = try {
                    measurePage(listOf(turn))
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    return null
                }
                if (measuredHeight <= 0) return null
                for (pixelRatio in IMAGE_PIXEL_RATIOS) {
                    val geometry = bitmapGeometry(widthPx, measuredHeight, pixelRatio) ?: continue
                    val bitmap = try {
                        Bitmap.createBitmap(geometry.width, geometry.height, Bitmap.Config.ARGB_8888)
                    } catch (_: Throwable) {
                        null
                    } ?: continue
                    try {
                        val canvas = Canvas(bitmap)
                        canvas.scale(geometry.scale, geometry.scale)
                        composeView.draw(canvas)
                        val file = withContext(Dispatchers.IO) {
                            File(shareDir, "eta-share-$shareTimestamp-${nextFileIndex + 1}.png").also { output ->
                                FileOutputStream(output).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                            }
                        }
                        if (file.length() > 0) {
                            nextFileIndex++
                            return file
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Throwable) {
                    } finally {
                        bitmap.recycle()
                    }
                }
                return null
            }

            val files = mutableListOf<File>()
            var fallbackToMarkdown = false
            for (turn in turns) {
                val file = renderTurn(turn)
                if (file == null) {
                    fallbackToMarkdown = true
                    break
                }
                files += file
            }
            withContext(Dispatchers.IO) {
                if (fallbackToMarkdown) {
                    val file = File(shareDir, "eta-share-$shareTimestamp.md")
                    file.writeText(markdownFor(turns, brand))
                    shareMarkdown(context, file, brand)
                } else {
                    shareImages(context, files, brand)
                }
            }
        } finally {
            decorView.removeView(composeView)
        }
    }

    internal fun bitmapGeometry(widthPx: Int, heightPx: Int, pixelRatio: Float): BitmapGeometry? {
        require(widthPx > 0)
        require(heightPx > 0)
        require(pixelRatio > 0)
        val width = (widthPx * pixelRatio).toInt()
        val height = (heightPx * pixelRatio).toInt()
        if (width < 1 || height < 1 || width > MAX_IMAGE_SIDE || height > MAX_IMAGE_SIDE) return null
        if (width.toLong() * height.toLong() > MAX_IMAGE_AREA) return null
        return BitmapGeometry(width, height, pixelRatio)
    }

    internal fun markdownFor(turns: List<ShareTurn>, brand: String): String = buildString {
        turns.forEachIndexed { turnIndex, turn ->
            if (turnIndex > 0) append("\n\n---\n\n")
            if (!turn.userPrompt.isNullOrBlank()) {
                append("## 用户\n\n").append(turn.userPrompt).append("\n\n")
            }
            turn.segments.forEach { segment ->
                when (segment) {
                    is ShareTurnSegment.Markdown -> append(segment.text).append("\n\n")
                    is ShareTurnSegment.PlainText -> append(segment.text).append("\n\n")
                    is ShareTurnSegment.ToolCall -> {
                        append("- **").append(segment.toolName).append("**")
                        if (segment.summary.isNotBlank()) append("：").append(segment.summary)
                        append('\n')
                        segment.failureLine?.let { append("  - 失败：").append(it).append('\n') }
                    }
                }
            }
            append("\n---\n\n").append(shareFooterBrand(listOf(turn), brand)).append('\n')
        }
    }

    /** 与聊天页同一套解析器，终态整体解析，在调用线程同步完成；失败的段退化为纯文本渲染。 */
    internal fun parseAllSegments(
        turns: List<ShareTurn>,
        inlineStyle: MarkdownInlineStyle,
    ): Map<String, MarkdownDocument> {
        val result = LinkedHashMap<String, MarkdownDocument>()
        for (turn in turns) {
            for (segment in turn.segments) {
                if (segment !is ShareTurnSegment.Markdown || result.containsKey(segment.text)) continue
                val document = runCatching {
                    StreamingGfmParserSession()
                        .parse(segment.text, isComplete = true, style = inlineStyle)
                        .document
                }.getOrNull()
                if (document != null) result[segment.text] = document
            }
        }
        return result
    }

    private fun shareImages(context: android.content.Context, files: List<File>, brand: String) {
        val uris = files.mapTo(ArrayList()) { file ->
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.single())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, brand))
    }

    private fun shareMarkdown(context: android.content.Context, file: File, brand: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
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
 * 轮与轮之间以间距分隔，页脚为分隔线 + 居中模型 ID（历史消息缺失时回退品牌）。
 * internal 供 Robolectric 渲染测试直接组合。
 */
@Composable
internal fun ShareTurnsCard(
    turns: List<ShareTurn>,
    documents: Map<String, MarkdownDocument>,
    style: MarkdownStyle,
    brand: String,
) {
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
                // 与聊天页一致：用户气泡右对齐、宽度包内容（封顶约 3/4 卡宽）。
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
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
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            // 连续工具调用合并成一条时间线渲染，其余段各自成块；块间统一 10dp 呼吸间距。
            var index = 0
            var firstUnit = true
            while (index < turn.segments.size) {
                if (!firstUnit) Spacer(modifier = Modifier.height(10.dp))
                firstUnit = false
                val segment = turn.segments[index]
                if (segment is ShareTurnSegment.ToolCall) {
                    val run = mutableListOf<ShareTurnSegment.ToolCall>()
                    while (index < turn.segments.size &&
                        turn.segments[index] is ShareTurnSegment.ToolCall
                    ) {
                        run.add(turn.segments[index] as ShareTurnSegment.ToolCall)
                        index++
                    }
                    ShareToolTimeline(calls = run)
                    continue
                }
                index++
                when (segment) {
                    is ShareTurnSegment.Markdown -> {
                        val document = documents[segment.text]
                        if (document != null) {
                            MarkdownContent(
                                document = document,
                                style = style,
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
                    is ShareTurnSegment.ToolCall -> Unit // 已在上方按时间线处理
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
                text = shareFooterBrand(turns, brand),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun shareFooterBrand(turns: List<ShareTurn>, fallback: String): String {
    val modelApiId = turns.lastOrNull()?.modelApiId?.trim()
    return modelApiId
        ?.takeIf { it.isNotEmpty() && it.none(Char::isISOControl) }
        ?: fallback
}

/** 时间线中该节点的位置：决定竖线从哪画到哪，保证跨行无缝衔接。 */
private enum class TimelinePosition { Single, First, Middle, Last }

/**
 * 工具调用时间线：仿聊天页工作过程区的样式——
 * 左侧竖线串联圆形节点图标，行间内边距代替间隔，竖线因此连续不断。
 */
@Composable
private fun ShareToolTimeline(calls: List<ShareTurnSegment.ToolCall>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        calls.forEachIndexed { i, call ->
            val position = when {
                calls.size == 1 -> TimelinePosition.Single
                i == 0 -> TimelinePosition.First
                i == calls.lastIndex -> TimelinePosition.Last
                else -> TimelinePosition.Middle
            }
            ShareToolTimelineRow(segment = call, position = position)
        }
    }
}

/**
 * 时间线中的一行：节点圆 + 图标在左（竖线从行中心穿过并与相邻行相接），
 * 参数摘要居中，右侧为状态标记与 chevron（样式对齐聊天页，静态图中仅作装饰）。
 */
@Composable
private fun ShareToolTimelineRow(segment: ShareTurnSegment.ToolCall, position: TimelinePosition) {
    val failed = segment.status == ToolActivityStatusUi.Failed
    val lineColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight()
                .drawBehind {
                    if (position == TimelinePosition.Single) return@drawBehind
                    val lineWidth = 1.5.dp.toPx()
                    val x = size.width / 2f
                    val centerY = size.height / 2f
                    val startY = if (position == TimelinePosition.First) centerY else 0f
                    val endY = if (position == TimelinePosition.Last) centerY else size.height
                    drawRect(
                        color = lineColor,
                        topLeft = Offset(x - lineWidth / 2f, startY),
                        size = Size(lineWidth, endY - startY),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForTool(segment.toolName),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (failed) {
                        StatusError
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.8f)
                    },
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
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
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f),
        )
    }
}
