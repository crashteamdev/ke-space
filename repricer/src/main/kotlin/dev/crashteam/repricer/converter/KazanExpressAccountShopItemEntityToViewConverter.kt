package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountShopItem
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class KazanExpressAccountShopItemEntityToViewConverter :
    DataConverter<KazanExpressAccountShopItemEntity, KeAccountShopItem> {

    override fun convert(source: KazanExpressAccountShopItemEntity): KeAccountShopItem {
        return KeAccountShopItem().apply {
            this.id = source.id
            this.productId = source.productId
            this.skuId = source.skuId
            this.name = source.name
            this.photoKey = source.photoKey
            this.fullPrice = BigDecimal.valueOf(source.fullPrice).movePointLeft(2).toDouble()
            this.sellPrice = BigDecimal.valueOf(source.sellPrice).movePointLeft(2).toDouble()
            this.barcode = source.barCode
            this.availableAmount = source.availableAmount
            this.minimumThreshold = source.minimumThreshold?.let { BigDecimal.valueOf(it).movePointLeft(2).toDouble() }
            this.maximumThreshold = source.maximumThreshold?.let { BigDecimal.valueOf(it).movePointLeft(2).toDouble() }
            this.step = source.step
        }
    }
}
