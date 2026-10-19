package io.github.mangi.eta.agent.tool

import io.github.mangi.eta.config.Prefs
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 网页搜索引擎抽象（Eta Mod）。
 *
 * 每个引擎负责自己的请求构造（URL / 鉴权 / 字段）与响应解析，统一映射为
 * `results[{title, url, published, text}]`；截断预算与喂模型的打包格式由
 * [AgentWebSearchTool] 统一处理。新增引擎 = 枚举值 + 两个 when 分支 + Prefs key +
 * 设置页入口。
 *
 * Key 与供应商同款——明文存放在本地 Agent 配置，仅本进程读取。
 */
internal enum class AgentSearchProvider(
    val prefValue: String,
    val displayName: String,
    val apiKeyPrefKey: String,
) {
    EXA("exa", "Exa", Prefs.Keys.AGENT_EXA_API_KEY),
    DEEPSEEK("deepseek", "DeepSeek", Prefs.Keys.AGENT_DEEPSEEK_API_KEY),
    ;

    fun apiKey(): String = Prefs.getLocalString(apiKeyPrefKey).trim()

    fun isConfigured(): Boolean = apiKey().isNotBlank()

    /** DeepSeek query 上限 128 字符，超长截断；Exa 无此限制。 */
    private fun sanitizeQuery(query: String): String = when (this) {
        EXA -> query
        DEEPSEEK -> query.take(DEEPSEEK_MAX_QUERY_CHARS)
    }

    fun buildRequest(query: String, numResults: Int, perResultTextChars: Int): Request {
        val payload = when (this) {
            EXA -> JSONObject()
                .put("query", sanitizeQuery(query))
                .put("numResults", numResults)
                .put("type", "auto")
                .put(
                    "contents",
                    JSONObject().put("text", JSONObject().put("maxCharacters", perResultTextChars)),
                )
            // need_content 固定 false：summary（600-800 字）本就是官方定位给模型读的摘要，
            // 单价相同且省流量。
            DEEPSEEK -> JSONObject()
                .put("query", sanitizeQuery(query))
                .put("count", numResults)
                .put("need_content", false)
        }
        val builder = Request.Builder()
            .url(
                when (this) {
                    EXA -> "https://api.exa.ai/search"
                    DEEPSEEK -> "https://api.deepseek.com/v1/tools/search"
                },
            )
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
        when (this) {
            EXA -> builder.header("x-api-key", apiKey())
            DEEPSEEK -> builder.header("Authorization", "Bearer ${apiKey()}")
        }
        return builder.build()
    }

    /**
     * 把引擎原始响应映射为统一结构 `results[{title, url, published, text}]`；
     * 返回 null 表示响应无法解析。
     */
    fun parseResults(body: String): JSONArray? {
        val raw = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val items = raw.optJSONArray("results") ?: return JSONArray()
        val results = JSONArray()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            results.put(
                JSONObject()
                    .put("title", item.optString("title").trim())
                    .put("url", item.optString("url").trim())
                    .put(
                        "published",
                        when (this) {
                            EXA -> item.optString("publishedDate")
                            DEEPSEEK -> item.optString("date")
                        }.trim(),
                    )
                    .put(
                        "text",
                        when (this) {
                            EXA -> item.optString("text")
                            DEEPSEEK -> item.optString("summary").ifBlank { item.optString("snippet") }
                        }.trim(),
                    ),
            )
        }
        return results
    }

    companion object {
        private const val DEEPSEEK_MAX_QUERY_CHARS = 128
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /** 当前选中的搜索引擎；缺省或未知值回退 EXA，兼容既有 Exa 用户的存量配置。 */
        fun current(): AgentSearchProvider {
            val stored = Prefs.getLocalString(Prefs.Keys.AGENT_SEARCH_PROVIDER).trim()
            return values().firstOrNull { it.prefValue == stored } ?: EXA
        }
    }
}
