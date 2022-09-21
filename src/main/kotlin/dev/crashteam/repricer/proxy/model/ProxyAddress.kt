package dev.crashteam.repricer.proxy.model

import java.net.Proxy

data class ProxyAddress(
    val type: Proxy.Type,
    val host: String,
    val port: Int? = null,
    val socksPort: Int? = null,
    val login: String? = null,
    val password: String? = null
)
