package io.github.mangi.eta.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Eta Mod：点击触感统一入口，默认轻点档（KeyboardTap）。
 * 返回稳定 lambda：`onClick` 更新不会导致调用方拿到新引用而失效重组。
 */
@Composable
internal fun hapticClick(
    type: HapticFeedbackType = HapticFeedbackType.KeyboardTap,
    onClick: () -> Unit,
): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    return remember(haptics, type) {
        {
            haptics.performHapticFeedback(type)
            currentOnClick()
        }
    }
}
