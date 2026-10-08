package io.github.mangi.eta.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.mangi.eta.R
import io.github.mangi.eta.config.Prefs
import io.github.mangi.eta.data.model.ReasoningEffort
import io.github.mangi.eta.data.repository.ProviderRepository
import io.github.mangi.eta.data.repository.RuntimeConfigRepository
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaDropdownPreference
import io.github.mangi.eta.ui.components.EtaPreferenceColors
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import io.github.mangi.eta.ui.components.EtaPreferenceGroup
import io.github.mangi.eta.ui.components.EtaPreferenceIcon
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import io.github.mangi.eta.ui.navigation.AppRoute
import top.yukonga.miuix.kmp.basic.DropdownItem

/** 设置二级页：模型与对话（提供商入口 / 思考 / 上下文压缩）。 */
@Composable
internal fun SettingsModelScreen(
    context: Context,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val agentPrefs = remember { Prefs.localAgentPreferences() }

    // Provider / Model 选中状态展示
    val providers by ProviderRepository.providersFlow().collectAsState(initial = emptyList())
    val selectedProviderId by RuntimeConfigRepository.selectedProviderIdFlow()
        .collectAsState(initial = null)
    val selectedModelId by RuntimeConfigRepository.selectedModelIdFlow()
        .collectAsState(initial = null)
    val selectedProvider = remember(providers, selectedProviderId) {
        providers.find { it.id == selectedProviderId }
    }
    val selectedModel = remember(selectedProvider, selectedModelId) {
        selectedProvider?.models?.find { it.id == selectedModelId }
    }
    val providerSummary = selectedProvider?.let { provider ->
        "${provider.name} / ${selectedModel?.displayName ?: stringResource(R.string.settings_model_not_selected)}"
    } ?: stringResource(R.string.settings_not_configured)

    // Eta Mod：默认思考深度下拉
    val defaultReasoningEffortOptions = remember {
        listOf(
            ReasoningEffort.DEFAULT,
            ReasoningEffort.OFF,
            ReasoningEffort.LOW,
            ReasoningEffort.MEDIUM,
            ReasoningEffort.HIGH,
            ReasoningEffort.XHIGH,
            ReasoningEffort.MAX,
        )
    }
    val followModelEffortLabel = stringResource(R.string.settings_reasoning_effort_follow_model)
    val defaultReasoningEffortItems = remember(defaultReasoningEffortOptions, followModelEffortLabel) {
        defaultReasoningEffortOptions.map { effort ->
            DropdownItem(
                text = if (effort == ReasoningEffort.DEFAULT) followModelEffortLabel else effort.displayName,
            )
        }
    }
    var defaultReasoningEffort by remember {
        mutableStateOf(
            ReasoningEffort.fromWireValue(
                agentPrefs?.getString(Prefs.Keys.AGENT_DEFAULT_REASONING_EFFORT, null),
            ) ?: ReasoningEffort.DEFAULT,
        )
    }
    // Eta Mod：上下文压缩模型（两级选择：服务商 → 模型）
    var compactProviderId by remember {
        mutableStateOf(agentPrefs?.getString(Prefs.Keys.AGENT_COMPACT_PROVIDER_ID, "").orEmpty())
    }
    var compactModelId by remember {
        mutableStateOf(agentPrefs?.getString(Prefs.Keys.AGENT_COMPACT_MODEL_ID, "").orEmpty())
    }

    MiuixScaffoldPage(
        title = stringResource(R.string.settings_category_model_conversation),
        onBack = onBack,
    ) {
        settingsTopSpacing()
        item(key = "model_conversation") {
            EtaPreferenceGroup {
                EtaArrowPreference(
                    title = stringResource(R.string.ui_model_provider_e8c7f5),
                    summary = providerSummary,
                    startAction = {
                        EtaPreferenceIcon(
                            icon = Icons.Rounded.Cloud,
                            tint = EtaPreferenceColors.Blue,
                        )
                    },
                    onClick = { onNavigate(AppRoute.ModelProviders) },
                )

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.ui_deep_thinking_enabled_by_default_c032d6),
                    key = Prefs.Keys.AGENT_THINKING_ENABLED,
                    icon = Icons.Rounded.Psychology,
                    iconTint = EtaPreferenceColors.Blue,
                )

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.settings_auto_expand_thinking),
                    summary = stringResource(R.string.settings_auto_expand_thinking_summary),
                    key = Prefs.Keys.AGENT_AUTO_EXPAND_THINKING,
                    icon = Icons.Rounded.UnfoldMore,
                    iconTint = EtaPreferenceColors.Blue,
                )

                EtaPreferenceDivider()
                EtaDropdownPreference(
                    title = stringResource(R.string.settings_default_reasoning_effort),
                    summary = stringResource(R.string.settings_default_reasoning_effort_summary),
                    items = defaultReasoningEffortItems,
                    selectedIndex = defaultReasoningEffortOptions.indexOf(defaultReasoningEffort),
                    onSelectedIndexChange = { index ->
                        val effort = defaultReasoningEffortOptions.getOrNull(index)
                            ?: return@EtaDropdownPreference
                        val targetPrefs = agentPrefs ?: return@EtaDropdownPreference
                        if (putStringSync(targetPrefs, Prefs.Keys.AGENT_DEFAULT_REASONING_EFFORT, effort.wireValue)) {
                            defaultReasoningEffort = effort
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
                            icon = Icons.Rounded.AccountTree,
                            tint = EtaPreferenceColors.Blue,
                            enabled = agentPrefs != null,
                        )
                    },
                    enabled = agentPrefs != null,
                )

                EtaPreferenceDivider()
                SwitchPref(
                    context = context,
                    prefs = agentPrefs,
                    title = stringResource(R.string.settings_auto_compaction),
                    key = Prefs.Keys.AGENT_AUTO_COMPACTION_ENABLED,
                    icon = Icons.Rounded.Layers,
                    iconTint = EtaPreferenceColors.Blue,
                )

                // Eta Mod：上下文压缩模型（服务商 → 模型两级选择）
                val compactProviders = providers.filter { it.isEnabled }
                EtaPreferenceDivider()
                EtaDropdownPreference(
                    title = stringResource(R.string.settings_compact_provider),
                    summary = stringResource(R.string.settings_compact_model_summary),
                    items = listOf(DropdownItem(text = stringResource(R.string.settings_compact_follow_main))) +
                        compactProviders.map { DropdownItem(text = it.name) },
                    selectedIndex = compactProviders.indexOfFirst { it.id == compactProviderId }
                        .let { if (it < 0) 0 else it + 1 },
                    onSelectedIndexChange = { index ->
                        val targetPrefs = agentPrefs ?: return@EtaDropdownPreference
                        val provider = compactProviders.getOrNull(index - 1)
                        val newId = provider?.id.orEmpty()
                        if (putStringSync(targetPrefs, Prefs.Keys.AGENT_COMPACT_PROVIDER_ID, newId) &&
                            putStringSync(targetPrefs, Prefs.Keys.AGENT_COMPACT_MODEL_ID, "")
                        ) {
                            compactProviderId = newId
                            compactModelId = ""
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
                            icon = Icons.Rounded.Memory,
                            tint = EtaPreferenceColors.Blue,
                            enabled = agentPrefs != null,
                        )
                    },
                    enabled = agentPrefs != null,
                )
                val compactProvider = compactProviders.firstOrNull { it.id == compactProviderId }
                if (compactProvider != null) {
                    val compactModels = compactProvider.models.filter { it.isEnabled }
                    EtaPreferenceDivider()
                    EtaDropdownPreference(
                        title = stringResource(R.string.settings_compact_model),
                        items = compactModels.map {
                            DropdownItem(text = it.displayName.ifBlank { it.modelId })
                        },
                        selectedIndex = compactModels.indexOfFirst { it.id == compactModelId }
                            .coerceAtLeast(0),
                        onSelectedIndexChange = { index ->
                            val model = compactModels.getOrNull(index)
                                ?: return@EtaDropdownPreference
                            val targetPrefs = agentPrefs ?: return@EtaDropdownPreference
                            if (putStringSync(targetPrefs, Prefs.Keys.AGENT_COMPACT_MODEL_ID, model.id)) {
                                compactModelId = model.id
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
                                icon = Icons.Rounded.Memory,
                                tint = EtaPreferenceColors.Blue,
                                enabled = agentPrefs != null,
                            )
                        },
                        enabled = agentPrefs != null,
                    )
                }
            }
        }
    }
}
