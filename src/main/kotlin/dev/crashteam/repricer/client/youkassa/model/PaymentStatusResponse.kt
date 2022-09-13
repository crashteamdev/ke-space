package dev.crashteam.repricer.client.youkassa.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class PaymentStatusResponse(
    val id: String,
    val status: String,
    val paid: Boolean,
    val amount: PaymentAmount,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    val description: String,
    @JsonProperty("expires_at")
    val expiresAt: LocalDateTime?,
    val refundable: Boolean
)
