package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeShopItem.KE_SHOP_ITEM
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressShopItemMapper : RecordMapper<KazanExpressShopItemEntity> {

    override fun convert(record: Record): KazanExpressShopItemEntity {
        return KazanExpressShopItemEntity(
            productId = record.getValue(KE_SHOP_ITEM.PRODUCT_ID),
            skuId = record.getValue(KE_SHOP_ITEM.SKU_ID),
            categoryId = record.getValue(KE_SHOP_ITEM.CATEGORY_ID),
            name = record.getValue(KE_SHOP_ITEM.NAME),
            photoKey = record.getValue(KE_SHOP_ITEM.PHOTO_KEY),
            avgHashFingerprint = record.getValue(KE_SHOP_ITEM.AVG_HASH_FINGERPRINT),
            pHashFingerprint = record.getValue(KE_SHOP_ITEM.P_HASH_FINGERPRINT),
            price = record.getValue(KE_SHOP_ITEM.PRICE),
            lastUpdate = record.getValue(KE_SHOP_ITEM.LAST_UPDATE),
            availableAmount = record.getValue(KE_SHOP_ITEM.AVAILABLE_AMOUNT)
        )
    }
}
