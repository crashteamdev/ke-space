package dev.crashteam.repricer.repository.postgre.entity

import java.util.*

data class KazanExpressAccountShopEntityWithData(
    val id: UUID? = null,
    val keAccountId: UUID,
    val externalShopId: Long,
    val name: String,
    val skuTitle: String?,
    val keAccountShopData: KeAccountShopData?
)
