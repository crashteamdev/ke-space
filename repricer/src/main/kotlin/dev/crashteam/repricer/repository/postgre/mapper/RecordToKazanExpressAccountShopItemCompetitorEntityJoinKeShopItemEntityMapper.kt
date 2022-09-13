package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.repricer.db.model.tables.KeShopItem.KE_SHOP_ITEM
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntityMapper :
    RecordMapper<KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity> {

    override fun convert(record: Record): KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity {
        return KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity(
            id = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.ID),
            keAccountShopItemId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.KE_ACCOUNT_SHOP_ITEM_ID),
            productId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
            skuId = record.getValue(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
            name = record.getValue(KE_SHOP_ITEM.NAME),
            availableAmount = record.getValue(KE_SHOP_ITEM.AVAILABLE_AMOUNT),
            price = record.getValue(KE_SHOP_ITEM.PRICE)
        )
    }
}
