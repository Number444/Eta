package io.github.mangi.eta.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.ImportContacts
import androidx.compose.material.icons.rounded.SportsBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaPreferenceColors
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import io.github.mangi.eta.ui.components.EtaPreferenceGroup
import io.github.mangi.eta.ui.components.EtaPreferenceIcon
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import io.github.mangi.eta.ui.navigation.AppRoute

/** 设置二级页：扩展与角色（记忆 / Skills / MCP 服务器 / 角色入口）。 */
@Composable
internal fun SettingsExtensionsScreen(
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    MiuixScaffoldPage(
        title = stringResource(R.string.settings_category_extensions),
        onBack = onBack,
    ) {
        settingsTopSpacing()
        item(key = "extensions") {
            EtaPreferenceGroup {
                EtaArrowPreference(
                    title = stringResource(R.string.ui_memory_b55ff5),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            tint = EtaPreferenceColors.Orange,
                        )
                    },
                    onClick = { onNavigate(AppRoute.Memory) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.route_skills),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.ImportContacts,
                            tint = EtaPreferenceColors.Green,
                        )
                    },
                    onClick = { onNavigate(AppRoute.Skills) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.route_mcp_servers),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.AccountTree,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                    onClick = { onNavigate(AppRoute.McpServers) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = "角色",
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.SportsBar,
                            tint = EtaPreferenceColors.Orange,
                        )
                    },
                    onClick = { onNavigate(AppRoute.Characters) },
                )
            }
        }
    }
}
