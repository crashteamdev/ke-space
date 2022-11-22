package dev.crashteam.repricer.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "repricer")
data class RepricerProperties(
    @field:NotEmpty
    val passwordKey: String? = null,
    @field:NotEmpty
    val productCron: String? = null,
    @field:NotEmpty
    val accountUpdateDataCron: String? = null,
    @field:NotEmpty
    val priceChangeCron: String? = null,
    @field:NotEmpty
    val paymentCron: String? = null,
    @field:NotEmpty
    val accountInitializeCron: String? = null,
    @field:NotEmpty
    val repairStuckStateCron: String? = null,
    val cookieBotProtectionBypassEnabled: Boolean = false,
    @field:NotNull
    val maxUpdateInProgress: Int? = null
)
