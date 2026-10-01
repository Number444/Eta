package io.github.mangi.eta.ui.pages.providers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.components.EtaPreference
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Eta Mod：「余额查询」折叠区（默认收起、输入框为空）。
 * 与 [providerHeadersEditor] 同范式：标题行 + 旋转箭头，展开后两个输入框。
 */
internal fun LazyListScope.providerBalanceEditor(
    balanceUrl: String,
    balanceJsonPath: String,
    onBalanceUrlChange: (String) -> Unit,
    onBalanceJsonPathChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    item(key = "balance_query") {
        ProviderSection(title = stringResource(R.string.provider_balance_section)) {
            val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f)
            EtaPreference(
                title = stringResource(
                    if (balanceUrl.isBlank()) {
                        R.string.provider_balance_state_unset
                    } else {
                        R.string.provider_balance_state_set
                    },
                ),
                summary = stringResource(R.string.provider_balance_summary),
                endActions = {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                },
                onClick = { onExpandedChange(!expanded) },
            )
            if (expanded) {
                EtaPreferenceDivider(hasLeading = false)
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextField(
                        value = balanceUrl,
                        onValueChange = onBalanceUrlChange,
                        label = stringResource(R.string.provider_balance_url_label),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = balanceJsonPath,
                        onValueChange = onBalanceJsonPathChange,
                        label = stringResource(R.string.provider_balance_path_label),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.provider_balance_hint),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}
