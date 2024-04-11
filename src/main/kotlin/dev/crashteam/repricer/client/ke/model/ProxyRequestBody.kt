package dev.crashteam.repricer.client.ke.model

data class ProxyRequestBody(
    val url: String,
    val httpMethod: String,
    val context: List<ProxyRequestContext>? = null,
    val proxySource: ProxySource? = null
)

data class ProxyRequestContext(
    val key: String,
    val value: Any
)

enum class ProxySource(val value: String) {
    PROXY_LINE("PROXY_LINE"),
    MOBILE_PROXY("MOBILE_PROXY"),
    PROXYS_IO("PROXYS_IO")
}
