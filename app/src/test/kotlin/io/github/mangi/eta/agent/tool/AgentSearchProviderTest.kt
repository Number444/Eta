package io.github.mangi.eta.agent.tool

import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentSearchProviderTest {

    @Test
    fun currentDefaultsToExaWithoutPersistedValue() {
        // 单元测试环境未注入 localAgent SharedPreferences，getLocalString 恒为空串。
        assertEquals(AgentSearchProvider.EXA, AgentSearchProvider.current())
    }

    @Test
    fun notConfiguredWithoutApiKey() {
        AgentSearchProvider.values().forEach { provider ->
            assertFalse(provider.displayName, provider.isConfigured())
        }
    }

    @Test
    fun exaParsesPublishedDateAndText() {
        val body = """
            {"results":[{"title":"T","url":"https://example.test/","publishedDate":"2026-01-01","text":"正文"}]}
        """.trimIndent()
        val results = AgentSearchProvider.EXA.parseResults(body)!!
        assertEquals(1, results.length())
        val item = results.getJSONObject(0)
        assertEquals("T", item.getString("title"))
        assertEquals("https://example.test/", item.getString("url"))
        assertEquals("2026-01-01", item.getString("published"))
        assertEquals("正文", item.getString("text"))
    }

    @Test
    fun deepseekParsesDateAndSummary() {
        val body = """
            {"results":[{"title":"T","url":"https://example.test/","date":"2026-06-01T00:00:00+00:00","snippet":"短","summary":"长摘要"}]}
        """.trimIndent()
        val results = AgentSearchProvider.DEEPSEEK.parseResults(body)!!
        val item = results.getJSONObject(0)
        assertEquals("2026-06-01T00:00:00+00:00", item.getString("published"))
        assertEquals("长摘要", item.getString("text"))
    }

    @Test
    fun deepseekFallsBackToSnippetWhenSummaryBlank() {
        val body = """
            {"results":[{"title":"T","url":"https://example.test/","snippet":"短摘要"}]}
        """.trimIndent()
        val results = AgentSearchProvider.DEEPSEEK.parseResults(body)!!
        assertEquals("短摘要", results.getJSONObject(0).getString("text"))
    }

    @Test
    fun parseResultsReturnsNullOnInvalidJson() {
        AgentSearchProvider.values().forEach { provider ->
            assertNull(provider.displayName, provider.parseResults("not json"))
        }
    }

    @Test
    fun parseResultsReturnsEmptyArrayWhenResultsMissing() {
        AgentSearchProvider.values().forEach { provider ->
            assertEquals(0, provider.parseResults("{}")!!.length())
        }
    }

    @Test
    fun deepseekBuildsBearerRequestWithCountAndNoContent() {
        val request = AgentSearchProvider.DEEPSEEK.buildRequest("查询词", 10, 1_000)
        assertEquals("https://api.deepseek.com/v1/tools/search", request.url.toString())
        assertTrue(request.header("Authorization")!!.startsWith("Bearer"))
        val payload = JSONObject(request.bodyText())
        assertEquals("查询词", payload.getString("query"))
        assertEquals(10, payload.getInt("count"))
        assertFalse(payload.getBoolean("need_content"))
    }

    @Test
    fun deepseekTruncatesQueryTo128Chars() {
        val request = AgentSearchProvider.DEEPSEEK.buildRequest("x".repeat(200), 10, 1_000)
        assertEquals(128, JSONObject(request.bodyText()).getString("query").length)
    }

    @Test
    fun exaBuildsApiKeyHeaderRequest() {
        val request = AgentSearchProvider.EXA.buildRequest("查询词", 15, 1_000)
        assertEquals("https://api.exa.ai/search", request.url.toString())
        assertTrue(request.header("x-api-key") != null)
        val payload = JSONObject(request.bodyText())
        assertEquals(15, payload.getInt("numResults"))
        assertEquals(1_000, payload.getJSONObject("contents").getJSONObject("text").getInt("maxCharacters"))
    }

    private fun okhttp3.Request.bodyText(): String {
        val buffer = Buffer()
        body!!.writeTo(buffer)
        return buffer.readUtf8()
    }
}
