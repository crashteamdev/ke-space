package dev.crashteam.repricer.repository.postgre.entity

data class StrategyOptionEntity(
    val id: Long,
    val minimumThreshold: Long? = null,
    val maximumThreshold: Long? = null
)