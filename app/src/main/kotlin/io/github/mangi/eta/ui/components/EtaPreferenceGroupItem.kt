package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.app.LocalLiquidGlassEnabled
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 长列表保持逐项组合，只对分组首尾绘制外侧圆角。 */
@Composable
internal fun EtaPreferenceGroupItem(
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    hasLeading: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val radius = EtaCardDefaults.CornerRadius
    // Eta Mod（焕新阶段四）：液态玻璃档下分组条目改半透明假玻璃底（surface × 0.72）。
    // 不描边——逐条目描边会在条目间穿帮；页面底色灰白化由骨架 Scaffold 提供。
    val fillColor = if (LocalLiquidGlassEnabled.current) {
        colors.surface.copy(alpha = EtaGlassStyles.ListRow.pseudoBackgroundAlpha)
    } else {
        colors.surfaceContainer
    }
    val surface = if (isFirst || isLast) {
        Modifier.squircleSurface(
            color = fillColor,
            topStart = if (isFirst) radius else 0.dp,
            topEnd = if (isFirst) radius else 0.dp,
            bottomStart = if (isLast) radius else 0.dp,
            bottomEnd = if (isLast) radius else 0.dp,
        )
    } else Modifier.background(fillColor)
    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceContainer) {
        Column(modifier.fillMaxWidth().padding(horizontal = EtaPreferenceDefaults.SidePadding).then(surface)) {
            content()
            if (!isLast) EtaPreferenceDivider(hasLeading = hasLeading)
        }
    }
}
