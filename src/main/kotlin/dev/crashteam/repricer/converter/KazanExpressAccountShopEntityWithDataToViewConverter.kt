package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountShop
import dev.crashteam.openapi.kerepricer.model.ShopData
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntityWithData
import org.springframework.stereotype.Component

@Component
class KazanExpressAccountShopEntityWithDataToViewConverter : DataConverter<KazanExpressAccountShopEntityWithData, KeAccountShop> {

    override fun convert(source: KazanExpressAccountShopEntityWithData): KeAccountShop {
        return KeAccountShop().apply {
            id = source.id
            name = source.name
            skuTitle = source.skuTitle
            shopData = ShopData().apply {
                poolItems = source.keAccountShopData?.countPoolItems?.toBigDecimal()
                products = source.keAccountShopData?.countProducts?.toBigDecimal()
                skus = source.keAccountShopData?.countSkus?.toBigDecimal()
            }
        }
    }
}
