package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import io.github.mangi.eta.ui.app.LocalLiquidGlassEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Eta Mod：液态玻璃材质 Token 层（UI 焕新架构改动一）。
 *
 * 所有玻璃观感参数集中在此，控件/弹层只选预设不手调参数，换材质=改这里一处。
 * 决策矩阵（Four 2026-10-16 定调，详见 docs/UI-GLASS-ARCHITECTURE.md）：
 * - overlay 系弹层、主页固定控件 → 真玻璃（[etaGlass]，需同 surface 的 backdrop）；
 * - Window 系弹层（独立 Window 采不到样）→ 假玻璃（[etaPseudoGlass]，半透明+高光描边）；
 * - 长列表项 → [EtaGlassStyles.ListRow] 弱化材质防滚动掉帧。
 *
 * 硬约束：Kyant lens 效果只支持 CornerBasedShape/RoundedRectangularShape，
 * 通用 Shape（如 squircle 路径）会抛 UnsupportedOperationException（v3.3.0-010 弹层闪退事故）。
 */
internal data class EtaGlassStyle(
    val shape: Shape,
    val blurRadius: Dp,
    val lensHeight: Dp,
    val lensAmount: Dp,
    /** 真玻璃表面着色透明度（叠在模糊层上的 surface tint）。 */
    val tintAlpha: Float = 0.07f,
    /** 假玻璃底色透明度（无采样源时的半透明底）。 */
    val pseudoBackgroundAlpha: Float = 0.72f,
    /** 假玻璃高光描边透明度。 */
    val pseudoBorderAlpha: Float = 0.28f,
)

internal object EtaGlassStyles {
    /** 圆形浮动按钮（主页顶栏双按钮、输入栏图标按钮）。参数即现网值，勿随意改。 */
    val FloatingButton = EtaGlassStyle(
        shape = CircleShape,
        blurRadius = 10.dp,
        lensHeight = 6.dp,
        lensAmount = 10.dp,
    )

    /** 圆角条形容器（底部输入栏）。shape 由调用方按圆角 copy。 */
    val InputBar = EtaGlassStyle(
        shape = RoundedCornerShape(24.dp),
        blurRadius = 8.dp,
        lensHeight = 8.dp,
        lensAmount = 12.dp,
    )

    /** 弹层容器（EtaListPopup 等 overlay 弹层；对齐 miuix ListPopupContent 的 16dp 圆角）。 */
    val Popup = EtaGlassStyle(
        shape = RoundedCornerShape(16.dp),
        blurRadius = 12.dp,
        lensHeight = 8.dp,
        lensAmount = 12.dp,
    )

    /** 弹窗卡片（确认/输入弹窗）。 */
    val DialogCard = EtaGlassStyle(
        shape = RoundedCornerShape(32.dp),
        blurRadius = 16.dp,
        lensHeight = 10.dp,
        lensAmount = 14.dp,
    )

    /** 长列表项弱化材质：轻模糊弱折射，避免整列 drawBackdrop 滚动掉帧。 */
    val ListRow = EtaGlassStyle(
        shape = RoundedCornerShape(12.dp),
        blurRadius = 4.dp,
        lensHeight = 4.dp,
        lensAmount = 6.dp,
    )
}

/**
 * 真玻璃：backdrop 非 null（液态玻璃档且设备支持）时走 Kyant drawBackdrop；
 * 否则 no-op，界面回退到现有外观。
 */
internal fun Modifier.etaGlass(
    style: EtaGlassStyle,
    backdrop: LayerBackdrop?,
    tint: Color = Color.Unspecified,
): Modifier = if (backdrop == null) {
    this
} else {
    drawBackdrop(
        backdrop = backdrop,
        shape = { style.shape },
        effects = {
            vibrancy()
            blur(style.blurRadius.toPx())
            lens(style.lensHeight.toPx(), style.lensAmount.toPx())
        },
        highlight = { Highlight.Default },
        shadow = null,
        onDrawSurface = {
            if (tint.isSpecified) drawRect(tint)
        },
    )
}

/**
 * 假玻璃：纯 Compose 半透明底 + 顶部亮边缘渐变描边，不依赖任何 backdrop，
 * 独立 Window（WindowDialog/WindowListPopup）内可用。仅液态玻璃档启用，其余档 no-op。
 */
internal fun Modifier.etaPseudoGlass(style: EtaGlassStyle): Modifier = composed {
    if (!LocalLiquidGlassEnabled.current) return@composed this
    val dark = isSystemInDarkTheme()
    val backgroundColor = MiuixTheme.colorScheme.surface.copy(alpha = style.pseudoBackgroundAlpha)
    val highlightColor = Color.White.copy(
        alpha = if (dark) style.pseudoBorderAlpha else style.pseudoBorderAlpha * 2f,
    )
    val borderBrush = Brush.verticalGradient(
        colors = listOf(highlightColor, highlightColor.copy(alpha = 0f)),
    )
    this
        .background(backgroundColor, style.shape)
        .border(0.5.dp, borderBrush, style.shape)
}

/**
 * 只加假玻璃高光描边：组件底色经自身参数（如 RichTooltip 的 containerColor）改成半透明时，
 * 用它补顶部亮边缘渐变，与 [etaPseudoGlass] 观感一致。仅液态玻璃档启用。
 */
internal fun Modifier.etaPseudoGlassBorder(style: EtaGlassStyle): Modifier = composed {
    if (!LocalLiquidGlassEnabled.current) return@composed this
    val dark = isSystemInDarkTheme()
    val highlightColor = Color.White.copy(
        alpha = if (dark) style.pseudoBorderAlpha else style.pseudoBorderAlpha * 2f,
    )
    val borderBrush = Brush.verticalGradient(
        colors = listOf(highlightColor, highlightColor.copy(alpha = 0f)),
    )
    this.border(0.5.dp, borderBrush, style.shape)
}
