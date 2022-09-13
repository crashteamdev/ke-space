package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressAccountShopItemCompetitorEntityMapper :
    RecordMapper<KazanExpressAccountShopItemCompetitorEntity> {

    override fun convert(record: Record): KazanExpressAccountShopItemCompetitorEntity {
        return KazanExpressAccountShopItemCompetitorEntity(
            id = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.ID),
            keAccountShopItemId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.KE_ACCOUNT_SHOP_ITEM_ID),
            productId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
            skuId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
        )
    }
}
