package io.github.mangi.eta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.app.LocalLiquidGlassEnabled
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun EtaSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val liquidGlass = LocalLiquidGlassEnabled.current
    InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        label = label,
        color = if (liquidGlass) Color.Transparent else MiuixTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.then(
            if (liquidGlass) {
                Modifier.etaPseudoGlass(EtaGlassStyles.InputBar.copy(shape = CircleShape))
            } else {
                Modifier
            },
        ),
    )
}

@Composable
internal fun EtaTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalLiquidGlassEnabled.current) {
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = modifier,
        )
        return
    }
    Row(
        modifier = modifier.fillMaxWidth().height(TabRowDefaults.TabRowHeight),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedTabIndex
            val background by animateColorAsState(
                targetValue = if (selected) MiuixTheme.colorScheme.surfaceContainer else Color.Transparent,
                label = "tab_background",
            )
            val content by animateColorAsState(
                targetValue = if (selected) MiuixTheme.colorScheme.onBackground else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                label = "tab_content",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .squircleBackground(background, TabRowDefaults.TabRowCornerRadius)
                    .etaSquircleGlassBorder(TabRowDefaults.TabRowCornerRadius, EtaGlassStyles.ListRow)
                    .semantics { this.selected = selected }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onTabSelected(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab,
                    color = content,
                    fontSize = MiuixTheme.textStyles.body1.fontSize,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal object EtaControlDefaults {
    val ButtonCornerRadius = 22.dp
    val ButtonMinHeight = 44.dp
    val ButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    val DialogInsideMargin = DpSize(24.dp, 24.dp)
    val DialogOutsideMargin = DpSize(16.dp, 16.dp)
}

@Composable
internal fun EtaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = EtaControlDefaults.ButtonCornerRadius,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = EtaControlDefaults.ButtonMinHeight,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues = EtaControlDefaults.ButtonPadding,
    textStyle: TextStyle? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
) = TextButton(
    text = text, onClick = onClick, modifier = modifier, enabled = enabled,
    cornerRadius = cornerRadius, minWidth = minWidth, minHeight = minHeight,
    colors = colors, insideMargin = insideMargin, textStyle = textStyle,
    interactionSource = interactionSource, indication = indication,
)

@Composable
internal fun EtaWindowDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    EtaPreferenceTheme {
        WindowDialog(
            show = show, title = title, summary = summary,
            // Eta Mod（焕新阶段四）：Window 弹窗不透明描边玻璃——原生不透明底，
            // 描边压在 squircleSurface 填充上（miuix user modifier 先于内部填充）。
            modifier = modifier.etaSquircleGlassBorder(DialogDefaults.CornerRadius, EtaGlassStyles.DialogCard),
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
            onDismissRequest = onDismissRequest, onDismissFinished = onDismissFinished,
            cornerRadius = DialogDefaults.CornerRadius,
            insideMargin = EtaControlDefaults.DialogInsideMargin,
            outsideMargin = EtaControlDefaults.DialogOutsideMargin,
            maxWidth = DialogDefaults.MaxWidth,
            content = content,
        )
    }
}

@Composable
internal fun EtaOverlayDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    EtaPreferenceTheme {
        OverlayDialog(
            show = show, title = title, summary = summary,
            // Eta Mod（焕新阶段四）：同 EtaWindowDialog，统一不透明描边玻璃（Four 拍板，
            // 模态弹窗底下有 dim，真玻璃模糊收益小，含 IME 弹窗少一层渲染风险）。
            modifier = modifier.etaSquircleGlassBorder(DialogDefaults.CornerRadius, EtaGlassStyles.DialogCard),
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
            onDismissRequest = onDismissRequest, onDismissFinished = onDismissFinished,
            cornerRadius = DialogDefaults.CornerRadius,
            insideMargin = EtaControlDefaults.DialogInsideMargin,
            outsideMargin = EtaControlDefaults.DialogOutsideMargin,
            maxWidth = DialogDefaults.MaxWidth,
            content = content,
        )
    }
}

@Composable
internal fun EtaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = Switch(
    checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
    modifier = modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = 44.dp.roundToPx()
        val height = 24.dp.roundToPx()
        layout(width, height) {
            placeable.placeWithLayer((width - placeable.width) / 2, (height - placeable.height) / 2) {
                scaleX = width.toFloat() / placeable.width
                scaleY = height.toFloat() / placeable.height
            }
        }
    },
)
