package dev.crashteam.repricer.repository.postgre.entity

import java.util.*

data class KazanExpressAccountShopItemCompetitorEntity(
    val id: UUID,
    val keAccountShopItemId: UUID,
    val productId: Long,
    val skuId: Long,
)
