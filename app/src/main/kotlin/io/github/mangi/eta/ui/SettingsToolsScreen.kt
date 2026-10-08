package io.github.mangi.eta.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.mangi.eta.R
import io.github.mangi.eta.config.Prefs
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaPreferenceColors
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import io.github.mangi.eta.ui.components.EtaPreferenceGroup
import io.github.mangi.eta.ui.components.EtaPreferenceIcon
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import io.github.mangi.eta.ui.navigation.AppRoute

/** 设置二级页：工具（工具列表 / 网页浏览与搜索 / 设备直达 / 终端 / Linux / Root 增强入口）。 */
@Composable
internal fun SettingsToolsScreen(
    context: Context,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val agentPrefs = remember { Prefs.localAgentPreferences() }

    MiuixScaffoldPage(
        title = stringResource(R.string.ui_tool_a72ef1),
        onBack = onBack,
    ) {
        settingsTopSpacing()
        item(key = "tools") {
            EtaPreferenceGroup {
                EtaArrowPreference(
                    title = stringResource(R.string.settings_tools_list),
                    startAction = { EtaPreferenceIcon(Icons.Rounded.Dashboard, tint = EtaPreferenceColors.Green) },
                    onClick = { onNavigate(AppRoute.Tools) },
                )

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.ui_enable_web_browsing_tools_8b6b03),
                    key = Prefs.Keys.AGENT_BROWSER_TOOLS,
                    icon = Icons.Rounded.Language,
                    iconTint = EtaPreferenceColors.Blue,
                )

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.settings_web_search_title),
                    summary = stringResource(R.string.settings_web_search_summary),
                    key = Prefs.Keys.AGENT_WEB_SEARCH,
                    icon = Icons.Rounded.TravelExplore,
                    iconTint = EtaPreferenceColors.Blue,
                )

                EtaPreferenceDivider()
                // Exa API Key：与供应商 key 同款——明文存储在本机，界面掩码显示。
                var exaApiKey by remember(agentPrefs) {
                    mutableStateOf(agentPrefs?.getString(Prefs.Keys.AGENT_EXA_API_KEY, "") ?: "")
                }
                var showExaKeyDialog by remember { mutableStateOf(false) }
                EtaArrowPreference(
                    title = "Exa API Key",
                    summary = if (exaApiKey.isBlank()) {
                        stringResource(R.string.settings_exa_api_key_not_set)
                    } else {
                        maskSecretSummary(exaApiKey)
                    },
                    startAction = {
                        EtaPreferenceIcon(Icons.Rounded.Key, tint = EtaPreferenceColors.Orange)
                    },
                    onClick = { showExaKeyDialog = true },
                )
                if (showExaKeyDialog) {
                    ExaApiKeyDialog(
                        initial = exaApiKey,
                        onDismiss = { showExaKeyDialog = false },
                        onSave = { value ->
                            val trimmed = value.trim()
                            if (agentPrefs?.edit()
                                    ?.putString(Prefs.Keys.AGENT_EXA_API_KEY, trimmed)
                                    ?.commit() == true
                            ) {
                                exaApiKey = trimmed
                            } else {
                                Toast.makeText(
                                    context.applicationContext,
                                    context.getString(R.string.settings_write_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            showExaKeyDialog = false
                        },
                    )
                }

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.ui_enable_device_direct_tools_e2d595),
                    key = Prefs.Keys.AGENT_DEVICE_DIRECT_TOOLS,
                    icon = Icons.Rounded.Smartphone,
                    iconTint = EtaPreferenceColors.Green,
                )

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.ui_allow_reading_of_sensitive_device_information_feaec0),
                    key = Prefs.Keys.AGENT_DEVICE_SENSITIVE_READ_TOOLS,
                    icon = Icons.Rounded.Visibility,
                    iconTint = EtaPreferenceColors.Blue,
                )

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.ui_allow_sensitive_device_operation_3d42ea),
                    key = Prefs.Keys.AGENT_DEVICE_SENSITIVE_ACTION_TOOLS,
                    icon = Icons.Rounded.GppMaybe,
                    iconTint = EtaPreferenceColors.Orange,
                )

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.ui_enable_terminal_file_tools_18bb43),
                    key = Prefs.Keys.AGENT_TERMINAL_TOOLS,
                    icon = Icons.Rounded.Terminal,
                    iconTint = EtaPreferenceColors.Green,
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.ui_linux_tool_environment_314d22),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.Inventory2,
                            tint = EtaPreferenceColors.Orange,
                        )
                    },
                    onClick = { onNavigate(AppRoute.LinuxEnvironment) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.capability_enhancements),
                    startAction = { EtaPreferenceIcon(Icons.Rounded.Security, tint = EtaPreferenceColors.Blue) },
                    onClick = { onNavigate(AppRoute.SystemEnhance) },
                )
            }
        }
    }
}
