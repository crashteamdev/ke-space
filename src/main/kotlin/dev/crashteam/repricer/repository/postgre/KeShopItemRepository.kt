package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.tables.KeShopItem.KE_SHOP_ITEM
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressShopItemMapper
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class KeShopItemRepository(
    private val dsl: DSLContext,
    private val recordToKazanExpressShopItemMapper: RecordToKazanExpressShopItemMapper
) {

    fun save(keShopItem: KazanExpressShopItemEntity): Int {
        val s = KE_SHOP_ITEM
        return dsl.insertInto(
            s,
            s.PRODUCT_ID,
            s.SKU_ID,
            s.CATEGORY_ID,
            s.NAME,
            s.PHOTO_KEY,
            s.AVG_HASH_FINGERPRINT,
            s.P_HASH_FINGERPRINT,
            s.PRICE,
            s.LAST_UPDATE,
            s.AVAILABLE_AMOUNT
        )
            .values(
                keShopItem.productId,
                keShopItem.skuId,
                keShopItem.categoryId,
                keShopItem.name,
                keShopItem.photoKey,
                keShopItem.avgHashFingerprint,
                keShopItem.pHashFingerprint,
                keShopItem.price,
                keShopItem.lastUpdate,
                keShopItem.availableAmount
            )
            .onDuplicateKeyUpdate()
            .set(
                mapOf(
                    s.CATEGORY_ID to keShopItem.categoryId,
                    s.NAME to keShopItem.name,
                    s.PHOTO_KEY to keShopItem.photoKey,
                    s.AVG_HASH_FINGERPRINT to keShopItem.avgHashFingerprint,
                    s.P_HASH_FINGERPRINT to keShopItem.pHashFingerprint,
                    s.PRICE to keShopItem.price,
                    s.LAST_UPDATE to keShopItem.lastUpdate,
                    s.AVAILABLE_AMOUNT to keShopItem.availableAmount
                )
            ).execute()
    }

    fun saveBatch(keShopItems: List<KazanExpressShopItemEntity>): IntArray {
        val s = KE_SHOP_ITEM
        return dsl.batch(
            keShopItems.map { keShopItem ->
                dsl.insertInto(
                    s,
                    s.PRODUCT_ID,
                    s.SKU_ID,
                    s.CATEGORY_ID,
                    s.NAME,
                    s.PHOTO_KEY,
                    s.AVG_HASH_FINGERPRINT,
                    s.P_HASH_FINGERPRINT,
                    s.PRICE,
                    s.LAST_UPDATE,
                    s.AVAILABLE_AMOUNT
                )
                    .values(
                        keShopItem.productId,
                        keShopItem.skuId,
                        keShopItem.categoryId,
                        keShopItem.name,
                        keShopItem.photoKey,
                        keShopItem.avgHashFingerprint,
                        keShopItem.pHashFingerprint,
                        keShopItem.price,
                        keShopItem.lastUpdate,
                        keShopItem.availableAmount
                    )
                    .onDuplicateKeyUpdate()
                    .set(
                        mapOf(
                            s.CATEGORY_ID to keShopItem.categoryId,
                            s.NAME to keShopItem.name,
                            s.PHOTO_KEY to keShopItem.photoKey,
                            s.AVG_HASH_FINGERPRINT to keShopItem.avgHashFingerprint,
                            s.P_HASH_FINGERPRINT to keShopItem.pHashFingerprint,
                            s.PRICE to keShopItem.price,
                            s.LAST_UPDATE to keShopItem.lastUpdate,
                            s.AVAILABLE_AMOUNT to keShopItem.availableAmount
                        )
                    )
            }
        ).execute()
    }

    fun findByProductIdAndSkuId(productId: Long, skuId: Long): KazanExpressShopItemEntity? {
        val s = KE_SHOP_ITEM
        return dsl.selectFrom(s)
            .where(s.PRODUCT_ID.eq(productId).and(s.SKU_ID.eq(skuId)))
            .fetchOne()?.map { recordToKazanExpressShopItemMapper.convert(it) }
    }

    fun findSimilarItemsByNameAndHash(
        productId: Long,
        skuId: Long,
        avgHash: String? = null,
        pHash: String? = null,
        name: String
    ): List<KazanExpressShopItemEntity> {
        val s = KE_SHOP_ITEM
        val records = dsl.selectFrom(s)
            .where(
                s.AVG_HASH_FINGERPRINT.eq(avgHash).or(
                    DSL.field(
                        "similarity({0}, {1})",
                        Double::class.java, s.NAME, name
                    ).greaterThan(0.6)
                ).and(s.PRODUCT_ID.notEqual(productId).and(s.SKU_ID.notEqual(skuId)))
            ).limit(30).fetch()

        return records.map { recordToKazanExpressShopItemMapper.convert(it) }
    }

    fun findSimilarItemsByName(
        productId: Long,
        skuId: Long,
        name: String
    ): List<KazanExpressShopItemEntity> {
        val s = KE_SHOP_ITEM
        val records = dsl.selectFrom(s)
            .where(DSL.field(
                "similarity({0}, {1})",
                Double::class.java, s.NAME, name
            ).greaterThan(0.4).and(s.PRODUCT_ID.notEqual(productId).and(s.SKU_ID.notEqual(skuId)))
            ).limit(30).fetch()

        return records.map { recordToKazanExpressShopItemMapper.convert(it) }
    }

}
