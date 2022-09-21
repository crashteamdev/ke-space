package dev.crashteam.repricer.repository.postgre.entity

import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

data class KazanExpressAccountShopItemPoolFilledEntity(
    val keAccountShopItemId: UUID,
    val keAccountId: UUID,
    val keAccountShopId: UUID,
    val productId: Long,
    val skuId: Long,
    val productSku: String,
    val price: Long,
    val purchasePrice: Long,
    val externalShopId: Long,
    val discount: BigInteger? = null,
    val step: Int? = null,
    val minimumThreshold: Long? = null,
    val maximumThreshold: Long? = null,
    val skuTitle: String,
    val barcode: Long,
    val lastCheck: LocalDateTime? = null
)
