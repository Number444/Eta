package io.github.mangi.eta.agent.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

/** Eta Mod：任务完成通知正文摘要的规整规则。 */
class AgentCompletionNotifierTest {

    @Test
    fun snippetCollapsesWhitespace() {
        assertEquals("第一行 第二行", AgentCompletionNotifier.snippet("  第一行\n\n  第二行\t "))
    }

    @Test
    fun snippetKeepsShortTextIntact() {
        assertEquals("完成", AgentCompletionNotifier.snippet("完成"))
    }

    @Test
    fun snippetTruncatesLongTextWithEllipsis() {
        val long = "字".repeat(AgentCompletionNotifier.MAX_TEXT_CHARS + 10)
        val result = AgentCompletionNotifier.snippet(long)
        assertEquals(AgentCompletionNotifier.MAX_TEXT_CHARS + 1, result.length)
        assertEquals("…", result.last().toString())
    }

    @Test
    fun snippetHandlesBlank() {
        assertEquals("", AgentCompletionNotifier.snippet("   \n  "))
    }
}
