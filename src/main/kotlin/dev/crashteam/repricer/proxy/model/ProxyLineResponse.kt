package dev.crashteam.repricer.proxy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProxyLineResponse(
    val count: Int,
    val results: List<ProxyLineResult>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProxyLineResult(
    val id: Long,
    val ip: String,
    @JsonProperty("port_http")
    val portHttp: String,
    @JsonProperty("port_socks5")
    val portSocks5: String,
    val user: String,
    val username: String,
    val password: String,

)
