package dev.crashteam.repricer.repository.redis.entity

data class UserTokenEntity(
    val accessToken: String,
    val refreshToken: String
)
