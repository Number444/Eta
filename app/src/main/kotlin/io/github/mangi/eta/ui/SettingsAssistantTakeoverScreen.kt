package io.github.mangi.eta.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsVoice
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.mangi.eta.R
import io.github.mangi.eta.config.PowerAssistantTarget
import io.github.mangi.eta.config.Prefs
import io.github.mangi.eta.systemizer.GoogleAppSystemizerInstaller
import io.github.mangi.eta.systemizer.RootManager
import io.github.mangi.eta.systemizer.SystemizerInstallResult
import io.github.mangi.eta.ui.app.rememberDeviceCapabilities
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaDropdownPreference
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
import top.yukonga.miuix.kmp.basic.DropdownItem

/**
 * 设置二级页：系统接管增强（电源键长按 / 默认助理 / 厂商助手兼容 / Gemini / 一圈即搜）。
 * 完整保留原 LSPosed（prefs != null || hasConnectedFramework）与 root/systemizer 条件渲染逻辑。
 */
@Composable
internal fun SettingsAssistantTakeoverScreen(
    context: Context,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val capabilities = rememberDeviceCapabilities()
    val frameworkState = rememberFrameworkPrefsState(context)
    val prefs = frameworkState.prefs
    val enhancementHistory = frameworkState.history
    var showSystemizerDialog by remember { mutableStateOf(false) }
    var installingSystemizer by remember { mutableStateOf(false) }

    var powerAssistantTarget by remember(prefs) {
        mutableStateOf(prefs?.let(Prefs::powerAssistantTarget) ?: enhancementHistory.powerTarget())
    }
    DisposableEffect(prefs) {
        val targetPrefs = prefs ?: return@DisposableEffect onDispose {}
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changedPrefs, key ->
            if (key == Prefs.Keys.POWER_KEY_ASSISTANT_TARGET ||
                key == Prefs.Keys.POWER_KEY_TAKEOVER
            ) {
                powerAssistantTarget = Prefs.powerAssistantTarget(changedPrefs)
            }
        }
        targetPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { targetPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val powerAssistantTargets = PowerAssistantTarget.entries
    val powerAssistantItems = powerAssistantTargets.map { target ->
        DropdownItem(text = target.displayName(context))
    }

    MiuixScaffoldPage(
        title = stringResource(R.string.ui_system_assistant_takes_over_f46043),
        onBack = onBack,
    ) {
        settingsTopSpacing()
        // ── 系统助手接管 ──────────────────────────────────────────────
        if (prefs != null || frameworkState.hasConnectedFramework) {
            item(key = "section_assistant_takeover") {
                EtaPreferenceGroupTitle(stringResource(R.string.ui_system_assistant_takes_over_f46043))
                EtaPreferenceGroup {
                    EtaDropdownPreference(
                        title = stringResource(R.string.ui_long_press_the_power_button_1958d0),
                        items = powerAssistantItems,
                        selectedIndex = powerAssistantTargets.indexOf(powerAssistantTarget),
                        onSelectedIndexChange = { index ->
                            val target = powerAssistantTargets.getOrNull(index)
                                ?: return@EtaDropdownPreference
                            val targetPrefs = prefs ?: return@EtaDropdownPreference
                            if (putStringSync(
                                    prefs = targetPrefs,
                                    key = Prefs.Keys.POWER_KEY_ASSISTANT_TARGET,
                                    value = target.persistedValue,
                                )
                            ) {
                                powerAssistantTarget = target
                                enhancementHistory.recordCommittedTarget(target)
                            } else {
                                Toast.makeText(
                                    context.applicationContext,
                                    context.getString(R.string.settings_write_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        startAction = {
                            EtaPreferenceIcon(
                                icon = Icons.Rounded.PowerSettingsNew,
                                tint = EtaPreferenceColors.Yellow,
                                enabled = prefs != null,
                            )
                        },
                        enabled = prefs != null,
                    )

                    EtaPreferenceDivider()
                    SwitchPref(
                        context = context,
                        prefs = prefs,
                        title = stringResource(R.string.ui_automatically_set_default_assistant_f86963),
                        key = Prefs.Keys.ASSISTANT_AUTO_CONFIG,
                        icon = Icons.Rounded.Settings,
                        iconTint = EtaPreferenceColors.Green,
                    )
                }
            }

            // ── 厂商助手兼容入口 ──────────────────────────────────────────
            item(key = "section_oem_assistant_compatibility") {
                EtaPreferenceGroupTitle(stringResource(R.string.ui_xiaobu_xiaoai_compatible_entrance_ae918a))
                EtaPreferenceGroup {
                    SwitchPref(
                        context = context,
                        prefs = prefs,
                        title = stringResource(R.string.ui_enable_vendor_assistant_custom_models_c8e465),
                        key = Prefs.Keys.AGENT_CUSTOM_MODEL,
                        icon = Icons.Rounded.Cloud,
                        iconTint = EtaPreferenceColors.Blue,
                    )

                    EtaPreferenceDivider()
                    SwitchPref(
                        context = context,
                        prefs = prefs,
                        title = stringResource(R.string.ui_only_take_over_with_agent_prefix_d17556),
                        key = Prefs.Keys.AGENT_REQUIRE_PREFIX,
                        icon = Icons.Rounded.FilterAlt,
                        iconTint = EtaPreferenceColors.Blue,
                    )
                }
            }
        }

        if (prefs != null || frameworkState.hasConnectedFramework || capabilities.root.isGranted || frameworkState.hasUsedSystemizer) {
            // ── Gemini ─────────────────────────────────────────────────
            item(key = "section_gemini") {
                EtaPreferenceGroupTitle("Gemini")
                EtaPreferenceGroup {
                    if (prefs != null || frameworkState.hasConnectedFramework) {
                        SwitchPref(
                            context = context,
                            prefs = prefs,
                            title = stringResource(R.string.ui_maintain_hey_google_detection_after_screen_rest_9d6877),
                            key = Prefs.Keys.HOTWORD_SELF_HEAL,
                            icon = Icons.Rounded.Hearing,
                            iconTint = EtaPreferenceColors.Green,
                        )

                        EtaPreferenceDivider()
                        SwitchPref(
                            context = context,
                            prefs = prefs,
                            title = stringResource(R.string.ui_lock_screen_evokes_automatic_voice_input_1cde18),
                            key = Prefs.Keys.LOCKSCREEN_VOICE_COMMAND,
                            icon = Icons.Rounded.Lock,
                            iconTint = EtaPreferenceColors.Blue,
                        )

                        EtaPreferenceDivider()
                        SwitchPref(
                            context = context,
                            prefs = prefs,
                            title = stringResource(R.string.ui_bright_screen_evokes_automatic_voice_input_4358fe),
                            key = Prefs.Keys.SCREEN_ON_VOICE_COMMAND,
                            icon = Icons.Rounded.SettingsVoice,
                            iconTint = EtaPreferenceColors.Green,
                        )

                    }
                    if (capabilities.root.isGranted || frameworkState.hasUsedSystemizer) {
                        if (prefs != null || frameworkState.hasConnectedFramework) {
                            EtaPreferenceDivider()
                        }
                        EtaArrowPreference(
                            title = stringResource(R.string.ui_convert_google_apps_to_system_apps_0f6d89),
                            startAction = {
                                EtaPreferenceIcon(
                                    icon = Icons.Rounded.Inventory,
                                    tint = EtaPreferenceColors.Orange,
                                    enabled = !installingSystemizer,
                                )
                            },
                            summary = if (capabilities.root.isGranted) null else stringResource(R.string.capability_root_required),
                            enabled = !installingSystemizer,
                            holdDownState = showSystemizerDialog,
                            onClick = {
                                if (!capabilities.root.isGranted) {
                                    onNavigate(AppRoute.SystemEnhance)
                                } else if (!installingSystemizer) {
                                    showSystemizerDialog = true
                                }
                            },
                        )
                    }
                }
            }
        }

        if (prefs != null || frameworkState.hasConnectedFramework) {
            // ── 一圈即搜 ────────────────────────────────────────────────
            item(key = "section_circle_to_search") {
                EtaPreferenceGroupTitle(stringResource(R.string.ui_search_in_one_turn_179584))
                EtaPreferenceGroup {
                    SwitchPref(
                        context = context,
                        prefs = prefs,
                        title = stringResource(R.string.ui_long_press_on_the_gesture_bar_triggers_a_circle_to_s_b80117),
                        key = Prefs.Keys.GESTURE_BAR_CIRCLE_TO_SEARCH,
                        icon = Icons.Rounded.SwipeUp,
                        iconTint = EtaPreferenceColors.Blue,
                    )

                    EtaPreferenceDivider()
                    SwitchPref(
                        context = context,
                        prefs = prefs,
                        title = stringResource(R.string.ui_long_press_with_two_fingers_to_trigger_a_circle_sear_ab597a),
                        key = Prefs.Keys.DOUBLE_FINGER_CIRCLE_TO_SEARCH,
                        icon = Icons.Rounded.TouchApp,
                        iconTint = EtaPreferenceColors.Blue,
                    )
                }
            }
        }
    }

    SystemizerConfirmDialog(
        show = showSystemizerDialog,
        installing = installingSystemizer,
        onDismissRequest = {
            if (!installingSystemizer) {
                showSystemizerDialog = false
            }
        },
        onConfirm = {
            if (installingSystemizer) return@SystemizerConfirmDialog
            if (!capabilities.root.isGranted) {
                showSystemizerDialog = false
                onNavigate(AppRoute.SystemEnhance)
                return@SystemizerConfirmDialog
            }
            enhancementHistory.recordSystemizerUse()
            frameworkState.hasUsedSystemizer = true
            showSystemizerDialog = false
            installingSystemizer = true
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    GoogleAppSystemizerInstaller(context.applicationContext).install()
                }
                installingSystemizer = false
                Toast.makeText(
                    context.applicationContext,
                    result.toToastMessage(context),
                    Toast.LENGTH_LONG,
                ).show()
            }
        },
    )
}

// ── 系统化确认对话框 ─────────────────────────────────────────────────────────

@Composable
private fun SystemizerConfirmDialog(
    show: Boolean,
    installing: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    EtaWindowDialog(
        show = show,
        title = stringResource(R.string.ui_convert_google_apps_to_system_apps_0f6d89),
        summary = stringResource(R.string.ui_system_applications_have_voice_wake_up_permissions_f_0190f2),
        onDismissRequest = onDismissRequest,
    ) {
        MiuixDialogActions(
            confirmText = if (installing) {
                stringResource(R.string.status_processing)
            } else {
                stringResource(R.string.action_confirm)
            },
            cancelEnabled = !installing,
            confirmEnabled = !installing,
            onCancel = onDismissRequest,
            onConfirm = onConfirm,
        )
    }
}

private fun PowerAssistantTarget.displayName(context: Context): String =
    when (this) {
        PowerAssistantTarget.OEM -> context.getString(R.string.power_assistant_system_default)
        PowerAssistantTarget.GEMINI -> "Gemini"
        PowerAssistantTarget.ETA -> "Eta"
    }

private fun SystemizerInstallResult.toToastMessage(context: Context): String =
    when (this) {
        SystemizerInstallResult.AlreadySystemized -> context.getString(R.string.systemizer_already_system)
        SystemizerInstallResult.GoogleAppMissing -> context.getString(R.string.systemizer_google_missing)
        SystemizerInstallResult.UnsupportedRootManager -> context.getString(R.string.systemizer_root_manager_missing)
        SystemizerInstallResult.KernelSuMetamoduleMissing -> context.getString(R.string.systemizer_metamodule_missing)
        is SystemizerInstallResult.RootPermissionUnavailable -> when (rootManager) {
            RootManager.KERNEL_SU -> context.getString(R.string.systemizer_grant_kernelsu)
            RootManager.MAGISK -> context.getString(R.string.systemizer_grant_magisk)
            RootManager.UNSUPPORTED -> context.getString(R.string.systemizer_root_denied)
        }
        is SystemizerInstallResult.InstalledRebootRequired -> context.getString(R.string.systemizer_installed)
        is SystemizerInstallResult.Failed -> commandOutput
            .lineSequence()
            .map { it.trim() }
            .lastOrNull { it.isNotEmpty() }
            ?.let { "$message：$it" }
            ?: message
    }
