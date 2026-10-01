package io.github.mangi.eta.data.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Eta Mod：余额 JSON 点号路径提取与地址解析（纯函数部分）。 */
class ProviderBalanceFetcherTest {

    @Test
    fun resolveBalanceUrlAcceptsFullUrl() {
        assertEquals(
            "https://api.deepseek.com/user/balance",
            ProviderBalanceFetcher.resolveBalanceUrl(
                " https://api.deepseek.com/user/balance ",
                "https://api.deepseek.com/v1",
            ),
        )
    }

    @Test
    fun resolveBalanceUrlAppendsPathToBaseUrl() {
        assertEquals(
            "https://api.deepseek.com/v1/user/balance",
            ProviderBalanceFetcher.resolveBalanceUrl(
                "/user/balance",
                "https://api.deepseek.com/v1/",
            ),
        )
    }

    @Test
    fun resolveBalanceUrlRejectsRelativePathWithoutBaseUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            ProviderBalanceFetcher.resolveBalanceUrl("/user/balance", " ")
        }
    }

    @Test
    fun extractBalanceReadsNestedObjectPath() {
        val body = """{"code":0,"data":{"total_balance":12.5,"currency":"CNY"}}"""

        assertEquals(
            "12.5",
            ProviderBalanceFetcher.extractBalance(body, "data.total_balance"),
        )
    }

    @Test
    fun extractBalanceFormatsIntegralDoubleWithoutDecimals() {
        val body = """{"data":{"balance":66.0}}"""

        assertEquals("66", ProviderBalanceFetcher.extractBalance(body, "data.balance"))
    }

    @Test
    fun extractBalanceReadsArrayIndex() {
        val body = """{"data":[{"balance":3.25}]}"""

        assertEquals("3.25", ProviderBalanceFetcher.extractBalance(body, "data.0.balance"))
    }

    @Test
    fun extractBalanceRejectsMissingSegment() {
        val body = """{"data":{"other":1}}"""

        assertThrows(IllegalStateException::class.java) {
            ProviderBalanceFetcher.extractBalance(body, "data.total_balance")
        }
    }

    @Test
    fun extractBalanceRejectsBooleanLeaf() {
        assertThrows(IllegalStateException::class.java) {
            ProviderBalanceFetcher.extractBalance("""{"ok":true}""", "ok")
        }
    }

    @Test
    fun extractBalanceKeepsStringLeaf() {
        val parsed = JSONObject("""{"data":{"balance":"¥ 8.88"}}""")

        assertEquals(
            "¥ 8.88",
            ProviderBalanceFetcher.extractBalance(parsed.toString(), "data.balance"),
        )
    }
}
