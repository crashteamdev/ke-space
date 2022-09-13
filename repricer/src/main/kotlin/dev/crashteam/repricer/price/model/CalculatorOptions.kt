package dev.crashteam.repricer.price.model

data class CalculatorOptions(
    val step: Int? = null,
    val minimumThreshold: Long? = null,
    val maximumThreshold: Long? = null,
)
