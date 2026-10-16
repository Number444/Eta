package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import io.github.mangi.eta.ui.app.LocalLiquidGlassEnabled
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Eta Mod：Kyant0 Backdrop 液态玻璃，仅用于首页（顶栏双按钮 + 底部输入栏）。
 *
 * 由 LocalLiquidGlassEnabled 门控（设置→外观→模糊效果选「液态玻璃」）：
 * 未开启或设备不支持 RuntimeShader 时 backdrop 为 null，
 * 所有 glass modifier 退化为 no-op，界面回退到现有外观。
 *
 * 注意：Backdrop 录制层（liquidGlassSource）只能包消息内容，不能包住任何
 * drawBackdrop 控件，否则渲染回路会炸（嵌套玻璃需 exportedBackdrop，此处不用）。
 */
internal val LocalLiquidGlassBackdrop = compositionLocalOf<LayerBackdrop?> { null }

@Composable
internal fun rememberLiquidGlassBackdrop(): LayerBackdrop? {
    if (!LocalLiquidGlassEnabled.current || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    // 必须先铺不透明底色再画内容，否则模糊采样到透明像素出现黑边
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/** 标记玻璃效果的背景采样源（滚动的消息内容）。 */
internal fun Modifier.liquidGlassSource(backdrop: LayerBackdrop?): Modifier =
    if (backdrop == null) this else layerBackdrop(backdrop)

/**
 * 在控件底下垫一个独立尺寸的圆形玻璃层：控件本体（布局、图标、触控）不参与，
 * 只把玻璃圆以固定 size 居中放在控件后面。
 */
@Composable
internal fun LiquidGlassCircleBehind(
    backdrop: LayerBackdrop?,
    tint: Color,
    size: Dp,
) {
    if (backdrop != null) {
        Box(Modifier.size(size).liquidGlassCircle(backdrop, tint))
    }
}

/** 圆形液态玻璃遮罩：顶栏按钮与输入栏内图标按钮用。 */
internal fun Modifier.liquidGlassCircle(
    backdrop: LayerBackdrop?,
    tint: Color = Color.Unspecified,
): Modifier = if (backdrop == null) {
    this
} else {
    drawBackdrop(
        backdrop = backdrop,
        shape = { CircleShape },
        effects = {
            vibrancy()
            blur(10.dp.toPx())
            lens(6.dp.toPx(), 10.dp.toPx())
        },
        highlight = { Highlight.Default },
        shadow = null,
        onDrawSurface = {
            if (tint.isSpecified) drawRect(tint)
        },
    )
}

/** 圆角液态玻璃材质：输入栏容器用。 */
internal fun Modifier.liquidGlassRounded(
    backdrop: LayerBackdrop?,
    cornerRadius: Dp,
    tint: Color = Color.Unspecified,
): Modifier = if (backdrop == null) {
    this
} else {
    drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedCornerShape(cornerRadius) },
        effects = {
            vibrancy()
            blur(8.dp.toPx())
            lens(8.dp.toPx(), 12.dp.toPx())
        },
        highlight = { Highlight.Default },
        shadow = null,
        onDrawSurface = {
            if (tint.isSpecified) drawRect(tint)
        },
    )
}
