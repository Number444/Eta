package io.github.mangi.eta.agent.model

import org.json.JSONArray
import org.json.JSONObject

/** 网页搜索工具 schema（Eta Mod，先支持 Exa）。 */
internal object AgentWebSearchToolCatalog {

    const val WEB_SEARCH = "web_search"

    fun appendTo(tools: JSONArray) {
        tools.put(
            AgentToolSchema.function(
                name = WEB_SEARCH,
                description = "使用 Exa 搜索引擎检索互联网公开网页，返回标题、链接、发布时间与正文摘要。" +
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
