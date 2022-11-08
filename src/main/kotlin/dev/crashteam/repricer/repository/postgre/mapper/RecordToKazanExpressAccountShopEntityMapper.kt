package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressAccountShopEntityMapper : RecordMapper<KazanExpressAccountShopEntity> {

    override fun convert(record: Record): KazanExpressAccountShopEntity {
        return KazanExpressAccountShopEntity(
            id = record.getValue(KE_ACCOUNT_SHOP.ID),
            keAccountId = record.getValue(KE_ACCOUNT_SHOP.KE_ACCOUNT_ID),
            externalShopId = record.getValue(KE_ACCOUNT_SHOP.EXTERNAL_SHOP_ID),
            name = record.getValue(KE_ACCOUNT_SHOP.NAME),
            skuTitle = record.getValue(KE_ACCOUNT_SHOP.SKU_TITLE)
        )
    }
}
