package io.github.mangi.eta.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.agent.model.AgentFileReferencePolicy
import io.github.mangi.eta.agent.model.AgentFileReferencePromptCodec
import io.github.mangi.eta.ui.model.AgentMessageUi
import io.github.mangi.eta.ui.model.UserMessageUi
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Eta Mod：会话页横向刻度导航（Turn Tick Rail）的一轮刻度。
 *
 * 轮边界与 [io.github.mangi.eta.ui.share.buildShareTurns] 同一语义：
 * 遇到下一条用户消息即封上一轮。[timelineIndex] 是该轮首条用户消息在时间线条目
 * （工作过程分组折叠后）里的下标，跳转时直接滚动到它。
 */
internal data class TurnTick(
    val timelineIndex: Int,
    val inputSummary: String,
    val outputSummary: String,
)

/** 从时间线条目派生刻度数据；调用方以 remember(timelineEntries) 持有结果。 */
internal fun buildTurnTicks(entries: List<AgentTimelineEntry>): List<TurnTick> {
    val ticks = ArrayList<TurnTick>()
    var pendingIndex = -1
    var pendingInput = ""
    var lastOutput = ""

    fun closeTurn() {
        if (pendingIndex >= 0) {
            ticks += TurnTick(
                timelineIndex = pendingIndex,
                inputSummary = pendingInput,
                outputSummary = lastOutput,
            )
        }
    }

    entries.forEachIndexed { index, entry ->
        val message = (entry as? AgentTimelineEntry.Message)?.message ?: return@forEachIndexed
        when (message) {
            is UserMessageUi -> {
                closeTurn()
                pendingIndex = index
                val parsed = AgentFileReferencePromptCodec.parse(message.content)
                pendingInput = AgentFileReferencePolicy.titleSource(parsed.request, parsed.references)
                    .ifBlank { message.content.trim() }
                    .collapseSummaryWhitespace(MAX_SUMMARY_CHARS)
                lastOutput = ""
            }
            is AgentMessageUi -> {
                if (message.content.isNotBlank()) {
                    lastOutput = message.content.collapseSummaryWhitespace(MAX_SUMMARY_CHARS)
                }
            }
            else -> Unit
        }
    }
    closeTurn()
    return ticks
}

private fun String.collapseSummaryWhitespace(maxChars: Int): String =
    lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .let { if (it.length > maxChars) it.take(maxChars) + "…" else it }

private const val MAX_SUMMARY_CHARS = 160

/** 刻度条总高度（含触控余量）。 */
internal val TurnTickRailHeight = 28.dp

private val TickSlot = 8.dp
private val EdgeFadeWidth = 28.dp

/** 分档（纯数量判断）：当前轮 / 最新第 2~11 轮 / 更早。 */
private val Tier1Width = 3.dp
private val Tier1Height = 16.dp
private val Tier2Width = 2.5.dp
private val Tier2Height = 11.dp
private val Tier3Width = 2.dp
private val Tier3Height = 7.dp
private val TickBottomPadding = 4.dp

/** 手指按下时全体刻度等比放大倍数（避免被手指挡住）。 */
private const val TOUCH_SCALE = 1.65f

/** 虚刻度（延伸占位）：真实刻度带两端之外的延伸刻度，仅按住期间淡入，不可选中、纯视觉。 */
private val GhostTickWidth = 1.5.dp
private val GhostTickHeight = 5.dp
private const val GHOST_TICK_ALPHA = 0.25f

/**
 * 把列表视口位置（首可见条目的浮点下标）换算成浮点轮次，用于刻度带跟随屏幕内容整体滑动。
 * 轮边界是该轮首条用户消息在时间线条目中的下标；条目位置在相邻边界间线性插值。
 */
internal fun resolveVisibleTurn(
    itemPosition: Float,
    turnBoundaries: List<Int>,
    totalItems: Int,
): Float {
    if (turnBoundaries.isEmpty()) return -1f
    val lastTurn = (turnBoundaries.size - 1).toFloat()
    if (itemPosition <= turnBoundaries.first()) return 0f
    for (i in 0 until turnBoundaries.size - 1) {
        val start = turnBoundaries[i]
        val end = turnBoundaries[i + 1]
        if (itemPosition < end) {
            return i + (itemPosition - start) / (end - start)
        }
    }
    val lastSpan = (totalItems - turnBoundaries.last()).coerceAtLeast(1)
    return (lastTurn + (itemPosition - turnBoundaries.last()) / lastSpan).coerceAtMost(lastTurn)
}

