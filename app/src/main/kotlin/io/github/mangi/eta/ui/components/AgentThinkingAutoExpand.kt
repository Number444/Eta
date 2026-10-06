package io.github.mangi.eta.ui.components

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.mangi.eta.config.Prefs

/**
 * 读取「自动展开思考过程」开关（Eta Mod）。默认开启，与上游行为一致；
 * 关闭后流式思考与工作过程保持收起，点击标题栏手动展开。
 */
@Composable
internal fun rememberAutoExpandThinking(): Boolean {
    val prefs = remember { Prefs.localAgentPreferences() }
    var enabled by remember {
        mutableStateOf(
            prefs?.getBoolean(Prefs.Keys.AGENT_AUTO_EXPAND_THINKING, true) ?: true
        )
    }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key == Prefs.Keys.AGENT_AUTO_EXPAND_THINKING) {
                enabled = changed.getBoolean(key, true)
            }
        }
        prefs?.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs?.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled
}
