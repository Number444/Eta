package io.github.mangi.eta.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
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
import io.github.mangi.eta.agent.tool.AgentSearchProvider
import io.github.mangi.eta.config.Prefs
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaOverlayDropdownPreference
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
                // 搜索引擎选择（Eta Mod）：Exa / DeepSeek，key 各自独立保存，切换不丢配置。
                var searchProvider by remember(agentPrefs) {
                    mutableStateOf(AgentSearchProvider.current())
                }
                EtaOverlayDropdownPreference(
                    title = stringResource(R.string.settings_search_provider_title),
                    summary = searchProvider.displayName,
                    items = AgentSearchProvider.values().map { it.displayName },
                    selectedIndex = searchProvider.ordinal,
                    startAction = {
                        EtaPreferenceIcon(Icons.AutoMirrored.Rounded.ManageSearch, tint = EtaPreferenceColors.Blue)
                    },
                    onSelectedIndexChange = { index ->
                        AgentSearchProvider.values().getOrNull(index)?.let { provider ->
                            if (agentPrefs?.let { putStringSync(it, Prefs.Keys.AGENT_SEARCH_PROVIDER, provider.prefValue) } == true) {
                                searchProvider = provider
                            } else {
                                Toast.makeText(
                                    context.applicationContext,
                                    context.getString(R.string.settings_write_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                )

                EtaPreferenceDivider()
                // 当前引擎的 API Key：与供应商 key 同款——明文存储在本机，界面掩码显示。
                var searchApiKey by remember(agentPrefs, searchProvider) {
                    mutableStateOf(agentPrefs?.getString(searchProvider.apiKeyPrefKey, "") ?: "")
                }
                var showSearchKeyDialog by remember { mutableStateOf(false) }
                EtaArrowPreference(
                    title = "${searchProvider.displayName} API Key",
                    summary = if (searchApiKey.isBlank()) {
                        stringResource(R.string.settings_exa_api_key_not_set)
                    } else {
                        maskSecretSummary(searchApiKey)
                    },
                    startAction = {
                        EtaPreferenceIcon(Icons.Rounded.Key, tint = EtaPreferenceColors.Orange)
                    },
                    onClick = { showSearchKeyDialog = true },
                )
                if (showSearchKeyDialog) {
                    ApiKeyDialog(
                        title = "${searchProvider.displayName} API Key",
                        initial = searchApiKey,
                        onDismiss = { showSearchKeyDialog = false },
                        onSave = { value ->
                            val trimmed = value.trim()
                            if (agentPrefs?.edit()
                                    ?.putString(searchProvider.apiKeyPrefKey, trimmed)
                                    ?.commit() == true
                            ) {
                                searchApiKey = trimmed
                            } else {
                                Toast.makeText(
                                    context.applicationContext,
                                    context.getString(R.string.settings_write_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            showSearchKeyDialog = false
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
