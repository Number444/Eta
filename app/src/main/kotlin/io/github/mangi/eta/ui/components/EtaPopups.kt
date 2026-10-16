package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.mangi.eta.ui.app.LocalLiquidGlassEnabled
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

/**
 * Eta Mod：列表弹层统一收口（UI 焕新架构改动二）。
 *
 * 全 app 的 ListPopup 只允许走这里，材质切换（真玻璃/假玻璃）单点生效，
 * 参数面与 miuix 严格对齐（drop-in，控制 merge 冲突裁决成本）。
 *
 * - 默认 overlay 实现：在应用渲染树内，可采样同 surface 的液态玻璃 backdrop（真玻璃）。
 *   前提：调用点上方必须有 miuix Scaffold 提供 overlay 宿主，否则弹层静默不渲染。
 * - [useWindow] = true 走独立 Window：无 Scaffold 宿主的区域（侧边栏面板等）使用；
 *   Window 采不到应用内 backdrop，材质为不透明描边玻璃（原生底 + etaPseudoGlassBorder）。
 *
 * overlay 分支内容外包 [EtaGlassPopupContainer]（真玻璃，
 * backdrop 为 null 时自动回退原生外观），已随焕新阶段二落地；
 * Window 分支的不透明描边玻璃已随阶段三落地（半透明假玻璃观感差，2026-10-17 修订）。
 */
@Composable
internal fun EtaListPopup(
    show: Boolean,
    popupModifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    enableWindowDim: Boolean = true,
    useWindow: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    maxHeight: Dp? = null,
    minWidth: Dp = ListPopupDefaults.MinWidth,
    content: @Composable () -> Unit,
) {
    if (useWindow) {
        if (LocalLiquidGlassEnabled.current) {
            // 焕新阶段三（Four 2026-10-17 修订）：Window 弹层改不透明描边玻璃——
            // 半透明假玻璃实测观感差，恢复原生不透明底，仅叠假玻璃高光描边。
            WindowListPopup(
                show = show,
                popupModifier = popupModifier,
                popupPositionProvider = popupPositionProvider,
                alignment = alignment,
                enableWindowDim = enableWindowDim,
                onDismissRequest = onDismissRequest,
                onDismissFinished = onDismissFinished,
                maxHeight = maxHeight,
                minWidth = minWidth,
            ) {
                Box(Modifier.etaPseudoGlassBorder(EtaGlassStyles.Popup)) {
                    content()
                }
            }
        } else {
            WindowListPopup(
                show = show,
                popupModifier = popupModifier,
                popupPositionProvider = popupPositionProvider,
                alignment = alignment,
                enableWindowDim = enableWindowDim,
                onDismissRequest = onDismissRequest,
                onDismissFinished = onDismissFinished,
                maxHeight = maxHeight,
                minWidth = minWidth,
                content = content,
            )
        }
    } else {
        OverlayListPopup(
            show = show,
            popupModifier = popupModifier,
            popupPositionProvider = popupPositionProvider,
            alignment = alignment,
            enableWindowDim = enableWindowDim,
            onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished,
            maxHeight = maxHeight,
            minWidth = minWidth,
        ) {
            EtaGlassPopupContainer { content() }
        }
    }
}

/**
 * overlay 弹层内容的真玻璃注入容器。
 *
 * 弹层尺寸由内容决定（ListPopupColumn 自适应），此 Box 恰好覆盖整个弹层可视区，
 * 盖在 miuix 容器的实色底之上。轮廓固定 16dp 圆角——Kyant lens 效果只支持
 * CornerBasedShape/RoundedRectangularShape（通用 Shape 会抛 UnsupportedOperationException，
 * v3.3.0-010 曾因此闪退），无法用 squircle 与容器 clip 路径逐像素对齐；
 * squircle 与圆角在 16dp 下的差异为亚像素级，真机观察。
 *
 * 注意 miuix 弹层条目（DropdownImpl）默认给每项画不透明 surfaceContainer 底，
 * 会从内侧盖住玻璃只剩高光（010 第二包事故）；此处经 MiuixTheme 局部覆写把
 * surfaceContainer 改为透明（条目选中底色原本就等于容器底色，无视觉损失）。
 *
 * backdrop 为 null（非液态玻璃档/设备不支持）时不包任何修饰，弹层保持原生外观。
 */
@Composable
private fun EtaGlassPopupContainer(content: @Composable () -> Unit) {
    val backdrop = LocalLiquidGlassBackdrop.current
    if (backdrop == null) {
        content()
        return
    }
    val style = EtaGlassStyles.Popup
    val colors = MiuixTheme.colorScheme
    val glassColors = remember(colors) { colors.copy(surfaceContainer = Color.Transparent) }
    MiuixTheme(colors = glassColors) {
        Box(
            Modifier.etaGlass(
                style = style,
                backdrop = backdrop,
                tint = glassColors.surface.copy(alpha = style.tintAlpha),
            ),
        ) {
            content()
        }
    }
}
