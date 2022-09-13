package dev.crashteam.repricer.client.youkassa.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentConfirmation(
    val type: String,
    @JsonProperty("return_url")
    val returnUrl: String,
)
