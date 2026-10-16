package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import io.github.mangi.eta.ui.app.LocalLiquidGlassEnabled
import top.yukonga.miuix.kmp.squircle.addSquircleRect
import top.yukonga.miuix.kmp.squircle.isSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Eta Mod：液态玻璃材质 Token 层（UI 焕新架构改动一）。
 *
 * 所有玻璃观感参数集中在此，控件/弹层只选预设不手调参数，换材质=改这里一处。
 * 决策矩阵（Four 2026-10-16 定调，详见 docs/UI-GLASS-ARCHITECTURE.md）：
 * - overlay 系弹层、主页固定控件 → 真玻璃（[etaGlass]，需同 surface 的 backdrop）；
 * - Window 系弹层（独立 Window 采不到样）→ 不透明描边玻璃（原生不透明底 + [etaPseudoGlassBorder]；
 *   半透明假玻璃实测观感差，Four 2026-10-17 修订）；
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

/** 浅色液态玻璃档的页面底色（Four 2026-10-17）：约 10% 灰白（6% 实测不可见）。纯白底下假玻璃的白色高光不可见。 */
internal val EtaGlassLightPageBase = Color(0xFFE6E6EA)

/**
 * 页面/面板底色入口：仅浅色 + 液态玻璃档返回灰白 [EtaGlassLightPageBase]，
 * 其余主题档与深色原样返回 [fallback]，不改变现有观感。
 */
@Composable
internal fun etaGlassPageBaseColor(fallback: Color): Color =
    if (LocalLiquidGlassEnabled.current && MiuixTheme.colorScheme.background.luminance() > 0.5f) {
        EtaGlassLightPageBase
    } else {
        fallback
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
 *
 * [pseudoBackground] 非 null 时直接作为底色（如反色玻璃：浅色系深玻璃、深色系浅玻璃），
 * 默认 surface × [EtaGlassStyle.pseudoBackgroundAlpha]。
 */
internal fun Modifier.etaPseudoGlass(
    style: EtaGlassStyle,
    pseudoBackground: Color? = null,
): Modifier = composed {
    if (!LocalLiquidGlassEnabled.current) return@composed this
    val dark = isSystemInDarkTheme()
    val backgroundColor = pseudoBackground
        ?: MiuixTheme.colorScheme.surface.copy(alpha = style.pseudoBackgroundAlpha)
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

/**
 * squircle 版假玻璃高光描边：miuix squircleSurface 容器（EtaCard/弹窗）专用，
 * 轮廓与容器填充完全贴合（miuix 自带 squircleBorder 只支持纯色，这里支持渐变 Brush）。
 * 绘在内容之上（onDrawWithContent）——miuix 的 Card/Dialog 都把 user modifier 排在
 * 内部 squircleSurface 之前，本修饰符放在调用方 modifier 上即可压在填充上。
 * 仅液态玻璃档启用；squircle 被关闭时经 addSquircleRect 自动回退圆角矩形。
 */
internal fun Modifier.etaSquircleGlassBorder(
    cornerRadius: Dp,
    style: EtaGlassStyle = EtaGlassStyles.Popup,
): Modifier = composed {
    if (!LocalLiquidGlassEnabled.current) return@composed this
    val dark = isSystemInDarkTheme()
    val squircleEnabled = isSquircleEnabled()
    val highlightColor = Color.White.copy(
        alpha = if (dark) style.pseudoBorderAlpha else style.pseudoBorderAlpha * 2f,
    )
    val borderBrush = Brush.verticalGradient(
        colors = listOf(highlightColor, highlightColor.copy(alpha = 0f)),
    )
    this.drawWithCache {
        val widthPx = 0.5.dp.toPx()
        val cornerRadiusPx = cornerRadius.toPx()
        val halfStroke = widthPx / 2f
        val innerW = size.width - widthPx
        val innerH = size.height - widthPx
        val path = Path()
        val drawable = widthPx > 0f && innerW > 0f && innerH > 0f
        if (drawable) {
            val innerCornerRadius = (cornerRadiusPx - halfStroke).coerceAtLeast(0f)
            path.addSquircleRect(
                width = innerW,
                height = innerH,
                cornerRadius = innerCornerRadius,
                squircleEnabled = squircleEnabled,
            )
        }
        val stroke = Stroke(width = widthPx)
        onDrawWithContent {
            drawContent()
            if (drawable) {
                translate(halfStroke, halfStroke) {
                    drawPath(path = path, brush = borderBrush, style = stroke)
                }
            }
        }
    }
}
