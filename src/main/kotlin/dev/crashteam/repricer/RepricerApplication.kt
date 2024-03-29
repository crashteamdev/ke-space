package dev.crashteam.repricer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class RepricerApplication

fun main(args: Array<String>) {
    runApplication<RepricerApplication>(*args)
}
