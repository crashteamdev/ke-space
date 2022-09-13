package dev.crashteam.repricer.repository.postgre.entity

import java.time.LocalDateTime
import java.util.*

data class KazanExpressShopItemPriceHistoryEntity(
    val keAccountShopItemId: UUID,
    val keAccountShopItemCompetitorId: UUID,
    val changeTime: LocalDateTime,
    val oldPrice: Long,
    val price: Long
)
