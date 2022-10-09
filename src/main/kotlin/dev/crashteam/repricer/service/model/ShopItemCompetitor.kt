package dev.crashteam.repricer.service.model

import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemEntity

data class ShopItemCompetitor(
    val shopItemEntity: KazanExpressShopItemEntity,
    val competitorEntity: KazanExpressAccountShopItemCompetitorEntity
)