/**
 * 横向刻度条：并入输入框浮层 dock、位于输入框正上方，左右边界与输入框一致。
 *
 * 标尺式中心锁定模型：
 * - 选中轮（蓝条）永远居中，刻度带作为整体在下方滑动
 * - 列表滚动时 [currentTurn] 跟随视口位置（浮点），刻度带随之平滑滑动
 * - 轻点刻度 → [onTap] 跳到该轮（末轮即"回到底部"，由调用方判定）
 * - 按住横向拖动 → 刻度带随手整体移动，中心蓝条为目标轮，列表实时刮擦跟随，
 *   回调 [onScrubStart] / [onScrubTick] / [onScrubEnd]
 * - 手指按下期间全体刻度等比放大（×[TOUCH_SCALE]），刻度带两端之外淡入虚刻度
 *   （延伸占位，不可选中、纯视觉，松手淡出）
 * - 刮擦期间经 [onBubbleChanged] 上报目标轮（气泡由调用方渲染在模糊层之外，
 *   磨砂带 Box 会裁剪子内容溢出，气泡不能放在刻度条内部）
 */
@Composable
internal fun TurnTickRail(
    ticks: List<TurnTick>,
    currentTurn: State<Float>,
    onTap: (TurnTick) -> Unit,
    onScrubStart: () -> Unit,
    onScrubTick: (TurnTick) -> Unit,
    onScrubEnd: (atLatest: Boolean) -> Unit,
    onBubbleChanged: (TurnTick?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ticks.size < MIN_TICKS) return

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val slotPx = with(density) { TickSlot.toPx() }
    val edgeFadePx = with(density) { EdgeFadeWidth.toPx() }
    val tickBottomPadPx = with(density) { TickBottomPadding.toPx() }
    val tier1WidthPx = with(density) { Tier1Width.toPx() }
    val tier1HeightPx = with(density) { Tier1Height.toPx() }
    val tier2WidthPx = with(density) { Tier2Width.toPx() }
    val tier2HeightPx = with(density) { Tier2Height.toPx() }
    val tier3WidthPx = with(density) { Tier3Width.toPx() }
    val tier3HeightPx = with(density) { Tier3Height.toPx() }
    val ghostTickWidthPx = with(density) { GhostTickWidth.toPx() }
    val ghostTickHeightPx = with(density) { GhostTickHeight.toPx() }

    var railWidthPx by remember { mutableFloatStateOf(0f) }
    // >=0 表示正在刮擦（手指驱动的浮点轮次）；<0 表示跟随列表视口。
    var scrubTurn by remember { mutableFloatStateOf(-1f) }
    var scrubTarget by remember { mutableIntStateOf(-1) }
    var touching by remember { mutableStateOf(false) }
    val touchAnim by animateFloatAsState(
        targetValue = if (touching) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "turnTickTouch",
    )

    val currentTicks by rememberUpdatedState(ticks)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnScrubStart by rememberUpdatedState(onScrubStart)
    val currentOnScrubTick by rememberUpdatedState(onScrubTick)
    val currentOnScrubEnd by rememberUpdatedState(onScrubEnd)
    val currentOnBubbleChanged by rememberUpdatedState(onBubbleChanged)

    fun displayTurn(): Float = when {
        scrubTurn >= 0f -> scrubTurn
        currentTurn.value >= 0f -> currentTurn.value.coerceIn(0f, currentTicks.lastIndex.toFloat())
        else -> currentTicks.lastIndex.toFloat()
    }

    fun fireScrubTarget() {
        val index = scrubTurn.roundToInt().coerceIn(0, currentTicks.lastIndex)
        if (index == scrubTarget) return
        scrubTarget = index
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        currentOnScrubTick(currentTicks[index])
        currentOnBubbleChanged(currentTicks[index])
    }

    val primaryColor = MiuixTheme.colorScheme.primary
    val tickColor = MiuixTheme.colorScheme.onSurfaceVariantSummary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(TurnTickRailHeight)
            .onSizeChanged { railWidthPx = it.width.toFloat() }
            .pointerInput(slotPx) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    touching = true
                    val startTurn = displayTurn()
                    var dragDistance = 0f
                    var scrubbing = false
                    val dragChange = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        if (!scrubbing) {
                            scrubbing = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnScrubStart()
                        }
                        dragDistance += overSlop
                        change.consume()
                    }
                    if (dragChange == null) {
                        // 未过滑动阈值即松手：轻点跳轮。
                        touching = false
                        val center = railWidthPx / 2f
                        val index = (displayTurn() + (down.position.x - center) / slotPx)
                            .roundToInt()
                            .coerceIn(0, currentTicks.lastIndex)
                        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        currentOnTap(currentTicks[index])
                        return@awaitEachGesture
                    }
                    scrubTurn = (startTurn - dragDistance / slotPx)
                        .coerceIn(0f, currentTicks.lastIndex.toFloat())
                    fireScrubTarget()
                    while (true) {
                        val continued = drag(dragChange.id) { dragEvent ->
                            dragDistance += dragEvent.position.x - dragEvent.previousPosition.x
                            scrubTurn = (startTurn - dragDistance / slotPx)
                                .coerceIn(0f, currentTicks.lastIndex.toFloat())
                            fireScrubTarget()
                            dragEvent.consume()
                        }
                        if (!continued) break
                    }
                    val atLatest = scrubTarget == currentTicks.lastIndex
                    touching = false
                    scrubTarget = -1
                    scrubTurn = -1f
                    currentOnBubbleChanged(null)
                    currentOnScrubEnd(atLatest)
                }
            },
    ) {
        val count = currentTicks.size
        if (count == 0 || size.width <= 0f) return@Canvas
        val windowed = count * slotPx > size.width
        val center = size.width / 2f
        val turn = when {
            scrubTurn >= 0f -> scrubTurn
            currentTurn.value >= 0f -> currentTurn.value.coerceIn(0f, (count - 1).toFloat())
            else -> (count - 1).toFloat()
        }
        val highlight = turn.roundToInt().coerceIn(0, count - 1)
        val scale = 1f + (TOUCH_SCALE - 1f) * touchAnim
        val baseline = size.height - tickBottomPadPx

        // 虚刻度：真实刻度带两端的延伸占位（更低一级，不可选中），按住淡入、松手淡出。
        if (touchAnim > 0.01f && count > 0) {
            val ghostWidth = ghostTickWidthPx * scale
            val ghostHeight = ghostTickHeightPx * scale
            val bandStart = center + (0f - turn) * slotPx
            val leftSlots = ((bandStart - slotPx) / slotPx).toInt().coerceIn(0, 12)
            fun drawGhost(x: Float) {
                var alpha = GHOST_TICK_ALPHA * touchAnim
                if (windowed) {
                    alpha *= (x / edgeFadePx).coerceIn(0f, 1f)
                    alpha *= ((size.width - x) / edgeFadePx).coerceIn(0f, 1f)
                }
                if (alpha <= 0.004f) return
                drawRoundRect(
                    color = tickColor,
                    topLeft = Offset(x - ghostWidth / 2f, baseline - ghostHeight),
                    size = Size(ghostWidth, ghostHeight),
                    cornerRadius = CornerRadius(ghostWidth / 2f),
                    alpha = alpha,
                )
            }
            for (k in 1..leftSlots) drawGhost(bandStart - k * slotPx)
        }

        for (i in 0 until count) {
            val x = center + (i - turn) * slotPx
            if (x < -slotPx || x > size.width + slotPx) continue
            val tier = when {
                i == highlight -> 1
                i >= count - 11 -> 2
                else -> 3
            }
            var alpha = when (tier) {
                1 -> 1f
                2 -> 0.75f
                else -> 0.5f
            }
            // 两端 28dp alpha 渐隐（窗口化时）。
            if (windowed) {
                alpha *= (x / edgeFadePx).coerceIn(0f, 1f)
                alpha *= ((size.width - x) / edgeFadePx).coerceIn(0f, 1f)
            }
            if (alpha <= 0.004f) continue
            val (baseWidth, baseHeight) = when (tier) {
                1 -> tier1WidthPx to tier1HeightPx
                2 -> tier2WidthPx to tier2HeightPx
                else -> tier3WidthPx to tier3HeightPx
            }
            val tickWidth = baseWidth * scale
            val tickHeight = baseHeight * scale
            drawRoundRect(
                color = if (tier == 1) primaryColor else tickColor,
                topLeft = Offset(x - tickWidth / 2f, baseline - tickHeight),
                size = Size(tickWidth, tickHeight),
                cornerRadius = CornerRadius(tickWidth / 2f),
                alpha = alpha,
            )
        }
    }
}

/**
 * 拖动专属气泡卡片（纯内容）：加粗输入摘要（≤2 行）+ 常规输出摘要（≤3 行）。
 * 定位由调用方负责——必须渲染在模糊层 Box 之外（磨砂带 textureBlur 层会裁剪子内容溢出）。
 */
@Composable
internal fun TurnTickBubble(
    tick: TurnTick,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (tick.inputSummary.isNotBlank()) {
            Text(
                text = tick.inputSummary,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (tick.outputSummary.isNotBlank()) {
            if (tick.inputSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = tick.outputSummary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val MIN_TICKS = 3
