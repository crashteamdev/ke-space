package dev.crashteam.repricer.repository.postgre.entity

import java.util.*

data class KazanExpressAccountShopEntity(
    val id: UUID? = null,
    val keAccountId: UUID,
    val externalShopId: Long,
    val name: String,
    val skuTitle: String
)
