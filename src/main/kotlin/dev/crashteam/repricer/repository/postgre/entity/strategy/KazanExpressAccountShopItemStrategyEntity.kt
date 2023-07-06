package dev.crashteam.repricer.repository.postgre.entity.strategy


data class KazanExpressAccountShopItemStrategyEntity(
    val id: Long,
    val strategyType: String,
    val strategyOptionId: Long,
    val minimumThreshold: Long?,
    val maximumThreshold: Long?,
    val step: Int?
)