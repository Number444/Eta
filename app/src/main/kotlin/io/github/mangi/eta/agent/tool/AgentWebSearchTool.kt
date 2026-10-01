package io.github.mangi.eta.agent.tool

import io.github.mangi.eta.agent.model.AgentHttpClient
import io.github.mangi.eta.config.Prefs
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 网页搜索工具执行器（Eta Mod）。
 *
 * 先支持 Exa：POST https://api.exa.ai/search，x-api-key 头鉴权。
 * Key 与供应商同款——明文存放在本地 Agent 配置，仅本进程读取。
 * 同步阻塞执行：与终端等工具一致，跑在 Agent 循环的后台线程上。
 */
internal object AgentWebSearchTool {

    private const val EXA_SEARCH_URL = "https://api.exa.ai/search"
    private const val DEFAULT_RESULTS = 15
    private const val MIN_RESULTS = 5
    private const val MAX_RESULTS = 25
    private const val PER_RESULT_TEXT_CHARS = 1_000

    /** 返回给模型的正文总预算（覆盖 25 条 × 1000 字符），避免超长搜索结果挤占上下文。 */
    private const val TOTAL_TEXT_BUDGET_CHARS = 25_000

    fun execute(args: JSONObject): String {
        val query = args.optString("query").trim()
        if (query.isEmpty()) {
            return errorResult("INVALID_ARGUMENTS", "query 不能为空")
        }
        val apiKey = Prefs.getLocalString(Prefs.Keys.AGENT_EXA_API_KEY).trim()
        if (apiKey.isBlank()) {
            return errorResult("WEB_SEARCH_NOT_CONFIGURED", "未配置 Exa API Key，请在设置-工具中填写")
        }
        val numResults = args.optInt("num_results", DEFAULT_RESULTS).coerceIn(MIN_RESULTS, MAX_RESULTS)
        val payload = JSONObject()
            .put("query", query)
            .put("numResults", numResults)
            .put("type", "auto")
            .put(
                "contents",
                JSONObject().put("text", JSONObject().put("maxCharacters", PER_RESULT_TEXT_CHARS)),
            )
        val request = Request.Builder()
            .url(EXA_SEARCH_URL)
            .header("x-api-key", apiKey)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            AgentHttpClient.client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    errorResult(
                        "WEB_SEARCH_HTTP_${response.code}",
                        body.take(300).ifBlank { "Exa 请求失败（HTTP ${response.code}）" },
                    )
                } else {
                    formatResults(body)
                }
            }
        } catch (failure: IOException) {
            errorResult("WEB_SEARCH_NETWORK", "网络请求失败：${failure.message ?: failure.javaClass.simpleName}")
        } catch (failure: Exception) {
            errorResult("WEB_SEARCH_ERROR", failure.message ?: failure.javaClass.simpleName)
        }
    }

    private fun formatResults(body: String): String {
        val raw = runCatching { JSONObject(body) }.getOrNull()
            ?: return errorResult("WEB_SEARCH_BAD_RESPONSE", "Exa 返回了无法解析的响应")
        val items = raw.optJSONArray("results") ?: JSONArray()
        val results = JSONArray()
        var budget = TOTAL_TEXT_BUDGET_CHARS
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val text = item.optString("text").trim()
                .take(budget.coerceAtMost(PER_RESULT_TEXT_CHARS))
            if (text.isEmpty() && item.optString("url").isBlank()) continue
            budget -= text.length
            results.put(
                JSONObject()
                    .put("title", item.optString("title").trim())
                    .put("url", item.optString("url").trim())
                    .put("published", item.optString("publishedDate").trim())
                    .put("text", text),
            )
            if (budget <= 0) break
        }
        return JSONObject()
            .put("ok", true)
            .put("count", results.length())
            .put("results", results)
            .toString()
    }

    private fun errorResult(code: String, message: String): String =
        JSONObject()
            .put("ok", false)
            .put("code", code)
            .put("message", message)
            .toString()
}
