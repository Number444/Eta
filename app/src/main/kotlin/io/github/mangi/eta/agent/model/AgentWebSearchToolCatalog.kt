package io.github.mangi.eta.agent.model

import io.github.mangi.eta.agent.tool.AgentSearchProvider
import org.json.JSONArray
import org.json.JSONObject

/** 网页搜索工具 schema（Eta Mod，引擎由 [AgentSearchProvider] 抽象）。 */
internal object AgentWebSearchToolCatalog {

    const val WEB_SEARCH = "web_search"

    /** web_search 仅在当前选中的搜索引擎已配置 API Key 时注册（官方免 Key 搜索在本机不可用，已移除）。 */
    fun searchConfigured(): Boolean =
        AgentSearchProvider.current().isConfigured()

    fun appendTo(tools: JSONArray) {
        tools.put(
            AgentToolSchema.function(
                name = WEB_SEARCH,
                description = "使用网页搜索引擎检索互联网公开网页，返回标题、链接、发布时间与正文摘要。" +
                    "需要最新信息、时事、资料查证或用户要求联网搜索时调用。",
                parameters = JSONObject()
                    .put("type", "object")
                    .put(
                        "properties",
                        JSONObject()
                            .put(
                                "query",
                                JSONObject()
                                    .put("type", "string")
                                    .put("description", "搜索关键词或自然语言问题")
                            )
                            .put(
                                "num_results",
                                JSONObject()
                                    .put("type", "integer")
                                    .put("description", "返回 5 到 25 条结果，默认 15")
                            )
                    )
                    .put("required", JSONArray().put("query"))
            )
        )
    }
}
