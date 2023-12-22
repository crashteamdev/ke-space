package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountShopItem
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemEntityWithLimitData
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class KazanExpressAccountShopItemEntityWithLimitDataToViewConverter :
    DataConverter<KazanExpressAccountShopItemEntityWithLimitData, KeAccountShopItem> {

    override fun convert(source: KazanExpressAccountShopItemEntityWithLimitData): KeAccountShopItem {
        return KeAccountShopItem().apply {
            this.id = source.id
            this.productId = source.productId
            this.skuId = source.skuId
            this.skuTitle = source.skuTitle
            this.name = source.name
            this.photoKey = source.photoKey
            this.price = BigDecimal.valueOf(source.price).movePointLeft(2).toDouble()
            this.barcode = source.barCode
            this.availableAmount = source.availableAmount
            this.minimumThreshold = source.minimumThreshold?.let { BigDecimal.valueOf(it).movePointLeft(2).toDouble() }
            this.maximumThreshold = source.maximumThreshold?.let { BigDecimal.valueOf(it).movePointLeft(2).toDouble() }
            this.step = source.step
            this.discount = source.discount?.toBigDecimal()
            this.isInPool = source.isInPool
            this.competitorsCurrent = source.competitorsCurrent
            this.availableCompetitors = source.availableCompetitors
        }
    }
}
