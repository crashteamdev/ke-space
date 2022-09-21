package dev.crashteam.repricer.repository.redis.entity

import dev.crashteam.repricer.proxy.model.ProxyAddress
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import java.time.LocalDateTime

@RedisHash(value = "SecureCookie", timeToLive = 900)
data class CookieEntity(
    @Id
    val userId: String,
    val proxyAddress: ProxyAddress,
    val name: String,
    val value: String,
    val expiryAt: LocalDateTime
)
