package dev.crashteam.repricer.client.ke.model.lk

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Long,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    val scope: String,
    @JsonProperty("token_type")
    val tokenType: String
)
