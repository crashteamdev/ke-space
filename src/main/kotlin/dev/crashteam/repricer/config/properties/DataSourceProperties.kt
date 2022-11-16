package dev.crashteam.repricer.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "data-source")
data class DataSourceProperties(
    @field:NotNull
    val poolSource: PoolSourceProperties? = null,
)

data class PoolSourceProperties(
    @field:NotEmpty
    val url: String? = null,
    @field:NotEmpty
    val username: String? = null,
    @field:NotEmpty
    val password: String? = null
)
