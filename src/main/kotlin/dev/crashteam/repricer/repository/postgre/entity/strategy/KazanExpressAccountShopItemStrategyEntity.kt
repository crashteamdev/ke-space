package dev.crashteam.repricer.repository.postgre.entity.strategy

import java.util.UUID


data class KazanExpressAccountShopItemStrategyEntity(
    val id: Long,
    val strategyType: String,
    val strategyOptionId: Long,
    val minimumThreshold: Long?,
    val maximumThreshold: Long?,
    val step: Int?,
    val discount: Int?,
    val keAccountShopItemId: UUID
)