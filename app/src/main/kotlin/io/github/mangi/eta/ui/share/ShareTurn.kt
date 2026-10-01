package io.github.mangi.eta.ui.share

import io.github.mangi.eta.agent.model.AgentFileReferencePromptCodec
import io.github.mangi.eta.ui.model.AgentChatMessageUi
import io.github.mangi.eta.ui.model.AgentMessageUi
import io.github.mangi.eta.ui.model.ToolActivityMessageUi
import io.github.mangi.eta.ui.model.ToolActivityStatusUi
import io.github.mangi.eta.ui.model.ToolSummaryMessageUi
import io.github.mangi.eta.ui.model.UserMessageUi

/**
 * Eta Mod：一轮分享内容的一个段。
 *
 * 一轮 = 用户提问 + 其后按真实时序排列的助手正文块与工具调用。思考过程与系统通知
 * 不进分享图。段的数据全部来自消息流原文（持久化在 conversation_messages 表），
 * 组装过程不发起任何异步解析。
 */
internal sealed interface ShareTurnSegment {

    /** Markdown 正文块（renderMarkdown = true 的助手消息）。 */
    data class Markdown(val text: String) : ShareTurnSegment

    /** 纯文本块（renderMarkdown = false 的助手消息，原样排版不走 Markdown）。 */
    data class PlainText(val text: String) : ShareTurnSegment

    /** 一次工具调用的静态摘要行。 */
    data class ToolCall(
        val toolName: String,
        val summary: String,
        val status: ToolActivityStatusUi,
        val failureLine: String?,
    ) : ShareTurnSegment
}

/**
 * 一轮可分享内容。当前每次只分享一轮；分享入口接受 List<ShareTurn>，
 * 后续多选多轮导出时组装层直接复用。
 */
internal data class ShareTurn(
    val userPrompt: String?,
    val segments: List<ShareTurnSegment>,
)

/**
 * 从消息流按轮组装分享内容，键为每轮最后一条助手消息的 id（即分享按钮
 * 所在的那条消息）。与 resolveFinalResultMessageIds 同一套轮边界语义：
 * 遇到下一条用户消息即封上一轮。
 */
internal fun buildShareTurns(messages: List<AgentChatMessageUi>): Map<String, ShareTurn> {
    val result = LinkedHashMap<String, ShareTurn>()
    val segments = ArrayList<ShareTurnSegment>()
    var userPrompt: String? = null
    var lastAgentMessageId: String? = null

    fun closeTurn() {
        val id = lastAgentMessageId ?: return
        if (segments.isNotEmpty()) {
            result[id] = ShareTurn(userPrompt = userPrompt, segments = segments.toList())
        }
    }

    messages.forEach { message ->
        when (message) {
            is UserMessageUi -> {
                closeTurn()
                segments.clear()
                lastAgentMessageId = null
                userPrompt = AgentFileReferencePromptCodec.parse(message.content).request
                    .takeIf { it.isNotBlank() }
            }
            is AgentMessageUi -> {
                if (message.content.isNotBlank()) {
                    segments += if (message.renderMarkdown) {
                        ShareTurnSegment.Markdown(message.content)
                    } else {
                        ShareTurnSegment.PlainText(message.content)
                    }
                }
                lastAgentMessageId = message.id
            }
            is ToolActivityMessageUi -> {
                // 失败原因直接放在行内：剥离去重「失败」前缀与日志用的 code= 尾巴。
                val failureLine = if (message.status == ToolActivityStatusUi.Failed) {
                    message.resultSummary
                        ?.lineSequence()?.firstOrNull()
                        ?.removePrefix("失败 · ")
                        ?.substringBefore(" · code=")
                        ?.takeIf { it.isNotBlank() && it != "失败" }
                } else {
                    null
                }
                segments += ShareTurnSegment.ToolCall(
                    toolName = message.toolName,
                    summary = message.argumentsSummary,
                    status = message.status,
                    failureLine = failureLine,
                )
            }
            is ToolSummaryMessageUi -> message.tools.forEach { toolName ->
                segments += ShareTurnSegment.ToolCall(
                    toolName = toolName,
                    summary = "",
                    status = ToolActivityStatusUi.Unknown,
                    failureLine = null,
                )
            }
            else -> Unit
        }
    }
    closeTurn()
    return result
}
