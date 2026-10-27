package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.layout.WidePageContent
import io.github.mangi.eta.ui.layout.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * Eta 二级列表页的统一骨架：手机折叠大标题、宽屏固定小标题、顶栏毛玻璃、横屏安全区、
 * 宽屏内容居中、滚动边界触感与越界回弹均由此处统一提供。
 */
@Composable
fun MiuixScaffoldPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    compactTitle: Boolean = false,
    content: LazyListScope.() -> Unit,
) {
    EtaPreferenceTheme {
        val scrollBehavior = MiuixScrollBehavior()
        val backdrop = rememberTopBarBackdrop()
        val topBarColor = topBarContainerColor(backdrop)
        // Eta Mod：设置骨架级玻璃采样源（焕新架构改动三）——31 个二级页一处注入，
        // 采样内容=滚动列表，消费者（顶栏控件等固定元素）在列表之外，不触嵌套禁令。
        // 阶段四：浅色液态玻璃档页面底色灰白化（etaGlassPageBaseColor），
        // 采样纹理基色同步，否则真玻璃返回钮采到的底与可见底不一致。
        val pageBaseColor = etaGlassPageBaseColor(MiuixTheme.colorScheme.surface)
        val glassBackdrop = rememberLiquidGlassBackdrop(baseColor = pageBaseColor)

        CompositionLocalProvider(LocalLiquidGlassBackdrop provides glassBackdrop) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = pageBaseColor,
            topBar = {
                TopBarBackdrop(backdrop) {
                    // 页面自带主视觉（如角色详情）时用紧凑标题，避免大标题与主视觉重复。
                    if (compactTitle) {
                        SmallTopAppBar(
                            title = title,
                            color = topBarColor,
                            navigationIcon = { MiuixBackButton(onClick = onBack) },
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    } else {
                        AdaptiveTopAppBar(
                            title = title,
                            color = topBarColor,
                            navigationIcon = { MiuixBackButton(onClick = onBack) },
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    }
                }
            },
        ) { innerPadding ->
            WidePageContent { sidePadding ->
                // 保留 MiuixTheme 注入的默认越界工厂，让短内容页也能回弹到顶栏采样区。
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalCutoutPadding()
                        .captureForTopBar(backdrop)
                        .liquidGlassSource(glassBackdrop)
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = sidePadding,
                        top = innerPadding.calculateTopPadding(),
                        end = sidePadding,
                    ),
                ) {
                    content()
                    item(key = "bottom_spacer") {
                        MiuixPageBottomSpacer()
                    }
                }
            }
        }
        }
    }
}

/**
 * 自定义内容二级页的低层骨架。调用方负责把顶部 padding、横向安全区与 nested scroll
 * 接入自己的内容；[sidePadding] 用于在宽屏限制实际内容宽度，滚动容器本身仍应保持全宽。
 */
@Composable
fun MiuixScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showNavigationIcon: Boolean = true,
    compactTitle: Boolean = false,
    captureGlassContent: Boolean = true,
    topBarContent: (@Composable (ScrollBehavior) -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (
        paddingValues: PaddingValues,
        scrollBehavior: ScrollBehavior,
        sidePadding: Dp,
    ) -> Unit,
) {
    EtaPreferenceTheme {
        val scrollBehavior = MiuixScrollBehavior()
        val backdrop = rememberTopBarBackdrop()
        val topBarColor = topBarContainerColor(backdrop)
        // Eta Mod：设置骨架级玻璃采样源（焕新架构改动三），同 MiuixScaffoldPage。
        val pageBaseColor = etaGlassPageBaseColor(MiuixTheme.colorScheme.surface)
        val glassBackdrop = rememberLiquidGlassBackdrop(baseColor = pageBaseColor)

        CompositionLocalProvider(LocalLiquidGlassBackdrop provides glassBackdrop) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = pageBaseColor,
            topBar = {
                TopBarBackdrop(backdrop) {
                    if (topBarContent != null) {
                        topBarContent(scrollBehavior)
                    } else if (compactTitle) {
                        SmallTopAppBar(
                            title = title,
                            color = topBarColor,
                            navigationIcon = { if (showNavigationIcon) MiuixBackButton(onClick = onBack) },
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    } else {
                        AdaptiveTopAppBar(
                            title = title,
                            color = topBarColor,
                            navigationIcon = { if (showNavigationIcon) MiuixBackButton(onClick = onBack) },
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    }
                }
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .captureForTopBar(backdrop)
                    .then(
                        if (captureGlassContent) Modifier.liquidGlassSource(glassBackdrop) else Modifier,
                    ),
            ) {
                WidePageContent { sidePadding ->
                    content(paddingValues, scrollBehavior, sidePadding)
                }
            }
        }
        }
    }
}

@Composable
fun MiuixPageBottomSpacer(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .height(24.dp)
            .navigationBarsPadding(),
    )
}
