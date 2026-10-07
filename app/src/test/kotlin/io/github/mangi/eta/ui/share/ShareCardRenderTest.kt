package io.github.mangi.eta.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.ui.app.AgentAppTheme
import io.github.mangi.eta.ui.markdown.LocalMarkdownStaticExport
import io.github.mangi.eta.ui.markdown.MarkdownInlineStyle
import io.github.mangi.eta.ui.markdown.MarkdownTone
import io.github.mangi.eta.ui.markdown.rememberMarkdownStyle
import io.github.mangi.eta.ui.model.ToolActivityStatusUi
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled

/**
 * Eta Mod：分享卡片本地渲染验证。
 *
 * 在 Robolectric（NATIVE 图形模式）下把 ShareTurnsCard 真实渲染成 PNG 落盘到
 * `app/build/outputs/share-card/ShareCardTest.png`，供人工核对布局：
 * 用户气泡右对齐、工具时间线竖线/节点圆、Markdown 排版、页脚。
 *
 * 注意：宿主机字体排版与真机略有出入（字宽/行高），布局结构与配色可在此完全验证，
 * 字体观感以真机截图为最终准。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w420dp-h2000dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareCardRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shareCardRendersToPng() {
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
                        """.trimIndent(),
                    ),
                ),
            ),
        )
        val documents = MessageShareImage.parseAllSegments(turns, MarkdownInlineStyle.Default)
        assertTrue("markdown 段应解析成功", documents.isNotEmpty())

        composeRule.setContent {
            AgentAppTheme(appearance = AppearanceSettings(), applyInterfaceScale = false) {
                val style = rememberMarkdownStyle(MarkdownTone.Answer)
                // 与 MessageShareImage 出图路径一致：静态导出模式隐藏交互件，
                // squircle 关闭回退普通圆角（软件画布不支持 RuntimeShader）。
                CompositionLocalProvider(
                    LocalMarkdownStaticExport provides true,
                    LocalSquircleEnabled provides false,
                ) {
                    Box(modifier = Modifier.width(420.dp)) {
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
        composeRule.waitForIdle()

        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        assertTrue("渲染结果应为非空位图", bitmap.width > 0 && bitmap.height > 0)

        val outDir = File("build/outputs/share-card").apply { mkdirs() }
        val outFile = File(outDir, "ShareCardTest.png")
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        println("分享卡片渲染图: ${outFile.absolutePath} (${bitmap.width}x${bitmap.height})")
        assertTrue("PNG 应已落盘", outFile.length() > 0)
    }
}
