package io.github.mangi.eta.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.mangi.eta.R
import io.github.mangi.eta.data.update.AppLatestRelease
import io.github.mangi.eta.data.update.AppUpdateChecker
import io.github.mangi.eta.ui.app.rememberDeviceCapabilities
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaPreference
import io.github.mangi.eta.ui.components.EtaPreferenceColors
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import io.github.mangi.eta.ui.components.EtaPreferenceGroup
import io.github.mangi.eta.ui.components.EtaPreferenceGroupTitle
import io.github.mangi.eta.ui.components.EtaPreferenceIcon
import io.github.mangi.eta.ui.components.EtaWindowDialog
import io.github.mangi.eta.ui.components.MiuixDialogActions
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import io.github.mangi.eta.ui.navigation.AppRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置一级页：分类入口 + 权限未授权计数 + 关于区。
 * 各分类明细在对应二级页（[AppRoute.SettingsModel] 等）。
 */
@Composable
internal fun SettingsScreen(
    context: Context,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val capabilities = rememberDeviceCapabilities()
    val permissionState = rememberPermissionSummaryState(context)
    val frameworkState = rememberFrameworkPrefsState(context)

    // 关于组：版本信息与更新检查。结果对话框在列表外渲染，状态需要页面级 owner。
    val appPackageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val appVersionName = appPackageInfo.versionName.orEmpty()
    val appVersionSummary = "${appPackageInfo.versionName} (${appPackageInfo.longVersionCode})"
    val openUrl: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    var checkingUpdate by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppLatestRelease?>(null) }
    val checkForUpdate: () -> Unit = {
        if (!checkingUpdate) {
            checkingUpdate = true
            coroutineScope.launch {
                val release = runCatching {
                    withContext(Dispatchers.IO) { AppUpdateChecker.fetchLatest() }
                }.getOrNull()
                checkingUpdate = false
                when {
                    release == null -> Toast.makeText(
                        context.applicationContext,
                        context.getString(R.string.ui_update_check_failed),
                        Toast.LENGTH_SHORT,
                    ).show()

                    AppUpdateChecker.isNewer(release.version, appVersionName) ->
                        availableUpdate = release

                    else -> Toast.makeText(
                        context.applicationContext,
                        context.getString(R.string.ui_update_already_latest),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    val showTakeoverEntry = frameworkState.prefs != null || frameworkState.hasConnectedFramework ||
        capabilities.root.isGranted || frameworkState.hasUsedSystemizer

    MiuixScaffoldPage(
        title = stringResource(R.string.ui_set_up_7debf9),
        onBack = onBack,
    ) {
        settingsTopSpacing()
        // ── 分类入口 ────────────────────────────────────────────────
        item(key = "section_categories") {
            EtaPreferenceGroup {
                EtaArrowPreference(
                    title = stringResource(R.string.settings_category_model_conversation),
                    startAction = {
                        EtaPreferenceIcon(icon = Icons.Rounded.Psychology, tint = EtaPreferenceColors.Blue)
                    },
                    onClick = { onNavigate(AppRoute.SettingsModel) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.settings_category_extensions),
                    startAction = {
                        EtaPreferenceIcon(icon = Icons.Rounded.Extension, tint = EtaPreferenceColors.Green)
                    },
                    onClick = { onNavigate(AppRoute.SettingsExtensions) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.ui_tool_a72ef1),
                    startAction = {
                        EtaPreferenceIcon(icon = Icons.Rounded.Build, tint = EtaPreferenceColors.Green)
                    },
                    onClick = { onNavigate(AppRoute.SettingsTools) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.settings_general),
                    startAction = {
                        EtaPreferenceIcon(icon = Icons.Rounded.Tune, tint = EtaPreferenceColors.Blue)
                    },
                    onClick = { onNavigate(AppRoute.SettingsGeneral) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.settings_category_permissions_notifications),
                    startAction = {
                        EtaPreferenceIcon(icon = Icons.Rounded.Security, tint = EtaPreferenceColors.Blue)
                    },
                    endActions = {
                        // 未授权计数聚合悬浮窗/无障碍/超级岛三状态，入口可见性不因入二级而丢失。
                        if (permissionState.unauthorizedCount > 0) {
                            Text(
                                text = stringResource(
                                    R.string.settings_permissions_unauthorized_count,
                                    permissionState.unauthorizedCount,
                                ),
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = MiuixTheme.colorScheme.error,
                            )
                        }
                    },
                    onClick = { onNavigate(AppRoute.SettingsPermissions) },
                )

                // 系统接管增强：仅 LSPosed/root/systemizer 条件满足时入口出现。
                if (showTakeoverEntry) {
                    EtaPreferenceDivider()
                    EtaArrowPreference(
                        title = stringResource(R.string.ui_system_assistant_takes_over_f46043),
                        startAction = {
                            EtaPreferenceIcon(icon = Icons.Rounded.SupportAgent, tint = EtaPreferenceColors.Green)
                        },
                        onClick = { onNavigate(AppRoute.SettingsAssistantTakeover) },
                    )
                }
            }
        }

        // ── 关于（留一级页，保持一键直达） ──────────────────────────────
        item(key = "section_about") {
            EtaPreferenceGroupTitle(stringResource(R.string.ui_about_bed172))
            EtaPreferenceGroup {
                EtaPreference(
                    title = stringResource(R.string.ui_about_version_title),
                    summary = appVersionSummary,
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.Info,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.ui_about_update_title),
                    summary = if (checkingUpdate) {
                        stringResource(R.string.ui_about_update_checking)
                    } else {
                        null
                    },
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.SystemUpdate,
                            tint = EtaPreferenceColors.Green,
                            enabled = !checkingUpdate,
                        )
                    },
                    enabled = !checkingUpdate,
                    onClick = checkForUpdate,
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.ui_about_feedback_title),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.BugReport,
                            tint = EtaPreferenceColors.Orange,
                        )
                    },
                    onClick = { openUrl("https://github.com/Number444/Eta/issues") },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.ui_about_github_star_title),
                    summary = stringResource(R.string.ui_about_github_star_hint),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.Code,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                    onClick = { openUrl("https://github.com/Number444/Eta") },
                )
            }
        }
    }

    availableUpdate?.let { update ->
        EtaWindowDialog(
            show = true,
            title = stringResource(R.string.ui_update_available_title),
            summary = stringResource(R.string.ui_update_available_message, update.version, appVersionName),
            onDismissRequest = { availableUpdate = null },
        ) {
            MiuixDialogActions(
                confirmText = stringResource(R.string.ui_update_go_download),
                onCancel = { availableUpdate = null },
                onConfirm = {
                    availableUpdate = null
                    openUrl(update.url)
                },
            )
        }
    }
}
