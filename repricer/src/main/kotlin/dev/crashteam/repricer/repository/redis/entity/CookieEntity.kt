package dev.crashteam.repricer.repository.redis.entity

import java.time.LocalDateTime

data class CookieEntity(
    val name: String,
    val value: String,
    val expiryAt: LocalDateTime
)
