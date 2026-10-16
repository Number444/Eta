package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 二级页面统一返回按钮，保持图标、语义、RTL 方向与点击区域一致。
 *
 * Eta Mod（焕新阶段四）：液态玻璃档下套真玻璃（与主页顶栏按钮同款 44dp 圆 +
 * FloatingButton 预设），设置骨架顶栏在录制层外，可采样背后的滚动列表。
 * 无采样源的页面（壳内非主页路由，如 Tools/Skills）须传 [enableGlass] = false，
 * 否则玻璃会采到主页残留帧。
 */
@Composable
fun MiuixBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enableGlass: Boolean = true,
) {
    val glassBackdrop = if (enableGlass) LocalLiquidGlassBackdrop.current else null
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .liquidGlassCircle(
                glassBackdrop,
                tint = MiuixTheme.colorScheme.surface.copy(alpha = EtaGlassStyles.FloatingButton.tintAlpha),
            ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
        )
    }
}
