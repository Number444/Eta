package io.github.mangi.eta.agent.model

import org.json.JSONArray
import io.github.mangi.eta.agent.tool.AgentToolCapabilities

/** 声明模型可见的工具及其 JSON Schema；不包含任何执行逻辑。 */
internal object AgentToolCatalog {
    fun build(
        terminalTools: Boolean,
        browserTools: Boolean,
        webSearchTools: Boolean = false,
        deviceDirectTools: Boolean = true,
        deviceSensitiveReadTools: Boolean = false,
        deviceSensitiveActionTools: Boolean = false,
        skillGitHubDiscovery: Boolean = false,
        skillGitHubInstall: Boolean = false,
        memoryTools: Boolean = false,
        memoryWritable: Boolean = true,
        capabilities: AgentToolCapabilities = AgentToolCapabilities(rootAvailable = true),
        localWebSearch: Boolean = true,
    ): JSONArray =
        capabilities.project(JSONArray().also { tools ->
            AgentContextAppToolCatalog.appendTo(tools)
            AgentGestureToolCatalog.appendTo(tools)
            AgentTextSystemToolCatalog.appendTo(tools)
            AgentDeviceToolCatalog.appendTo(
                tools,
                directTools = deviceDirectTools,
                sensitiveReadTools = deviceSensitiveReadTools,
                sensitiveActionTools = deviceSensitiveActionTools,
            )
            // Eta Mod：官方免 Key 搜索在本机不可用（连续超时），web_search 只走 Exa（需已配置 API Key）；
            // 上游 AgentWebToolCatalog 不再注册官方 web_search，仅保留 fetch_url。
            if (browserTools) {
                AgentWebToolCatalog.appendTo(tools, includeSearch = false)
                AgentBrowserToolCatalog.appendTo(tools)
            }
            if (webSearchTools && localWebSearch && AgentWebSearchToolCatalog.exaConfigured()) AgentWebSearchToolCatalog.appendTo(tools)
            AgentSkillToolCatalog.appendTo(
                tools,
                githubDiscovery = skillGitHubDiscovery,
                githubInstall = skillGitHubInstall,
            )
            if (memoryTools) AgentMemoryToolCatalog.appendTo(tools, writable = memoryWritable)
            if (terminalTools) {
                AgentFileVisionToolCatalog.appendTo(tools)
                AgentTerminalToolCatalog.appendTo(tools)
                AgentFileToolCatalog.appendTo(tools)
            }
        })
}
