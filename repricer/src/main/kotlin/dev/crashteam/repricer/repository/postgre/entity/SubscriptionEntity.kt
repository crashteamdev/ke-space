package dev.crashteam.repricer.repository.postgre.entity

import dev.crashteam.repricer.db.model.enums.SubscriptionPlan

data class SubscriptionEntity(
    val id: Long,
    val name: String,
    val plan: SubscriptionPlan,
    val price: Long
)
