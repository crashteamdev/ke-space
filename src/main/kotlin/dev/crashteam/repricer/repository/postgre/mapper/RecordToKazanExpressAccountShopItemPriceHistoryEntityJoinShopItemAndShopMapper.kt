package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.repricer.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.repricer.db.model.tables.KeAccountShopItemPriceHistory.KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressAccountShopItemPriceHistoryEntityJoinShopItemAndShopMapper :
    RecordMapper<KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity> {

    override fun convert(record: Record): KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity {
        return KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity(
            keAccountShopItemId = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.KE_ACCOUNT_SHOP_ITEM_ID),
            keAccountShopItemCompetitorId = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.KE_ACCOUNT_SHOP_ITEM_COMPETITOR_ID),
            productId = record.getValue(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            skuId = record.getValue(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
            shopName = record.getValue(KE_ACCOUNT_SHOP.NAME.`as`("shop_name")),
            itemName = record.getValue(KE_ACCOUNT_SHOP_ITEM.NAME.`as`("item_name")),
            changeTime = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.CHANGE_TIME),
            oldPrice = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.OLD_PRICE),
            price = record.getValue(KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY.PRICE),
            barcode = record.getValue(KE_ACCOUNT_SHOP_ITEM.BARCODE)
        )
    }
}
