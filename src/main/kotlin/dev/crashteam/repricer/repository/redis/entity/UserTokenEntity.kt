package dev.crashteam.repricer.repository.redis.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed

@RedisHash(value = "KeUserToken", timeToLive = 900)
data class UserTokenEntity(
    @Id
    val id: String,
    @Indexed
    val userId: String,
    @Indexed
    val keAccountId: String,
    val accessToken: String,
    val refreshToken: String
)
