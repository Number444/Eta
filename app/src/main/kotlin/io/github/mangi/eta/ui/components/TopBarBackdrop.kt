package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import io.github.mangi.eta.ui.app.LocalBlurEnabled
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun rememberTopBarBackdrop(): LayerBackdrop? {
    if (!LocalBlurEnabled.current || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
internal fun TopBarBackdrop(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    Box {
        // 背景层：渐进模糊——顶端实、向下缘淡出，滚入内容柔和消隐。
        // 注意：textureBlur 系 modifier 不能被 graphicsLayer(Offscreen)+DstIn 遮罩包裹，
        // 离屏合成会让内部 RuntimeShader 模糊静默失效（2026-10-08 实锤）；要做渐变淡出
        // 只能用库自带的 progressiveTextureBlur，它是单节点实现，无需离屏遮罩。
        val backgroundModifier = if (backdrop == null) {
            Modifier.background(surfaceColor)
        } else {
            Modifier.progressiveTextureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                blurRadius = TopBarBlurRadius,
                colors = BlurColors(
                    blendColors = listOf(
                        // Eta Mod：0.8 → 0.5，原值下滚动内容只剩 20% 透色，模糊弱到近乎不可感知。
                        BlendColorEntry(surfaceColor.copy(alpha = TopBarSurfaceAlpha)),
                    ),
                ),
            )
        }
        Box(modifier = Modifier.matchParentSize().then(backgroundModifier))
        content()
    }
}

internal fun Modifier.captureForTopBar(backdrop: LayerBackdrop?): Modifier =
    if (backdrop == null) this else layerBackdrop(backdrop)

@Composable
internal fun topBarContainerColor(backdrop: LayerBackdrop?): Color =
    if (backdrop == null) MiuixTheme.colorScheme.surface else Color.Transparent

// 注意：miuix blur 内部会把 blurRadius × 0.45 再量化到 2 的幂降采样档，
// 30f 实际生效约 13.5px（≈4dp），需要更强涂抹感时按 ÷0.45 反推。
private const val TopBarBlurRadius = 30f
private const val TopBarSurfaceAlpha = 0.5f
