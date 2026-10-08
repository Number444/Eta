package io.github.mangi.eta.ui

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.libxposed.service.XposedService
import io.github.mangi.eta.EtaApp
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.accessibility.AccessibilityProtectionClient
import io.github.mangi.eta.agent.accessibility.AgentAccessibilityService
import io.github.mangi.eta.agent.runtime.AgentIslandNotifier
import io.github.mangi.eta.config.Prefs
import io.github.mangi.eta.data.repository.RuntimeConfigRepository
import io.github.mangi.eta.ui.app.EnhancementSettingsHistory
import io.github.mangi.eta.ui.components.EtaSwitchPreference
import io.github.mangi.eta.ui.components.EtaPreferenceIcon
import io.github.mangi.eta.ui.components.EtaWindowDialog
import io.github.mangi.eta.ui.components.MiuixDialogActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextField

// ── 设置页共享：状态持有器与开关/弹窗 helper（一级页与各二级页共用） ─────────────

/**
 * 无分组标题的设置页顶部留白：原有标题的页（如外观）标题块占约 32dp，裸卡片页会
 * 顶在大标题下方，补 12dp 让首张卡片与大标题底部的间距与其它页视觉平衡。
 */
internal fun LazyListScope.settingsTopSpacing() {
    item(key = "settings_top_spacing") {
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * LSPosed 框架配置状态：远程 [prefs]（service 到达时切换 RemotePreferences）+
 * 连接史/系统化使用史。一级页（接管入口显隐）与系统接管二级页共用。
 */
internal class FrameworkPrefsState internal constructor(
    prefsState: MutableState<SharedPreferences?>,
    connectedState: MutableState<Boolean>,
    usedSystemizerState: MutableState<Boolean>,
    val history: EnhancementSettingsHistory,
) {
    var prefs by prefsState
    var hasConnectedFramework by connectedState
    var hasUsedSystemizer by usedSystemizerState
}

@Composable
internal fun rememberFrameworkPrefsState(context: Context): FrameworkPrefsState {
    val history = remember(context.applicationContext) { EnhancementSettingsHistory(context) }
    val state = remember {
        FrameworkPrefsState(
            prefsState = mutableStateOf(Prefs.remotePreferencesForUi(EtaApp.serviceInstance)),
            connectedState = mutableStateOf(history.hasConnected),
            usedSystemizerState = mutableStateOf(history.hasUsedSystemizer),
            history = history,
        )
    }
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val listener = object : EtaApp.ServiceStateListener {
            override fun onServiceStateChanged(service: XposedService?) {
                state.prefs = Prefs.remotePreferencesForUi(service)
                state.prefs?.let { connected ->
                    history.captureConnected(connected)
                    state.hasConnectedFramework = true
                }
                Prefs.reconcileAgentPreferences(service)
                coroutineScope.launch {
                    RuntimeConfigRepository.ensureDefaults(service)
                }
            }
        }
        EtaApp.addServiceStateListener(listener, notifyImmediately = true)
        onDispose { EtaApp.removeServiceStateListener(listener) }
    }
    return state
}

/** 权限/通知状态：一级页聚合计数与权限二级页共用，ON_RESUME 自动刷新。 */
internal class PermissionSummaryState internal constructor(
    overlayState: MutableState<Boolean>,
    accessibilityState: MutableState<Boolean>,
    protectionEnabledState: MutableState<Boolean>,
    islandState: MutableState<AgentIslandNotifier.SupportStatus>,
) {
    var overlayGranted by overlayState
    var accessibilityGranted by accessibilityState
    var accessibilityProtectionEnabled by protectionEnabledState
    var islandStatus by islandState

    /** 悬浮窗 / 无障碍 / 超级岛三项中未授权的数量（岛不支持或检测中不计入）。 */
    val unauthorizedCount: Int
        get() = listOf(
            overlayGranted,
            accessibilityGranted || AgentAccessibilityService.isAvailable(),
            islandStatus != AgentIslandNotifier.SupportStatus.NO_PERMISSION,
        ).count { !it }
}

@Composable
internal fun rememberPermissionSummaryState(context: Context): PermissionSummaryState {
    val state = remember {
        PermissionSummaryState(
            overlayState = mutableStateOf(android.provider.Settings.canDrawOverlays(context)),
            accessibilityState = mutableStateOf(isAgentAccessibilityEnabled(context)),
            protectionEnabledState = mutableStateOf(AccessibilityProtectionClient.isEnabled(context)),
            islandState = mutableStateOf(AgentIslandNotifier.SupportStatus.CHECKING),
        )
    }
    // 岛权限查询为耗时调用，后台探测一次。
    LaunchedEffect(Unit) {
        state.islandStatus = withContext(Dispatchers.IO) {
            AgentIslandNotifier.querySupportStatus(context.applicationContext)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.overlayGranted = android.provider.Settings.canDrawOverlays(context)
                state.accessibilityGranted = isAgentAccessibilityEnabled(context)
                state.accessibilityProtectionEnabled = AccessibilityProtectionClient.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

internal fun isAgentAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(
        context,
        AgentAccessibilityService::class.java
    ).flattenToString()
    val enabledServices = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
}

/** 与供应商 key 同款的掩码显示：短 key 全掩，长 key 首尾各留 4 位。 */
internal fun maskSecretSummary(secret: String): String =
    if (secret.length <= 8) {
        "*".repeat(secret.length)
    } else {
        "${secret.take(4)}${"*".repeat(secret.length - 8)}${secret.takeLast(4)}"
    }

/** Exa API Key 输入弹窗：密码式输入 + 可见性切换，与供应商 key 输入同款。 */
@Composable
internal fun ExaApiKeyDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    var visible by remember { mutableStateOf(false) }
    EtaWindowDialog(
        show = true,
        title = "Exa API Key",
        summary = stringResource(R.string.settings_exa_api_key_summary),
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = value,
            onValueChange = { value = it },
            label = "API Key",
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        MiuixDialogActions(
            confirmText = stringResource(R.string.action_confirm),
            onCancel = onDismiss,
            onConfirm = { onSave(value) },
        )
    }
}

/**
 * 单个布尔开关：状态随 [prefs]/[key] 变化重读，切换时同步写入。
 *
 * 配置来源由调用方按能力边界传入。Hook 开关仍可能因 LSPosed 未连接而禁用；Agent
 * Runtime 开关始终使用 App 本地配置。
 */
@Composable
internal fun SwitchPref(
    context: Context,
    prefs: SharedPreferences?,
    title: String,
    summary: String? = null,
    key: String,
    icon: ImageVector,
    iconTint: Color,
) {
    val enabled = prefs != null
    val history = remember(context.applicationContext) { EnhancementSettingsHistory(context) }
    val default = Prefs.Keys.BOOLEAN_DEFAULTS[key] ?: true
    var checked by remember(prefs, key) {
        mutableStateOf(prefs?.getBoolean(key, default) ?: history.checked(key, default))
    }
    DisposableEffect(prefs, key) {
        val targetPrefs = prefs ?: return@DisposableEffect onDispose {}
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changedPrefs, changedKey ->
            if (changedKey == key) {
                checked = changedPrefs.getBoolean(key, default)
            }
        }
        targetPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { targetPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    EtaSwitchPreference(
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = { value ->
            // 同步提交；RemotePreferences.commit() 失败（binder 提交失败）时回滚 UI 状态，
            // 避免 UI 显示已切换而 hook 进程实际未收到。
            val targetPrefs = prefs ?: return@EtaSwitchPreference
            if (putBooleanSync(targetPrefs, key, value)) {
                checked = value
                history.recordCommittedBoolean(key, value)
                if (key in Prefs.Keys.LOCAL_AGENT_KEYS) {
                    Prefs.reconcileAgentPreferences(EtaApp.serviceInstance)
                }
            } else {
                Toast.makeText(
                    context.applicationContext,
                    context.getString(R.string.settings_write_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        startAction = {
            EtaPreferenceIcon(icon = icon, enabled = enabled, tint = iconTint)
        },
        enabled = enabled,
    )
}

/**
 * 同步写入布尔值。RemotePreferences 的 commit 先更新本进程 map 再同步等待 binder 提交，
 * 失败（binder RemoteException）返回 false 但本进程 map 已被改写——此时 hook 进程收不到新值。
 * 返回是否提交成功，供调用方决定是否更新 UI。
 */
internal fun putBooleanSync(
    prefs: SharedPreferences,
    key: String,
    value: Boolean
): Boolean =
    runCatching { prefs.edit().putBoolean(key, value).commit() }.getOrDefault(false)

internal fun putStringSync(
    prefs: SharedPreferences,
    key: String,
    value: String
): Boolean =
    runCatching { prefs.edit().putString(key, value).commit() }.getOrDefault(false)
