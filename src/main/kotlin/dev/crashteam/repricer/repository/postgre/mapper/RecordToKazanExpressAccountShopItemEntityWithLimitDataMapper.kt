package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.repricer.db.model.tables.KeAccountShopItemPool
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemEntityWithLimitData
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressAccountShopItemEntityWithLimitDataMapper : RecordMapper<KazanExpressAccountShopItemEntityWithLimitData> {

    override fun convert(record: Record): KazanExpressAccountShopItemEntityWithLimitData {
        return KazanExpressAccountShopItemEntityWithLimitData(
            id = record.getValue(KE_ACCOUNT_SHOP_ITEM.ID),
            keAccountId = record.getValue(KE_ACCOUNT_SHOP_ITEM.KE_ACCOUNT_ID),
            keAccountShopId = record.getValue(KE_ACCOUNT_SHOP_ITEM.KE_ACCOUNT_SHOP_ID),
            categoryId = record.getValue(KE_ACCOUNT_SHOP_ITEM.CATEGORY_ID),
            productId = record.getValue(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            skuId = record.getValue(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
            name = record.getValue(KE_ACCOUNT_SHOP_ITEM.NAME),
            photoKey = record.getValue(KE_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
            purchasePrice = record.getValue(KE_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
            price = record.getValue(KE_ACCOUNT_SHOP_ITEM.PRICE),
            barCode = record.getValue(KE_ACCOUNT_SHOP_ITEM.BARCODE),
            productSku = record.getValue(KE_ACCOUNT_SHOP_ITEM.PRODUCT_SKU),
            skuTitle = record.getValue(KE_ACCOUNT_SHOP_ITEM.SKU_TITLE),
            availableAmount = record.getValue(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
            minimumThreshold = record.getValue(KE_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
            maximumThreshold = record.getValue(KE_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
            step = record.getValue(KE_ACCOUNT_SHOP_ITEM.STEP),
            lastUpdate = record.getValue(KE_ACCOUNT_SHOP_ITEM.LAST_UPDATE),
            discount = record.getValue(KE_ACCOUNT_SHOP_ITEM.DISCOUNT),
            isInPool = record.get(KeAccountShopItemPool.KE_ACCOUNT_SHOP_ITEM_POOL.KE_ACCOUNT_SHOP_ITEM_ID) != null,
            availableCompetitors = record.getValue("competitor_limit") as Int,
            competitorsCurrent = record.getValue("competitor_count") as Int
        )
    }
}
