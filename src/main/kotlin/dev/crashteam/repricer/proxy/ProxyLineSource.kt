package dev.crashteam.repricer.proxy

import dev.crashteam.repricer.config.RedisConfig
import dev.crashteam.repricer.proxy.model.ProxyAddress
import dev.crashteam.repricer.proxy.model.ProxyLineResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.net.Proxy

@Component
class ProxyLineSource(
    private val restTemplate: RestTemplate,
    @Value("\${proxies.proxyline.apiKey}")
    private val proxyLineApiKey: String
) : ProxySource {

    val proxyLineUrl = "https://panel.proxyline.net/api/proxies/?api_key=$proxyLineApiKey&status=active"

    @Cacheable(value = [RedisConfig.PROXY_SOURCE_NAME], key = "proxy-line", unless="#result == null")
    override fun getProxies(): List<ProxyAddress> {
        val responseEntity = restTemplate.getForEntity<ProxyLineResponse>(proxyLineUrl)
        val proxyLineResponse = responseEntity.body!!
        return proxyLineResponse.results.map {
            ProxyAddress(
                type = Proxy.Type.SOCKS,
                host = it.ip,
                port = it.portHttp.toInt(),
                socksPort = it.portSocks5.toInt(),
                login = it.user,
                password = it.password
            )
        }
    }
}
