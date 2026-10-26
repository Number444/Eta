package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w420dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EtaGlassBottomSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun glassSheetKeepsSingleNativeTitleAndRendersOneHandle() {
        composeRule.setContent {
            MiuixTheme(colors = lightColorScheme()) {
                val backdrop = rememberLayerBackdrop {
                    drawRect(Color(0xFFE6E6EA))
                    drawContent()
                }
                CompositionLocalProvider(
                    LocalLiquidGlassBackdrop provides backdrop,
                    LocalSquircleEnabled provides false,
                ) {
                    Scaffold {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xFFD7D7DE))
                                .liquidGlassSource(backdrop),
                        )
                        EtaGlassBottomSheet(
                            show = true,
                            title = "添加模型",
                            onDismissRequest = {},
                            insideMargin = DpSize(0.dp, 0.dp),
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .padding(24.dp),
                            ) {
                                Text("远端有 4 个尚未添加的模型")
                            }
                        }
                    }
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onAllNodesWithText("添加模型").assertCountEquals(1)
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val output = File("build/outputs/bottom-sheet/EtaGlassBottomSheetTest.png")
        output.parentFile?.mkdirs()
        FileOutputStream(output).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
