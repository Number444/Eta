@file:android.annotation.SuppressLint("LocalContextGetResourceValueCall")

package io.github.mangi.eta.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.mangi.eta.EtaApp
import io.github.mangi.eta.R
import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.model.AnthropicProviderSetting
import io.github.mangi.eta.data.model.CustomProviderSetting
import io.github.mangi.eta.data.model.Model
import io.github.mangi.eta.data.model.OpenAiEndpointMode
import io.github.mangi.eta.data.model.ProviderAuthModes
import io.github.mangi.eta.data.model.ProviderSetting
import io.github.mangi.eta.data.provider.BuiltinProviders
import io.github.mangi.eta.data.provider.OfficialModelCatalog
import io.github.mangi.eta.data.repository.ModelRepository
import io.github.mangi.eta.data.repository.ProviderRepository
import io.github.mangi.eta.data.repository.RemoteModelCatalog
import io.github.mangi.eta.data.repository.RemoteModelFetcher
import io.github.mangi.eta.data.repository.RuntimeConfigRepository
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaControlDefaults
import io.github.mangi.eta.ui.components.EtaGlassStyles
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import io.github.mangi.eta.ui.components.EtaPreferenceGroup
import io.github.mangi.eta.ui.components.EtaPreferenceGroupTitle
import io.github.mangi.eta.ui.components.EtaPreferenceIcon
import io.github.mangi.eta.ui.components.EtaTextButton
import io.github.mangi.eta.ui.components.LocalLiquidGlassBackdrop
import io.github.mangi.eta.ui.components.MiuixScaffold
import io.github.mangi.eta.ui.components.etaGlass
import io.github.mangi.eta.ui.components.etaPseudoGlass
import io.github.mangi.eta.ui.components.liquidGlassSource
import io.github.mangi.eta.ui.openAccessibilitySettings
import io.github.mangi.eta.ui.openNotificationSettings
import io.github.mangi.eta.ui.openOverlaySettings
import io.github.mangi.eta.ui.pages.providers.ProviderBrandIcon
import io.github.mangi.eta.ui.pages.providers.ProviderConfigDraft
import io.github.mangi.eta.ui.pages.providers.ProviderConfigDraftSaver
import io.github.mangi.eta.ui.pages.providers.ProviderConnectionFields
import io.github.mangi.eta.ui.pages.providers.ProviderIcon
import io.github.mangi.eta.ui.pages.providers.ProviderLoginState
import io.github.mangi.eta.ui.pages.providers.ProviderLoginStore
import io.github.mangi.eta.ui.pages.providers.openProviderAccountLink
import io.github.mangi.eta.ui.pages.providers.toProvider
import io.github.mangi.eta.ui.pages.providers.validateProviderDraft
import io.github.mangi.eta.ui.rememberPermissionSummaryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private enum class OnboardingStep { Provider, Connection, Model, Permissions }

