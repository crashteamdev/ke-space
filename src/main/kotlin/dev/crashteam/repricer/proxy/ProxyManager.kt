package dev.crashteam.repricer.proxy

import dev.crashteam.repricer.proxy.model.ProxyAddress
import org.springframework.stereotype.Component

@Component
class ProxyManager(
    private val proxySource: List<ProxySource>
) {

    fun getRandomProxy(): ProxyAddress {
        val randomSource = proxySource.random()
        return randomSource.getProxies().random()
    }

    fun getProxyByHost(host: String): ProxyAddress? {
        return proxySource.flatMap {
            it.getProxies()
        }.find { it.host == host }
    }

}
