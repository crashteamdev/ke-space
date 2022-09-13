package dev.crashteam.repricer.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "service")
data class ServiceProperties(
    @field:NotNull
    val proxy: ProxyProperties? = null,
)

data class ProxyProperties(
    @field:NotEmpty
    val url: String? = null,
)
