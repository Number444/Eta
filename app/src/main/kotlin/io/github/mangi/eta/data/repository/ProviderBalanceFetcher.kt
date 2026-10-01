package io.github.mangi.eta.data.repository

import android.util.Log

import io.github.mangi.eta.agent.model.AgentHttpClient
import io.github.mangi.eta.agent.model.ProviderRequestHeaders
import io.github.mangi.eta.data.model.ProviderSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Eta Mod：按厂商配置的「余额查询」拉取账户余额。
 * 固定 GET + Bearer 认证 + 自定义请求头；地址支持完整 URL 或以 / 开头相对 Base URL。
 */
internal object ProviderBalanceFetcher {

    suspend fun fetch(provider: ProviderSetting): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(resolveBalanceUrl(provider.balanceUrl, provider.baseUrl))
                .headers(
                    okhttp3.Headers.Builder()
                        .add("Accept", "application/json")
                        .apply {
                            if (provider.apiKey.isNotBlank()) {
                                add("Authorization", "Bearer ${provider.apiKey}")
                            }
                            ProviderRequestHeaders.mergeInto(this, provider.baseUrl, provider.customHeaders)
                        }
                        .build(),
                )
                .get()
                .build()
            val body = AgentHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
            extractBalance(body, provider.balanceJsonPath)
        }
    }

    internal fun resolveBalanceUrl(balanceUrl: String, baseUrl: String): String {
        val raw = balanceUrl.trim()
        require(raw.isNotEmpty()) { "未配置余额 API 地址" }
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        val base = baseUrl.trim().trimEnd('/')
        require(base.isNotEmpty()) { "未配置 Base URL，余额地址必须使用完整 URL" }
        return base + if (raw.startsWith("/")) raw else "/$raw"
    }

    /** 按点号路径从 JSON 中取值；对象段按键、数组段按下标。 */
    internal fun extractBalance(body: String, path: String): String {
        val trimmed = path.trim()
        require(trimmed.isNotEmpty()) { "未配置余额 JSON 路径" }
        var current: Any? = JSONObject(body)
        for (segment in trimmed.split('.')) {
            current = when (val node = current) {
                is JSONObject ->
                    if (node.has(segment) && !node.isNull(segment)) node.get(segment)
                    else error("余额 JSON 路径不存在：$trimmed")
                is JSONArray -> node.get(
                    segment.toIntOrNull() ?: error("余额 JSON 路径不存在：$trimmed"),
                )
                else -> error("余额 JSON 路径不存在：$trimmed")
            }
        }
        return when (val value = current) {
            is Number -> {
                val double = value.toDouble()
                if (double % 1.0 == 0.0) double.toLong().toString() else value.toString()
            }
            is Boolean, null -> error("余额 JSON 路径不存在：$trimmed")
            else -> value.toString()
        }
    }
}

/**
 * Eta Mod：余额缓存（providerId → 展示文本）。TTL 内复用，失败缓存空串不显示；
 * 配置被清除的供应商会立即从缓存移除。
 */
internal object ProviderBalanceStore {
    private const val TTL_MS = 5 * 60 * 1000L
    private const val TAG = "EtaBalance"

    private data class CacheEntry(val value: String, val fetchedAt: Long)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    private val _balances = MutableStateFlow<Map<String, String>>(emptyMap())

    /** 供模型选择 popout 订阅；值为空串表示未配置或拉取失败（不显示）。 */
    val balances: StateFlow<Map<String, String>> = _balances.asStateFlow()

    fun refresh(providers: List<ProviderSetting>, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val configurable = providers
            .filter { it.balanceUrl.isNotBlank() && it.balanceJsonPath.isNotBlank() }
            .associateBy { it.id }
        Log.d(
            TAG,
            "refresh: providers=${providers.size}, configurable=${configurable.size}, " +
                "configuredIds=${providers.filter { it.balanceUrl.isNotBlank() }.map { it.id.take(8) }}",
        )
        val removed = cache.keys - configurable.keys
        if (removed.isNotEmpty()) {
            removed.forEach(cache::remove)
            _balances.value = _balances.value - removed.toSet()
        }
        for (provider in configurable.values) {
            val entry = cache[provider.id]
            if (!force && entry != null && now - entry.fetchedAt < TTL_MS) continue
            if (!inFlight.add(provider.id)) continue
            scope.launch {
                try {
                    val value = ProviderBalanceFetcher.fetch(provider).getOrDefault("")
                    Log.d(TAG, "fetched ${provider.id.take(8)} -> '$value'")
                    cache[provider.id] = CacheEntry(value, System.currentTimeMillis())
                    _balances.value = _balances.value + (provider.id to value)
                } finally {
                    inFlight.remove(provider.id)
                }
            }
        }
    }
}
