package io.github.mangi.eta.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun EtaDropdownPreference(
    title: String,
    items: List<DropdownItem>,
    selectedIndex: Int,
    startAction: (@Composable () -> Unit)? = null,
    summary: String? = null,
    modifier: Modifier = Modifier,
    bottomAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    // Eta Mod（焕新阶段四，Four 定调）：下拉弹层全真玻璃化——默认 overlay 变体（真玻璃）；
    // 仅剩侧栏长按菜单等特殊场景经 EtaListPopup 显式走 window 变体（不透明描边玻璃）。
    useWindow: Boolean = false,
    onSelectedIndexChange: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    val available = enabled && items.isNotEmpty()
    LaunchedEffect(available) {
        if (!available) { expanded = false; holding = false }
    }
    val haptics = LocalHapticFeedback.current
    val color = if (available) MiuixTheme.colorScheme.onSurfaceVariantActions
        else MiuixTheme.colorScheme.disabledOnSurface
    val entry = DropdownEntry(items.mapIndexed { index, item ->
        item.copy(selected = index == selectedIndex, onClick = {
            onSelectedIndexChange(index)
            item.onClick?.invoke()
        })
    })
    EtaPreferenceRow(
        title = title,
        summary = summary,
        modifier = modifier,
        bottomAction = bottomAction,
        startAction = startAction,
        enabled = available,
        holdDownState = holding && available,
        interaction = Modifier.clickable(enabled = available, role = Role.DropdownList) {
            expanded = !expanded
            if (expanded) {
                holding = true
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            }
        },
    ) {
        items.getOrNull(selectedIndex)?.text?.let { value ->
            Text(
                text = value,
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                style = MiuixTheme.textStyles.body2,
                color = color,
                textAlign = TextAlign.End,
            )
        }
        Row {
            DropdownArrowEndAction(actionColor = color)
            // Eta Mod（焕新阶段四）：弹层收口到 EtaListPopup——window 变体不透明描边玻璃、
            // overlay 变体真玻璃，材质单点生效；miuix WindowDropdownPopup 无修饰符入口。
            EtaListPopup(
                show = expanded && available,
                alignment = PopupPositionProvider.Align.End,
                useWindow = useWindow,
                onDismissRequest = { expanded = false },
                onDismissFinished = { holding = false },
                maxHeight = null,
            ) {
                ListPopupColumn {
                    entry.items.forEachIndexed { index, item ->
                        DropdownImpl(
                            item = item,
                            optionSize = entry.items.size,
                            isSelected = item.selected,
                            index = index,
                            onSelectedIndexChange = {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                expanded = false
                                item.onClick?.invoke()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EtaOverlayDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onSelectedIndexChange: (Int) -> Unit,
) = EtaDropdownPreference(
    title = title, items = items.map { DropdownItem(text = it) }, selectedIndex = selectedIndex,
    summary = summary, startAction = startAction, enabled = enabled,
    onSelectedIndexChange = onSelectedIndexChange,
)
