package dev.crashteam.repricer.repository.postgre.entity

import java.time.LocalDateTime
import java.util.*

data class KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity(
    val keAccountShopItemId: UUID,
    val keAccountShopItemCompetitorId: UUID,
    val productId: Long,
    val skuId: Long,
    val shopName: String,
    val itemName: String,
    val changeTime: LocalDateTime,
    val oldPrice: Long,
    val price: Long,
    var barcode: Long
)
