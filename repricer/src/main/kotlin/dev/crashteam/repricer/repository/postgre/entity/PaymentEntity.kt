package dev.crashteam.repricer.repository.postgre.entity

import dev.crashteam.repricer.db.model.enums.PaymentStatus
import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import java.util.*

data class PaymentEntity(
    val id: UUID,
    val userId: String,
    val externalId: String,
    val amount: Long,
    val subscriptionPlan: SubscriptionPlan,
    val status: PaymentStatus,
    val multiply: Short
)
