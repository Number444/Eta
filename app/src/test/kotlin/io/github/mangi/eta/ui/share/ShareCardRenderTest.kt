package io.github.mangi.eta.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.ui.app.AgentAppTheme
import io.github.mangi.eta.ui.markdown.LocalMarkdownStaticExport
import io.github.mangi.eta.ui.markdown.MarkdownInlineStyle
import io.github.mangi.eta.ui.markdown.MarkdownTone
import io.github.mangi.eta.ui.markdown.rememberMarkdownStyle
import io.github.mangi.eta.ui.model.AgentMessageUi
import io.github.mangi.eta.ui.model.ToolActivityStatusUi
import io.github.mangi.eta.ui.model.UserMessageUi
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h2000dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareCardRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shareCardRendersPhoneTabletAndDesktopWidths() {
        val turns = listOf(
            ShareTurn(
                userPrompt = "测试网络搜索搜索最新新闻",
                segments = listOf(
                    ShareTurnSegment.ToolCall(
                        toolName = "web_search",
                        summary = "最新新闻",
                        status = ToolActivityStatusUi.Failed,
                        failureLine = "网页搜索失败",
                    ),
                    ShareTurnSegment.ToolCall(
                        toolName = "web_search",
                        summary = "今日热点",
                        status = ToolActivityStatusUi.Failed,
                        failureLine = "网页搜索失败",
                    ),
                    ShareTurnSegment.ToolCall(
                        toolName = "device_info",
                        summary = "查看网络信息",
                        status = ToolActivityStatusUi.Success,
                        failureLine = null,
                    ),
                    ShareTurnSegment.ToolCall(
                        toolName = "web_search",
                        summary = "科技新闻",
                        status = ToolActivityStatusUi.Failed,
                        failureLine = "网页搜索失败",
                    ),
                    ShareTurnSegment.Markdown(
                        """
                        测试结果：**web_search 连续三次超时**（WEB_TIMEOUT），但手机本机网络正常（Wi-Fi 已连接且验证通过）。

                        也就是说：设备联网没问题，是搜索服务这一环连不上。

                        - 要点一：设备联网正常
                        - 要点二：搜索服务连不上

                        ```kotlin
                        val timeout = check("web_search")
                        ```

                        | 项目 | 状态 |
                        |---|---|
                        | 本机网络 | 正常 |
                        | 搜索服务 | 超时 |

                        https://example.com/a/very/long/path/used/to/check/wrapping/behavior/for/share/images
                        """.trimIndent(),
                    ),
                ),
            ),
        )
        val documents = MessageShareImage.parseAllSegments(turns, MarkdownInlineStyle.Default)
        assertTrue("markdown 段应解析成功", documents.isNotEmpty())
        val format = mutableStateOf(ShareImageFormat.Phone)

        composeRule.setContent {
            AgentAppTheme(appearance = AppearanceSettings(), applyInterfaceScale = false) {
                val style = rememberMarkdownStyle(MarkdownTone.Answer)
                CompositionLocalProvider(
                    LocalMarkdownStaticExport provides true,
                    LocalSquircleEnabled provides false,
                ) {
                    Box(modifier = Modifier.width(format.value.widthDp).testTag(ShareCardTag)) {
                        ShareTurnsCard(
                            turns = turns,
                            documents = documents,
                            style = style,
                            brand = "Eta Mod",
                        )
                    }
                }
            }
        }

        val density = RuntimeEnvironment.getApplication().resources.displayMetrics.density
        ShareImageFormat.entries.forEach { imageFormat ->
            format.value = imageFormat
            composeRule.waitForIdle()
            val bitmap = composeRule.onNodeWithTag(ShareCardTag).captureToImage().asAndroidBitmap()
            val expectedWidth = (imageFormat.widthDp.value * density + 0.5f).toInt()
            assertEquals("${imageFormat.name} 位图宽度", expectedWidth, bitmap.width)
            assertTrue("${imageFormat.name} 位图高度", bitmap.height > 0)

            val outDir = File("build/outputs/share-card").apply { mkdirs() }
            val outFile = File(outDir, "ShareCardTest-${imageFormat.name}.png")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            println("分享卡片渲染图: ${outFile.absolutePath} (${bitmap.width}x${bitmap.height})")
            assertTrue("PNG 应已落盘", outFile.length() > 0)
        }
    }

    @Test
    fun shareTurnKeepsRunModelSnapshotAndSanitizesFooter() {
        val messages = listOf(
            UserMessageUi(id = "user-1", content = "你好"),
            AgentMessageUi(id = "assistant-1", content = "第一段", modelApiId = "kimi-k2.5"),
            AgentMessageUi(id = "assistant-2", content = "最终回答", modelApiId = "kimi-k2.5"),
            UserMessageUi(id = "user-2", content = "下一轮"),
            AgentMessageUi(id = "assistant-3", content = "旧消息"),
        )
        val turns = buildShareTurns(messages)

        assertEquals("kimi-k2.5", turns.getValue("assistant-2").modelApiId)
        assertEquals("kimi-k2.5", shareFooterBrand(listOf(turns.getValue("assistant-2")), "Eta Mod"))
        assertEquals(null, turns.getValue("assistant-3").modelApiId)
        assertEquals("Eta Mod", shareFooterBrand(listOf(turns.getValue("assistant-3")), "Eta Mod"))
        assertEquals(
            "Eta Mod",
            shareFooterBrand(
                listOf(turns.getValue("assistant-2").copy(modelApiId = "bad\nmodel")),
                "Eta Mod",
            ),
        )
    }

    @Test
    fun bitmapGeometryUsesDshPixelRatioAndBudget() {
        val geometry = MessageShareImage.bitmapGeometry(420, 7_784, 2f)
        assertEquals(840, geometry?.width)
        assertEquals(15_568, geometry?.height)
        assertEquals(2f, geometry?.scale)
        assertTrue(840L * 15_568L <= 33_554_432L)
        assertEquals(null, MessageShareImage.bitmapGeometry(1_000, 70_000, 0.5f))
        assertEquals(null, MessageShareImage.bitmapGeometry(20_000, 20_000, 1f))
    }

    @Test
    fun markdownFallbackKeepsPromptSegmentsToolsAndModel() {
        val turn = ShareTurn(
            userPrompt = "问题",
            segments = listOf(
                ShareTurnSegment.Markdown("**正文**"),
                ShareTurnSegment.ToolCall("run_command", "检查状态", ToolActivityStatusUi.Failed, "权限不足"),
                ShareTurnSegment.PlainText("结尾"),
            ),
            modelApiId = "kimi-k2.5",
        )

        val markdown = MessageShareImage.markdownFor(listOf(turn), "Eta Mod")

        assertTrue("## 用户" in markdown)
        assertTrue("**正文**" in markdown)
        assertTrue("run_command" in markdown)
        assertTrue("权限不足" in markdown)
        assertTrue("结尾" in markdown)
        assertTrue(markdown.trimEnd().endsWith("kimi-k2.5"))
    }

    @Test
    fun shareFormatPopupSelectsEachPresetAndCloses() {
        val showPopup = mutableStateOf(true)
        val selections = mutableListOf<ShareImageFormat>()
        composeRule.setContent {
            MiuixTheme(colors = lightColorScheme()) {
                Scaffold {
                    Box(modifier = Modifier.width(42.dp)) {
                        ShareImageFormatPopup(
                            show = showPopup.value,
                            onDismissRequest = { showPopup.value = false },
                            onSelect = {
                                selections += it
                                showPopup.value = false
                            },
                        )
                    }
                }
            }
        }

        val context = RuntimeEnvironment.getApplication()
        ShareImageFormat.entries.forEach { imageFormat ->
            showPopup.value = true
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.waitForIdle()
            val label = context.getString(
                when (imageFormat) {
                    ShareImageFormat.Phone -> R.string.share_image_format_phone
                    ShareImageFormat.Tablet -> R.string.share_image_format_tablet
                    ShareImageFormat.Desktop -> R.string.share_image_format_desktop
                },
            )
            composeRule.onNodeWithText(label).assertIsDisplayed().performClick()
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.waitForIdle()
            assertEquals(imageFormat, selections.last())
            assertFalse(showPopup.value)
        }
        assertEquals(ShareImageFormat.entries.toList(), selections)
    }

    private companion object {
        const val ShareCardTag = "share-card"
    }
}
