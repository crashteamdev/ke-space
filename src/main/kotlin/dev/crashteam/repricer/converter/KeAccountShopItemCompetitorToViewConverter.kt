package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountCompetitorShopItem
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class KeAccountShopItemCompetitorToViewConverter :
    DataConverter<KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity, KeAccountCompetitorShopItem> {
    override fun convert(source: KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity): KeAccountCompetitorShopItem {
        return KeAccountCompetitorShopItem().apply {
            this.name = source.name
            this.productId = source.productId
            this.skuId = source.skuId
            this.price = BigDecimal.valueOf(source.price, 2).toDouble()
            this.availableAmount = source.availableAmount
        }
    }
}
