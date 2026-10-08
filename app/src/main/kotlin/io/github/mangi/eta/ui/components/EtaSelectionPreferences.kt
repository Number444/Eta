package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.preference.CheckboxLocation

@Composable
internal fun EtaSwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    // 确认档：整行点按补与 miuix 控件球一致的 ToggleOn/ToggleOff 触感，消除"点球有震、点行无震"
    val haptics = LocalHapticFeedback.current
    EtaPreferenceRow(
        title = title, summary = summary, modifier = modifier,
        startAction = startAction, bottomAction = bottomAction, enabled = enabled,
        interaction = Modifier.toggleable(
            value = checked, enabled = enabled, role = Role.Switch,
            onValueChange = { newValue ->
                haptics.performHapticFeedback(if (newValue) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(newValue)
            },
        ),
    ) {
        EtaSwitch(
            checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
internal fun EtaRadioButtonPreference(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    startAction: (@Composable () -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
) {
    // 确认档：仅在从未选中变为选中时震，与 miuix RadioButton 内部行为对齐
    val haptics = LocalHapticFeedback.current
    EtaPreferenceRow(
        title = title, summary = summary, modifier = modifier,
        startAction = startAction, bottomAction = bottomAction, enabled = enabled,
        interaction = Modifier.selectable(
            selected = selected, enabled = enabled, role = Role.RadioButton,
            onClick = {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                onClick()
            },
        ),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
internal fun EtaCheckboxPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    checkboxLocation: CheckboxLocation = CheckboxLocation.End,
    startAction: (@Composable () -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
) {
    val checkbox: @Composable () -> Unit = {
        Checkbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = null, enabled = enabled, modifier = Modifier.clearAndSetSemantics {},
        )
    }
    val leading: (@Composable () -> Unit)? = if (checkboxLocation == CheckboxLocation.Start) {
        { Row { checkbox(); startAction?.let { Spacer(Modifier.width(8.dp)); it() } } }
    } else startAction
    val trailing: (@Composable RowScope.() -> Unit)? = if (checkboxLocation == CheckboxLocation.End) {
        { checkbox() }
    } else null
    // 确认档：整行点按补与 miuix 控件一致的 ToggleOn/ToggleOff 触感
    val haptics = LocalHapticFeedback.current
    EtaPreferenceRow(
        title = title, summary = summary, modifier = modifier,
        startAction = leading, endActions = trailing, bottomAction = bottomAction, enabled = enabled,
        interaction = if (onCheckedChange != null) Modifier.toggleable(
            value = checked, enabled = enabled, role = Role.Checkbox,
            onValueChange = { newValue ->
                haptics.performHapticFeedback(if (newValue) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(newValue)
            },
        ) else Modifier,
    )
}
