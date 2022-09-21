package dev.crashteam.repricer.proxy

import dev.crashteam.repricer.proxy.model.ProxyAddress

interface ProxySource {
    fun getProxies(): List<ProxyAddress>
}
