package io.github.mangi.eta.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaPreferenceColors
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import io.github.mangi.eta.ui.components.EtaPreferenceGroup
import io.github.mangi.eta.ui.components.EtaPreferenceIcon
import io.github.mangi.eta.ui.components.LanguagePreference
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import io.github.mangi.eta.ui.navigation.AppRoute

/** 设置二级页：通用（外观 / 语音 / 语言 / 数据备份 / 数字助理应用）。 */
@Composable
internal fun SettingsGeneralScreen(
    context: Context,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val openAssistantSettings: () -> Unit = {
        val failed = runCatching {
            context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        }.isFailure
        if (failed) {
            Toast.makeText(context, context.getString(R.string.settings_open_assistant_failed), Toast.LENGTH_SHORT).show()
        }
    }

    MiuixScaffoldPage(
        title = stringResource(R.string.settings_general),
        onBack = onBack,
    ) {
        settingsTopSpacing()
        item(key = "general") {
            EtaPreferenceGroup {
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_title),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.Palette,
                            tint = EtaPreferenceColors.Orange,
                        )
                    },
                    onClick = { onNavigate(AppRoute.AppearanceSettings) },
                )

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.speech_settings_title),
                    startAction = { EtaPreferenceIcon(icon = Icons.Rounded.Mic, tint = EtaPreferenceColors.Blue) },
                    onClick = { onNavigate(AppRoute.SpeechSettings) },
                )

                EtaPreferenceDivider()
                LanguagePreference(iconTint = EtaPreferenceColors.Blue)

                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.data_backup_title),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.Description,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                    onClick = { onNavigate(AppRoute.DataBackup) },
                )

                EtaPreferenceDivider()
                // 数字助理应用：原「系统助手接管」组中无条件可用的系统设置入口，并入通用。
                EtaArrowPreference(
                    title = stringResource(R.string.ui_eta_system_assistant_003e9b),
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.SupportAgent,
                            tint = EtaPreferenceColors.Green,
                        )
                    },
                    onClick = openAssistantSettings,
                )
            }
        }
    }
}
