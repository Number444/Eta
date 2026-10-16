package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.app.LocalLiquidGlassEnabled
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

internal object EtaCardDefaults {
    val CornerRadius = 24.dp
}

/**
 * Eta Mod（焕新阶段四）：液态玻璃档下卡片默认色改半透明底（假玻璃），
 * 高光描边由 [EtaCard] 的 modifier 侧补；其余档保持 miuix 原生实色。
 */
@Composable
private fun etaGlassCardColors(): CardColors {
    val base = CardDefaults.defaultColors()
    if (!LocalLiquidGlassEnabled.current) return base
    return CardColors(
        color = MiuixTheme.colorScheme.surface.copy(alpha = EtaGlassStyles.ListRow.pseudoBackgroundAlpha),
        contentColor = base.contentColor,
    )
}

@Composable
internal fun EtaCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = etaGlassCardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.etaSquircleGlassBorder(EtaCardDefaults.CornerRadius),
        cornerRadius = EtaCardDefaults.CornerRadius,
        insideMargin = insideMargin,
        colors = colors,
        content = content,
    )
}

@Composable
internal fun EtaCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = etaGlassCardColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.etaSquircleGlassBorder(EtaCardDefaults.CornerRadius),
        cornerRadius = EtaCardDefaults.CornerRadius,
        insideMargin = insideMargin,
        colors = colors,
        pressFeedbackType = pressFeedbackType,
        showIndication = showIndication,
        holdDownState = holdDownState,
        onClick = onClick,
        onLongPress = onLongPress,
        content = content,
    )
}
