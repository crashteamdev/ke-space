package dev.crashteam.repricer.config

import dev.crashteam.repricer.config.properties.DataSourceProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DataSourceConfig(
    private val dataSourceProperties: DataSourceProperties,
) {

    @Bean
    fun poolDataSource(): DataSource {
        val dataSourceBuilder = DataSourceBuilder.create()
        dataSourceBuilder.driverClassName("org.postgresql.Driver")
        dataSourceBuilder.url(dataSourceProperties.poolSource!!.url)
        dataSourceBuilder.username(dataSourceProperties.poolSource!!.username)
        dataSourceBuilder.password(dataSourceProperties.poolSource!!.password)
        return dataSourceBuilder.build()
    }
}
