package dev.crashteam.repricer.filter

import dev.crashteam.repricer.controller.*
import dev.crashteam.repricer.db.model.tables.KeAccountShopItem
import dev.crashteam.repricer.db.model.tables.records.KeAccountShopItemRecord

class KeAccountShopItemFilterRecordMapper : FilterRecordMapper {
    override fun recordMapper(): Map<String, ViewFieldToTableFieldMapper<KeAccountShopItemRecord, out Comparable<*>>> {
        return mapOf(
            "productId" to LongTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            "skuId" to LongTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.SKU_ID),
            "skuTitle" to StringTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.SKU_TITLE),
            "name" to StringTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.NAME),
            "photoKey" to StringTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
            "purchasePrice" to LongTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.PURCHASE_PRICE),
            "price" to LongTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.PRICE),
            "barCode" to LongTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.BARCODE),
            "availableAmount" to LongTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
            "minimumThreshold" to LongTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
            "maximumThreshold" to LongTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
            "step" to IntegerTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.STEP),
            "discount" to BigIntegerTableFieldMapper(KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM.DISCOUNT)
        )
    }
}
