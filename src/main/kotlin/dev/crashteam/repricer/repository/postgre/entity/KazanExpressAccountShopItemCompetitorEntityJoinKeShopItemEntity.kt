package dev.crashteam.repricer.repository.postgre.entity

import java.util.*

data class KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity(
    val id: UUID,
    val keAccountShopItemId: UUID,
    val productId: Long,
    val skuId: Long,
    val name: String,
    val availableAmount: Long,
    val price: Long,
    val photoKey: String,
)
