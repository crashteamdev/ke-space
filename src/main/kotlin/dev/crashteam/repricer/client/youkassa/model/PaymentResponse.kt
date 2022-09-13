package dev.crashteam.repricer.client.youkassa.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentResponse(
    val id: String,
    val status: String,
    val paid: Boolean,
    val amount: PaymentAmount,
    val confirmation: PaymentConfirmationUrl
)

data class PaymentConfirmationUrl(
    val type: String,
    @JsonProperty("confirmation_url")
    val confirmationUrl: String,
)
