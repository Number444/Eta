package io.github.mangi.eta.agent.runtime

import android.content.res.Configuration
import io.github.mangi.eta.R
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Eta Mod：岛参数 JSON 必须符合小米官方协议（param_v2 模版 1）：
 * A 区图文组件 1 + B 区空，小岛仅图标，安静胶囊（不自动展开）。
 * 字段名以《小米超级岛模板库》(20260129) 为准。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgentIslandNotifierTest {

    @Test
    fun focusParamMatchesOfficialTemplate1() {
        val root = JSONObject(AgentIslandNotifier.buildFocusParam("思考中", "Eta"))
        val v2 = root.getJSONObject("param_v2")

        assertEquals(1, v2.getInt("protocol"))
        assertEquals("agent_task", v2.getString("business"))
        assertFalse(v2.getBoolean("islandFirstFloat"))
        assertFalse(v2.getBoolean("enableFloat"))
        assertTrue(v2.getBoolean("updatable"))
        assertEquals("思考中", v2.getString("ticker"))
        assertEquals(AgentIslandNotifier.PIC_KEY, v2.getString("tickerPic"))
        assertEquals("思考中", v2.getString("aodTitle"))

        val baseInfo = v2.getJSONObject("baseInfo")
        assertEquals(1, baseInfo.getInt("type"))
        assertEquals("思考中", baseInfo.getString("title"))
        assertEquals("Eta", baseInfo.getString("content"))

        val island = v2.getJSONObject("param_island")
        assertEquals(1, island.getInt("islandProperty"))

        val bigIsland = island.getJSONObject("bigIslandArea")
        val left = bigIsland.getJSONObject("imageTextInfoLeft")
        assertEquals(1, left.getInt("type"))
        assertEquals(1, left.getJSONObject("picInfo").getInt("type"))
        assertEquals(AgentIslandNotifier.PIC_KEY, left.getJSONObject("picInfo").getString("pic"))
        val textInfo = left.getJSONObject("textInfo")
        assertEquals("思考中", textInfo.getString("title"))
        assertFalse(textInfo.getBoolean("showHighlightColor"))
        // 模版 1：B 区为空（大岛除 A 区组件外不携带其它内容字段）
        assertEquals(setOf("imageTextInfoLeft"), bigIsland.keys().asSequence().toSet())

        val small = island.getJSONObject("smallIslandArea").getJSONObject("picInfo")
        assertEquals(1, small.getInt("type"))
        assertEquals(AgentIslandNotifier.PIC_KEY, small.getString("pic"))
    }

    @Test
    fun focusParamStateTextsStayWithinFourChineseChars() {
        // 官方摘要态规范：总字数建议不超过 4 个中文字（校验简体中文文案）。
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val zhConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.SIMPLIFIED_CHINESE)
        }
        val zhContext = context.createConfigurationContext(zhConfig)
        listOf(
            R.string.island_state_thinking,
            R.string.island_state_output,
            R.string.island_state_tool,
            R.string.island_state_done,
            R.string.island_state_failed,
            R.string.island_state_stopped,
        ).forEach { resId ->
            val text = zhContext.getString(resId)
            assertTrue("$text 超过 4 个字", text.length <= 4)
        }
    }
}
