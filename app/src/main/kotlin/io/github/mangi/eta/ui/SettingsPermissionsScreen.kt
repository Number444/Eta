package io.github.mangi.eta.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.accessibility.AccessibilityProtectionClient
import io.github.mangi.eta.agent.accessibility.AgentAccessibilityService
import io.github.mangi.eta.agent.runtime.AgentIslandDiagnostics
import io.github.mangi.eta.agent.runtime.AgentIslandNotifier
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaPreferenceColors
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import io.github.mangi.eta.ui.components.EtaPreferenceGroup
import io.github.mangi.eta.ui.components.EtaPreferenceIcon
import io.github.mangi.eta.ui.components.EtaSwitchPreference
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 设置二级页：权限与通知（悬浮窗 / 无障碍 / 强制保持无障碍 / 超级岛 / 岛诊断）。 */
@Composable
internal fun SettingsPermissionsScreen(
    context: Context,
    onBack: () -> Unit,
) {
    val permissionState = rememberPermissionSummaryState(context)
    val frameworkState = rememberFrameworkPrefsState(context)
    var accessibilityProtectionPending by remember { mutableStateOf(false) }

    MiuixScaffoldPage(
        title = stringResource(R.string.settings_category_permissions_notifications),
        onBack = onBack,
    ) {
        settingsTopSpacing()
        item(key = "permissions") {
            EtaPreferenceGroup {
                EtaArrowPreference(
                    title = stringResource(R.string.ui_floating_window_permissions_076b77),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.Layers,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                    endActions = {
                        Text(
                            text = stringResource(
                                if (permissionState.overlayGranted) R.string.status_authorized else R.string.status_unauthorized,
                            ),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = if (permissionState.overlayGranted) {
                                MiuixTheme.colorScheme.onSurfaceVariantActions
                            } else {
                                MiuixTheme.colorScheme.error
                            },
                        )
                    },
                    onClick = {
                        if (!permissionState.overlayGranted) {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                        }
                    },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.ui_accessibility_enhancement_tools_8fd257),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.AccessibilityNew,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                    endActions = {
                        val enabled = permissionState.accessibilityGranted || AgentAccessibilityService.isAvailable()
                        Text(
                            text = stringResource(
                                if (enabled) R.string.status_enabled else R.string.status_disabled,
                            ),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = if (enabled) {
                                MiuixTheme.colorScheme.onSurfaceVariantActions
                            } else {
                                MiuixTheme.colorScheme.primary
                            },
                        )
                    },
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                            )
                        }
                    },
                )
                if (AccessibilityProtectionClient.isSupported() &&
                    (frameworkState.prefs != null || frameworkState.hasConnectedFramework)
                ) {
                    EtaPreferenceDivider()
                    EtaSwitchPreference(
                        title = stringResource(R.string.ui_enforce_accessibility_55e838),
                        checked = permissionState.accessibilityProtectionEnabled,
                        onCheckedChange = { enabled ->
                            if (accessibilityProtectionPending) {
                                return@EtaSwitchPreference
                            }
                            accessibilityProtectionPending = true
                            AccessibilityProtectionClient.setEnabled(
                                context = context,
                                enabled = enabled,
                            ) { result ->
                                accessibilityProtectionPending = false
                                permissionState.accessibilityProtectionEnabled = result.enabled
                                permissionState.accessibilityGranted = isAgentAccessibilityEnabled(context)
                                val failureMessage = when (result.status) {
                                    AccessibilityProtectionClient.ControlStatus.APPLIED -> null
                                    AccessibilityProtectionClient.ControlStatus.UNAVAILABLE ->
                                        context.getString(R.string.accessibility_protection_unavailable)
                                    AccessibilityProtectionClient.ControlStatus.REJECTED ->
                                        context.getString(R.string.accessibility_protection_rejected)
                                }
                                if (failureMessage != null) {
                                    Toast.makeText(
                                        context.applicationContext,
                                        failureMessage,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        startAction = {
                            EtaPreferenceIcon(
                                icon = Icons.Rounded.VerifiedUser,
                                tint = EtaPreferenceColors.Blue,
                                enabled = frameworkState.prefs != null && !accessibilityProtectionPending,
                            )
                        },
                        enabled = frameworkState.prefs != null && !accessibilityProtectionPending,
                    )
                }

                // Eta Mod：小米超级岛状态行；权限未开时点击直达系统通知设置。
                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.settings_island_title),
                    summary = stringResource(
                        when (permissionState.islandStatus) {
                            AgentIslandNotifier.SupportStatus.SUPPORTED ->
                                R.string.settings_island_summary_supported
                            AgentIslandNotifier.SupportStatus.NO_PERMISSION ->
                                R.string.settings_island_summary_no_permission
                            AgentIslandNotifier.SupportStatus.UNSUPPORTED ->
                                R.string.settings_island_summary_unsupported
                            AgentIslandNotifier.SupportStatus.CHECKING ->
                                R.string.settings_island_summary_checking
                        },
                    ),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.Notifications,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                    endActions = {
                        if (permissionState.islandStatus != AgentIslandNotifier.SupportStatus.CHECKING) {
                            val granted =
                                permissionState.islandStatus == AgentIslandNotifier.SupportStatus.SUPPORTED
                            Text(
                                text = stringResource(
                                    when (permissionState.islandStatus) {
                                        AgentIslandNotifier.SupportStatus.SUPPORTED ->
                                            R.string.status_authorized
                                        AgentIslandNotifier.SupportStatus.NO_PERMISSION ->
                                            R.string.status_unauthorized
                                        else -> R.string.status_unsupported
                                    },
                                ),
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = if (granted) {
                                    MiuixTheme.colorScheme.onSurfaceVariantActions
                                } else {
                                    MiuixTheme.colorScheme.error
                                },
                            )
                        }
                    },
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        }
                    },
                )

                // Eta Mod：岛通知诊断测试（临时工具，定位岛自动展开触发源）。
                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.settings_island_test_title),
                    summary = stringResource(R.string.settings_island_test_summary),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.BugReport,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                    onClick = {
                        val started = AgentIslandDiagnostics.start(context)
                        Toast.makeText(
                            context.applicationContext,
                            if (started) {
                                context.getString(R.string.settings_island_test_started)
                            } else {
                                context.getString(R.string.settings_island_test_running)
                            },
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                )
            }
        }
    }
}
