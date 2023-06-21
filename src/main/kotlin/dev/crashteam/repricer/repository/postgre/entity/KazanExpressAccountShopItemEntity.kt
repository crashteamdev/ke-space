package dev.crashteam.repricer.repository.postgre.entity

import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

data class KazanExpressAccountShopItemEntity(
    val id: UUID,
    val keAccountId: UUID,
    val keAccountShopId: UUID,
    val categoryId: Long,
    val productId: Long,
    val skuId: Long,
    val name: String,
    val photoKey: String,
    val purchasePrice: Long?,
    val price: Long,
    val barCode: Long,
    val productSku: String,
    val skuTitle: String,
    val availableAmount: Long,
    val minimumThreshold: Long? = null,
    val maximumThreshold: Long? = null,
    val step: Int? = null,
    val discount: BigInteger? = null,
    val lastUpdate: LocalDateTime,
    val isInPool: Boolean? = false,
    val strategyId: Long?
)
