package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountPriceChangeHistory
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity
import org.springframework.stereotype.Component

@Component
class KeAccountPriceChangeHistoryFilledToViewConverter :
    DataConverter<KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity, KeAccountPriceChangeHistory> {

    override fun convert(source: KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity): KeAccountPriceChangeHistory {
        return KeAccountPriceChangeHistory().apply {
            this.id = source.keAccountShopItemId
            this.productId = source.productId
            this.skuId = source.skuId
            this.shopName = source.shopName
            this.itemName = source.itemName
            this.oldPrice = source.oldPrice.toBigDecimal().setScale(2).toDouble()
            this.newPrice = source.price.toBigDecimal().setScale(2).toDouble()
            this.barcode = source.barcode
        }
    }
}