@Composable
internal fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val providers by ProviderRepository.providersFlow().collectAsState(initial = emptyList())
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Provider) }
    var source by remember { mutableStateOf<ProviderSetting?>(null) }
    var isNew by rememberSaveable { mutableStateOf(false) }
    val newProviderId = rememberSaveable { ProviderRepository.newId() }
    var draft by rememberSaveable(stateSaver = ProviderConfigDraftSaver) {
        mutableStateOf(ProviderConfigDraft.from(BuiltinProviders.PROVIDERS.first()))
    }
    var catalog by remember { mutableStateOf<RemoteModelCatalog?>(null) }
    var models by remember { mutableStateOf<List<Model>>(emptyList()) }
    var modelsLoading by remember { mutableStateOf(false) }
    var modelSaving by remember { mutableStateOf(false) }
    var modelLoadCount by remember { mutableIntStateOf(0) }
    var selectedModelId by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loginCompleted by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var prefillAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RuntimeConfigRepository.ensureDefaults(EtaApp.serviceInstance)
    }
    LaunchedEffect(providers) {
        if (prefillAttempted || providers.isEmpty()) return@LaunchedEffect
        prefillAttempted = true
        val settings = SettingsDataStore.settings()
        val currentProvider = providers.firstOrNull { it.id == settings.selectedProviderId } ?: return@LaunchedEffect
        source = currentProvider
        draft = ProviderConfigDraft.from(currentProvider)
        selectedModelId = currentProvider.models.firstOrNull { it.id == settings.selectedModelId }?.modelId
    }

    fun finish() {
        if (finishing) return
        finishing = true
        scope.launch {
            try {
                SettingsDataStore.setOnboardingCompleted(true)
                onFinished()
            } finally {
                finishing = false
            }
        }
    }

    fun selectProvider(provider: ProviderSetting, new: Boolean) {
        val providerChanged = source?.id != provider.id || source?.let { it::class } != provider::class
        source = provider
        isNew = new
        draft = ProviderConfigDraft.from(provider)
        catalog = null
        models = emptyList()
        if (providerChanged) selectedModelId = null
        errorMessage = null
        loginCompleted = false
    }

    fun continueConnection() {
        val validation = validateProviderDraft(context, draft)
        errorMessage = when {
            validation != null -> context.getString(R.string.provider_error, validation)
            draft.authMode == ProviderAuthModes.API_KEY && draft.apiKey.isBlank() ->
                context.getString(R.string.onboarding_api_key_required)
            ProviderAuthModes.isOAuth(draft.authMode) && !loginCompleted ->
                context.getString(R.string.onboarding_login_required)
            else -> null
        }
        if (errorMessage != null) return
        step = OnboardingStep.Model
    }

    fun loadModels() {
        val currentBuilt = source?.let(draft::toProvider) ?: return
        scope.launch {
            modelsLoading = true
            errorMessage = null
            catalog = null
            val result = RemoteModelFetcher.fetchCatalog(currentBuilt)
            result.onSuccess {
                catalog = it
                models = it.models
            }.onFailure { throwable ->
                models = OfficialModelCatalog.modelsForProvider(currentBuilt).ifEmpty { currentBuilt.models }
                errorMessage = if (models.isEmpty()) {
                    context.getString(
                        R.string.provider_error,
                        throwable.message ?: throwable.javaClass.simpleName,
                    )
                } else {
                    context.getString(R.string.onboarding_models_fallback)
                }
            }
            modelsLoading = false
        }
    }

    LaunchedEffect(step, modelLoadCount) {
        if (step == OnboardingStep.Model) loadModels()
    }

    fun selectModel(model: Model) {
        if (modelsLoading || modelSaving) return
        val currentBuilt = source?.let(draft::toProvider) ?: return
        selectedModelId = model.modelId
        modelSaving = true
        scope.launch {
            try {
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    saveOnboardingModel(
                        provider = currentBuilt,
                        isNew = isNew,
                        catalog = catalog,
                        model = model,
                    )
                }
                modelSaving = false
                step = OnboardingStep.Permissions
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                errorMessage = context.getString(
                    R.string.provider_error,
                    throwable.message ?: throwable.javaClass.simpleName,
                )
            } finally {
                modelSaving = false
            }
        }
    }

    val permissionState = rememberPermissionSummaryState(context)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionState.notificationsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun permissionAction(kind: String) {
        when (kind) {
            "notifications" -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationSettings(context)
                }
            }
            "overlay" -> openOverlaySettings(context)
            "accessibility" -> openAccessibilitySettings(context)
        }
    }

    fun skipCurrentStep() {
        when (step) {
            OnboardingStep.Provider -> {
                if (source == null) {
                    val fallback = providers.firstOrNull() ?: BuiltinProviders.PROVIDERS.first()
                    source = fallback
                    isNew = false
                    draft = ProviderConfigDraft.from(fallback)
                }
                step = OnboardingStep.Connection
            }
            OnboardingStep.Connection -> step = OnboardingStep.Model
            OnboardingStep.Model -> step = OnboardingStep.Permissions
            OnboardingStep.Permissions -> finish()
        }
    }

    MiuixScaffold(
        title = "",
        onBack = {
            when (step) {
                OnboardingStep.Provider -> finish()
                OnboardingStep.Connection -> step = OnboardingStep.Provider
                OnboardingStep.Model -> step = OnboardingStep.Connection
                OnboardingStep.Permissions -> step = OnboardingStep.Model
            }
        },
        showNavigationIcon = false,
        captureGlassContent = false,
        topBarContent = {},
    ) { paddingValues, _, sidePadding ->
        val glassBackdrop = LocalLiquidGlassBackdrop.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            Box(modifier = Modifier.fillMaxSize().liquidGlassSource(glassBackdrop)) {
                Crossfade(
                    targetState = step,
                    animationSpec = tween(180),
                    modifier = Modifier.fillMaxSize(),
                ) { currentStep ->
                    when (currentStep) {
                        OnboardingStep.Provider -> OnboardingProviderPage(
                            providers = providers,
                            selected = source,
                            newProviderSelected = isNew,
                            step = currentStep,
                            sidePadding = sidePadding,
                            onSelect = ::selectProvider,
                            onCustom = { type ->
                                val custom = when (type) {
                                    OnboardingCustomType.OpenAi -> CustomProviderSetting(
                                        id = newProviderId,
                                        name = "",
                                        baseUrl = "",
                                        endpointMode = OpenAiEndpointMode.CHAT_COMPLETIONS,
                                    )
                                    OnboardingCustomType.Anthropic -> AnthropicProviderSetting(
                                        id = newProviderId,
                                        name = "",
                                        baseUrl = "https://api.anthropic.com",
                                    )
                                }
                                selectProvider(custom, new = true)
                            },
                        )
                        OnboardingStep.Connection -> if (source != null) {
                            OnboardingConnectionPage(
                                provider = source!!,
                                draft = draft,
                                onDraftChange = { draft = it },
                                step = currentStep,
                                sidePadding = sidePadding,
                                errorMessage = errorMessage,
                                onLoginValidationError = { errorMessage = it },
                                onLoginStateChange = { loginCompleted = it.account != null },
                            )
                        }
                        OnboardingStep.Model -> OnboardingModelPage(
                            models = models,
                            loading = modelsLoading,
                            selectedModelId = selectedModelId,
                            errorMessage = errorMessage,
                            step = currentStep,
                            sidePadding = sidePadding,
                            onRetry = { modelLoadCount++ },
                            onSelect = ::selectModel,
                        )
                        OnboardingStep.Permissions -> OnboardingPermissionsPage(
                            overlayGranted = permissionState.overlayGranted,
                            accessibilityGranted = permissionState.accessibilityGranted,
                            notificationsGranted = permissionState.notificationsGranted,
                            step = currentStep,
                            sidePadding = sidePadding,
                            onAction = ::permissionAction,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = sidePadding + 32.dp)
                    .padding(bottom = 32.dp),
            ) {
                val primaryActionEnabled = when (step) {
                    OnboardingStep.Provider -> source != null
                    OnboardingStep.Connection -> !modelsLoading
                    OnboardingStep.Permissions -> !finishing
                    OnboardingStep.Model -> selectedModelId != null && !modelsLoading && !modelSaving
                }
                OnboardingFloatingButton(
                    text = stringResource(
                        if (step == OnboardingStep.Permissions) R.string.onboarding_finish else R.string.onboarding_next,
                    ),
                    onClick = {
                        when (step) {
                            OnboardingStep.Provider -> if (source != null) step = OnboardingStep.Connection
                            OnboardingStep.Connection -> continueConnection()
                            OnboardingStep.Permissions -> finish()
                            OnboardingStep.Model -> models.firstOrNull { it.modelId == selectedModelId }?.let(::selectModel)
                        }
                    },
                    enabled = primaryActionEnabled,
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OnboardingFloatingButton(
                        text = stringResource(R.string.action_back),
                        onClick = {
                            step = when (step) {
                                OnboardingStep.Connection -> OnboardingStep.Provider
                                OnboardingStep.Model -> OnboardingStep.Connection
                                OnboardingStep.Permissions -> OnboardingStep.Model
                                OnboardingStep.Provider -> step
                            }
                        },
                        enabled = step != OnboardingStep.Provider && !finishing,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OnboardingFloatingButton(
                        text = stringResource(R.string.onboarding_skip),
                        onClick = ::skipCurrentStep,
                        enabled = !finishing,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

internal suspend fun saveOnboardingModel(
    provider: ProviderSetting,
    isNew: Boolean,
    catalog: RemoteModelCatalog?,
    model: Model,
) {
    if (isNew) {
        ProviderRepository.addProvider(provider)
    } else {
        ProviderRepository.updateProvider(provider)
    }
    catalog?.let { ModelRepository.syncRemoteCatalog(it, setOf(model.modelId)) }
    val savedProvider = requireNotNull(ProviderRepository.providerById(provider.id))
    val savedModel = requireNotNull(savedProvider.models.firstOrNull { it.modelId == model.modelId })
    RuntimeConfigRepository.setSelectedModelId(savedModel.id)
    RuntimeConfigRepository.syncToRemotePreferences(EtaApp.serviceInstance)
}

private enum class OnboardingCustomType { OpenAi, Anthropic }

@Composable
private fun OnboardingFloatingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val backdrop = LocalLiquidGlassBackdrop.current
    if (backdrop == null) {
        EtaTextButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.onboardingContentGlass(primary = primary, enabled = enabled),
            colors = if (primary) ButtonDefaults.textButtonColorsPrimary() else ButtonDefaults.textButtonColors(),
        )
        return
    }
    val style = EtaGlassStyles.InputBar.copy(shape = CircleShape)
    Box(
        modifier = modifier
            .heightIn(min = EtaControlDefaults.ButtonMinHeight)
            .etaGlass(
                style = style,
                backdrop = backdrop,
                tint = if (primary && enabled) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.12f)
                },
            )
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(EtaControlDefaults.ButtonPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            color = when {
                !enabled -> MiuixTheme.colorScheme.disabledOnSurface
                primary -> MiuixTheme.colorScheme.onPrimary
                else -> MiuixTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun Modifier.onboardingContentGlass(
    primary: Boolean = false,
    enabled: Boolean = true,
): Modifier = etaPseudoGlass(
    style = EtaGlassStyles.InputBar.copy(shape = CircleShape),
    pseudoBackground = if (primary) {
        if (enabled) {
            MiuixTheme.colorScheme.primary.copy(alpha = 0.85f)
        } else {
            MiuixTheme.colorScheme.surface.copy(alpha = 0.45f)
        }
    } else {
        null
    },
)

@Composable
private fun OnboardingStepHeader(step: OnboardingStep) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 18.dp)
                .offset(y = (-8).dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MiuixTheme.textStyles.title2.copy(
                    fontSize = 33.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(12.dp))
            CircularProgressIndicator(
                progress = (step.ordinal + 1).toFloat() / OnboardingStep.entries.size,
                size = 24.dp,
                strokeWidth = 2.5.dp,
                modifier = Modifier.offset(y = with(LocalDensity.current) { 2.toDp() }),
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = MiuixTheme.colorScheme.primary,
                    disabledForegroundColor = MiuixTheme.colorScheme.primary,
                    backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                ),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (step) {
                    OnboardingStep.Provider -> R.string.onboarding_step_provider
                    OnboardingStep.Connection -> R.string.onboarding_step_connection
                    OnboardingStep.Model -> R.string.onboarding_step_model
                    OnboardingStep.Permissions -> R.string.onboarding_step_permissions
                },
            ),
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 32.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun OnboardingProviderPage(
    providers: List<ProviderSetting>,
    selected: ProviderSetting?,
    newProviderSelected: Boolean,
    step: OnboardingStep,
    sidePadding: Dp,
    onSelect: (ProviderSetting, Boolean) -> Unit,
    onCustom: (OnboardingCustomType) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.onboardingScroll(),
        contentPadding = onboardingContentPadding(sidePadding),
        verticalArrangement = Arrangement.Center,
    ) {
        item { OnboardingStepHeader(step) }
        item {
            Text(
                text = stringResource(R.string.onboarding_provider_summary),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 8.dp),
            )
        }
        item {
            EtaPreferenceGroup {
                providers.forEachIndexed { index, provider ->
                    if (index > 0) EtaPreferenceDivider()
                    EtaArrowPreference(
                        title = provider.name,
                        summary = provider.baseUrl,
                        startAction = { ProviderIcon(provider) },
                        endActions = {
                            if (selected?.id == provider.id) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MiuixTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = { onSelect(provider, false) },
                    )
                }
            }
        }
        item {
            EtaPreferenceGroupTitle(stringResource(R.string.ui_add_new_provider_74df54))
            EtaPreferenceGroup {
                EtaArrowPreference(
                    title = stringResource(R.string.ui_added_openai_compatible_6bd471),
                    startAction = { EtaPreferenceIcon(Icons.Rounded.Storefront) },
                    endActions = {
                        if (newProviderSelected && selected is CustomProviderSetting) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = { onCustom(OnboardingCustomType.OpenAi) },
                )
                EtaPreferenceDivider()
                EtaArrowPreference(
                    title = stringResource(R.string.ui_new_anthropic_db6098),
                    startAction = { ProviderBrandIcon(io.github.mangi.eta.data.model.ProviderSourceTypes.ANTHROPIC) },
                    endActions = {
                        if (newProviderSelected && selected is AnthropicProviderSetting) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = { onCustom(OnboardingCustomType.Anthropic) },
                )
            }
        }
    }
}

@Composable
private fun OnboardingConnectionPage(
    provider: ProviderSetting,
    draft: ProviderConfigDraft,
    onDraftChange: (ProviderConfigDraft) -> Unit,
    step: OnboardingStep,
    sidePadding: Dp,
    errorMessage: String?,
    onLoginValidationError: (String) -> Unit,
    onLoginStateChange: (ProviderLoginState) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loginStore = remember(provider.id) { ProviderLoginStore(provider.id, scope) }
    val loginState = loginStore.state.collectAsState().value
    var browserError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(loginStore) {
        onDispose { loginStore.close() }
    }
    LaunchedEffect(loginStore, draft.authMode) {
        loginStore.load(draft.authMode)
    }
    LaunchedEffect(loginState.account, draft.authMode) {
        onLoginStateChange(loginState)
    }
    LaunchedEffect(loginState.prompt) {
        val prompt = loginState.prompt
        if (prompt != null && prompt.userCode == null) {
            browserError = openProviderAccountLink(context, prompt.authorizationUrl)
        }
    }
    LazyColumn(
        modifier = Modifier.onboardingScroll(),
        contentPadding = onboardingContentPadding(sidePadding),
        verticalArrangement = Arrangement.Center,
    ) {
        item { OnboardingStepHeader(step) }
        item {
            EtaPreferenceGroup {
                ProviderConnectionFields(
                    provider = provider,
                    draft = draft,
                    onDraftChange = onDraftChange,
                    loginState = loginState,
                    enabled = !loginState.isBusy,
                    browserError = browserError,
                    onOpenBrowser = {
                        loginState.prompt?.let {
                            browserError = openProviderAccountLink(context, it.authorizationUrl)
                        }
                    },
                    onManageUsage = {
                        browserError = openProviderAccountLink(context, "https://chatgpt.com/codex/settings/usage")
                    },
                    onLogin = {
                        val validation = validateProviderDraft(context, draft)
                        if (validation != null) {
                            onLoginValidationError(context.getString(R.string.provider_error, validation))
                        } else {
                            loginStore.login(draft.toProvider(provider))
                        }
                    },
                    onDisconnect = loginStore::disconnect,
                    onCancel = loginStore::cancel,
                )
            }
        }
        if (errorMessage != null || loginState.message != null) {
            item {
                Text(
                    text = errorMessage ?: loginState.message.orEmpty(),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun OnboardingModelPage(
    models: List<Model>,
    loading: Boolean,
    selectedModelId: String?,
    errorMessage: String?,
    step: OnboardingStep,
    sidePadding: Dp,
    onRetry: () -> Unit,
    onSelect: (Model) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.onboardingScroll(),
        contentPadding = onboardingContentPadding(sidePadding),
        verticalArrangement = Arrangement.Center,
    ) {
        item { OnboardingStepHeader(step) }
        if (loading) {
            item {
                Text(
                    text = stringResource(R.string.onboarding_models_loading),
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                )
            }
        }
        if (!loading && models.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.onboarding_models_empty),
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
        if (models.isNotEmpty()) {
            item {
                EtaPreferenceGroup {
                    models.forEachIndexed { index, model ->
                        if (index > 0) EtaPreferenceDivider()
                        EtaArrowPreference(
                            title = model.displayName,
                            summary = model.modelId,
                            enabled = !loading,
                            endActions = {
                                if (selectedModelId == model.modelId) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MiuixTheme.colorScheme.primary,
                                    )
                                }
                            },
                            onClick = { onSelect(model) },
                        )
                    }
                }
            }
        }
        if (!loading) {
            item {
                EtaTextButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).onboardingContentGlass(),
                )
            }
        }
    }
}

@Composable
private fun OnboardingPermissionsPage(
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    notificationsGranted: Boolean,
    step: OnboardingStep,
    sidePadding: Dp,
    onAction: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.onboardingScroll(),
        contentPadding = onboardingContentPadding(sidePadding),
        verticalArrangement = Arrangement.Center,
    ) {
        item { OnboardingStepHeader(step) }
        item {
            Text(
                text = stringResource(R.string.onboarding_permissions_summary),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 8.dp),
            )
        }
        item {
            EtaPreferenceGroup {
                OnboardingPermissionRow(
                    title = stringResource(R.string.capability_notifications_title),
                    granted = notificationsGranted,
                    icon = Icons.Rounded.Notifications,
                    onClick = { onAction("notifications") },
                )
                EtaPreferenceDivider()
                OnboardingPermissionRow(
                    title = stringResource(R.string.ui_floating_window_permissions_076b77),
                    granted = overlayGranted,
                    icon = Icons.Rounded.Layers,
                    onClick = { onAction("overlay") },
                )
                EtaPreferenceDivider()
                OnboardingPermissionRow(
                    title = stringResource(R.string.ui_accessibility_enhancement_tools_8fd257),
                    granted = accessibilityGranted,
                    icon = Icons.Rounded.AccessibilityNew,
                    onClick = { onAction("accessibility") },
                )
            }
        }
    }
}

@Composable
private fun OnboardingPermissionRow(
    title: String,
    granted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    EtaArrowPreference(
        title = title,
        startAction = { EtaPreferenceIcon(icon) },
        endActions = {
            Text(
                text = stringResource(if (granted) R.string.status_authorized else R.string.status_unauthorized),
                style = MiuixTheme.textStyles.body2,
                color = if (granted) MiuixTheme.colorScheme.onSurfaceVariantActions else MiuixTheme.colorScheme.error,
            )
        },
        onClick = onClick,
    )
}

private fun Modifier.onboardingScroll(): Modifier =
    fillMaxSize()
        .scrollEndHaptic()
        .overScrollVertical()

private fun onboardingContentPadding(sidePadding: Dp) = PaddingValues(
    start = sidePadding,
    top = 12.dp,
    end = sidePadding,
    bottom = 156.dp,
)
