package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntityWithData
import dev.crashteam.repricer.repository.postgre.entity.KeAccountShopData
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressAccountShopEntityDataMapper : RecordMapper<KazanExpressAccountShopEntityWithData> {

    override fun convert(record: Record): KazanExpressAccountShopEntityWithData {
        val shopData = KeAccountShopData(countPoolItems = record.getValue("pool_count") as Int,
            countProducts = record.getValue("product_count") as Int,
            countSkus = record.getValue("sku_count") as Int)
        return KazanExpressAccountShopEntityWithData(
            id = record.getValue(KE_ACCOUNT_SHOP.ID),
            keAccountId = record.getValue(KE_ACCOUNT_SHOP.KE_ACCOUNT_ID),
            externalShopId = record.getValue(KE_ACCOUNT_SHOP.EXTERNAL_SHOP_ID),
            name = record.getValue(KE_ACCOUNT_SHOP.NAME),
            skuTitle = record.get(KE_ACCOUNT_SHOP.SKU_TITLE),
            keAccountShopData = shopData
        )
    }
}
