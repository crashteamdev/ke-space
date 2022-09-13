package dev.crashteam.repricer.repository.postgre.entity

import java.time.LocalDateTime

data class AccountEntity(
    val id: Long? = null,
    val userId: String,
    val subscription: SubscriptionEntity? = null,
    val subscriptionValidUntil: LocalDateTime? = null
)
