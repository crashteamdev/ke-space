package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.repricer.extensions.paginate
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemEntity
import dev.crashteam.repricer.repository.postgre.entity.PaginateEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountShopItemEntityMapper
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
class KeAccountShopItemRepository(
    private val dsl: DSLContext,
    private val recordToKazanExpressAccountShopItemEntityMapper: RecordToKazanExpressAccountShopItemEntityMapper,
) {

    fun save(kazanExpressAccountShopItemEntity: KazanExpressAccountShopItemEntity): UUID? {
        val i = KE_ACCOUNT_SHOP_ITEM
        return dsl.insertInto(
            i,
            i.ID,
            i.KE_ACCOUNT_ID,
            i.KE_ACCOUNT_SHOP_ID,
            i.CATEGORY_ID,
            i.PRODUCT_ID,
            i.SKU_ID,
            i.NAME,
            i.PHOTO_KEY,
            i.PRICE,
            i.PURCHASE_PRICE,
            i.BARCODE,
            i.PRODUCT_SKU,
            i.SKU_TITLE,
            i.AVAILABLE_AMOUNT,
            i.MINIMUM_THRESHOLD,
            i.MAXIMUM_THRESHOLD,
            i.STEP,
            i.DISCOUNT,
            i.LAST_UPDATE
        ).values(
            kazanExpressAccountShopItemEntity.id,
            kazanExpressAccountShopItemEntity.keAccountId,
            kazanExpressAccountShopItemEntity.keAccountShopId,
            kazanExpressAccountShopItemEntity.categoryId,
            kazanExpressAccountShopItemEntity.productId,
            kazanExpressAccountShopItemEntity.skuId,
            kazanExpressAccountShopItemEntity.name,
            kazanExpressAccountShopItemEntity.photoKey,
            kazanExpressAccountShopItemEntity.price,
            kazanExpressAccountShopItemEntity.purchasePrice,
            kazanExpressAccountShopItemEntity.barCode,
            kazanExpressAccountShopItemEntity.productSku,
            kazanExpressAccountShopItemEntity.skuTitle,
            kazanExpressAccountShopItemEntity.availableAmount,
            kazanExpressAccountShopItemEntity.minimumThreshold,
            kazanExpressAccountShopItemEntity.maximumThreshold,
            kazanExpressAccountShopItemEntity.step,
            kazanExpressAccountShopItemEntity.discount,
            kazanExpressAccountShopItemEntity.lastUpdate
        ).onDuplicateKeyUpdate()
            .set(
                mapOf(
                    i.CATEGORY_ID to kazanExpressAccountShopItemEntity.categoryId,
                    i.PRODUCT_ID to kazanExpressAccountShopItemEntity.productId,
                    i.SKU_ID to kazanExpressAccountShopItemEntity.skuId,
                    i.NAME to kazanExpressAccountShopItemEntity.name,
                    i.PHOTO_KEY to kazanExpressAccountShopItemEntity.photoKey,
                    i.PRICE to kazanExpressAccountShopItemEntity.price,
                    i.PURCHASE_PRICE to kazanExpressAccountShopItemEntity.purchasePrice,
                    i.BARCODE to kazanExpressAccountShopItemEntity.barCode,
                    i.PRODUCT_SKU to kazanExpressAccountShopItemEntity.productSku,
                    i.SKU_TITLE to kazanExpressAccountShopItemEntity.skuTitle,
                    i.AVAILABLE_AMOUNT to kazanExpressAccountShopItemEntity.availableAmount,
                    i.LAST_UPDATE to kazanExpressAccountShopItemEntity.lastUpdate
                )
            ).returningResult(i.ID)
            .fetchOne()!!.getValue(i.ID)
    }

    fun saveBatch(kazanExpressAccountShopItemEntity: List<KazanExpressAccountShopItemEntity>): IntArray {
        val i = KE_ACCOUNT_SHOP_ITEM
        return dsl.batch(
            kazanExpressAccountShopItemEntity.map { kazanExpressAccountShopItemEntity ->
                dsl.insertInto(
                    i,
                    i.ID,
                    i.KE_ACCOUNT_ID,
                    i.KE_ACCOUNT_SHOP_ID,
                    i.CATEGORY_ID,
                    i.PRODUCT_ID,
                    i.SKU_ID,
                    i.NAME,
                    i.PHOTO_KEY,
                    i.PRICE,
                    i.PURCHASE_PRICE,
                    i.BARCODE,
                    i.PRODUCT_SKU,
                    i.SKU_TITLE,
                    i.AVAILABLE_AMOUNT,
                    i.MINIMUM_THRESHOLD,
                    i.MAXIMUM_THRESHOLD,
                    i.STEP,
                    i.DISCOUNT,
                    i.LAST_UPDATE
                ).values(
                    kazanExpressAccountShopItemEntity.id,
                    kazanExpressAccountShopItemEntity.keAccountId,
                    kazanExpressAccountShopItemEntity.keAccountShopId,
                    kazanExpressAccountShopItemEntity.categoryId,
                    kazanExpressAccountShopItemEntity.productId,
                    kazanExpressAccountShopItemEntity.skuId,
                    kazanExpressAccountShopItemEntity.name,
                    kazanExpressAccountShopItemEntity.photoKey,
                    kazanExpressAccountShopItemEntity.price,
                    kazanExpressAccountShopItemEntity.purchasePrice,
                    kazanExpressAccountShopItemEntity.barCode,
                    kazanExpressAccountShopItemEntity.productSku,
                    kazanExpressAccountShopItemEntity.skuTitle,
                    kazanExpressAccountShopItemEntity.availableAmount,
                    kazanExpressAccountShopItemEntity.minimumThreshold,
                    kazanExpressAccountShopItemEntity.maximumThreshold,
                    kazanExpressAccountShopItemEntity.step,
                    kazanExpressAccountShopItemEntity.discount,
                    kazanExpressAccountShopItemEntity.lastUpdate
                ).onDuplicateKeyUpdate()
                    .set(
                        mapOf(
                            i.CATEGORY_ID to kazanExpressAccountShopItemEntity.categoryId,
                            i.NAME to kazanExpressAccountShopItemEntity.name,
                            i.PHOTO_KEY to kazanExpressAccountShopItemEntity.photoKey,
                            i.PRICE to kazanExpressAccountShopItemEntity.price,
                            i.PURCHASE_PRICE to kazanExpressAccountShopItemEntity.purchasePrice,
                            i.BARCODE to kazanExpressAccountShopItemEntity.barCode,
                            i.AVAILABLE_AMOUNT to kazanExpressAccountShopItemEntity.availableAmount,
                            i.LAST_UPDATE to kazanExpressAccountShopItemEntity.lastUpdate,
                            i.PRODUCT_SKU to kazanExpressAccountShopItemEntity.productSku,
                            i.SKU_TITLE to kazanExpressAccountShopItemEntity.skuTitle
                        )
                    )
            }
        ).execute()
    }

    fun findShopItem(
        keAccountId: UUID,
        keAccountShopId: UUID,
        productId: Long,
        skuId: Long
    ): KazanExpressAccountShopItemEntity? {
        val i = KE_ACCOUNT_SHOP_ITEM
        val record = dsl.select()
            .from(i)
            .where(
                i.KE_ACCOUNT_ID.eq(keAccountId),
                i.KE_ACCOUNT_SHOP_ID.eq(keAccountShopId),
                i.PRODUCT_ID.eq(productId),
                i.SKU_ID.eq(skuId)
            ).fetchOne() ?: return null
        return recordToKazanExpressAccountShopItemEntityMapper.convert(record)
    }

    fun findShopItem(
        keAccountId: UUID,
        keAccountShopId: UUID,
        keAccountShopItemId: UUID
    ): KazanExpressAccountShopItemEntity? {
        val i = KE_ACCOUNT_SHOP_ITEM
        val record = dsl.select()
            .from(i)
            .where(
                i.KE_ACCOUNT_ID.eq(keAccountId),
                i.KE_ACCOUNT_SHOP_ID.eq(keAccountShopId),
                i.ID.eq(keAccountShopItemId),
            ).fetchOne() ?: return null
        return recordToKazanExpressAccountShopItemEntityMapper.convert(record)
    }

    fun findShopItem(
        keAccountId: UUID,
        keAccountShopItemId: UUID
    ): KazanExpressAccountShopItemEntity? {
        val i = KE_ACCOUNT_SHOP_ITEM
        val record = dsl.select()
            .from(i)
            .where(
                i.KE_ACCOUNT_ID.eq(keAccountId),
                i.ID.eq(keAccountShopItemId),
            ).fetchOne() ?: return null
        return recordToKazanExpressAccountShopItemEntityMapper.convert(record)
    }

    fun findAllItems(
        keAccountId: UUID,
        keAccountShopId: UUID
    ): MutableList<KazanExpressAccountShopItemEntity> {
        val i = KE_ACCOUNT_SHOP_ITEM
        val records = dsl.selectFrom(i)
            .where(
                i.KE_ACCOUNT_ID.eq(keAccountId),
                i.KE_ACCOUNT_SHOP_ID.eq(keAccountShopId)
            ).fetch()
        return records.map { recordToKazanExpressAccountShopItemEntityMapper.convert(it) }
    }

    fun findShopItems(
        keAccountId: UUID,
        keAccountShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long,
    ): MutableList<PaginateEntity<KazanExpressAccountShopItemEntity>> {
        val i = KE_ACCOUNT_SHOP_ITEM
        var select = dsl.selectFrom(i)
            .where(
                i.KE_ACCOUNT_ID.eq(keAccountId),
                i.KE_ACCOUNT_SHOP_ID.eq(keAccountShopId)
            )
        if (filter != null) {
            select = select.and(filter)
        }
        val sortFields = sortFields ?: listOf(KE_ACCOUNT_SHOP_ITEM.ID to SortType.ASC)
        val records = dsl.paginate(select, sortFields, limit, offset).fetch()
        val items = records.map {
            PaginateEntity(
                item = recordToKazanExpressAccountShopItemEntityMapper.convert(it),
                limit = limit,
                offset = offset,
                total = it.get("total_rows", Long::class.java),
                row = it.get("row", Long::class.java)
            )
        }
        return items
    }

    fun findShopItems(
        keAccountId: UUID,
        keAccountShopId: UUID
    ): List<KazanExpressAccountShopItemEntity> {
        val i = KE_ACCOUNT_SHOP_ITEM
        val records = dsl.selectFrom(i)
            .where(
                i.KE_ACCOUNT_ID.eq(keAccountId),
                i.KE_ACCOUNT_SHOP_ID.eq(keAccountShopId)
            )
            .fetch()

        return records.map { recordToKazanExpressAccountShopItemEntityMapper.convert(it) }
    }

    fun deleteWhereOldLastUpdate(keAccountId: UUID, keAccountShopId: UUID, lastUpdateTime: LocalDateTime): Int {
        val i = KE_ACCOUNT_SHOP_ITEM
        return dsl.deleteFrom(i)
            .where(
                i.KE_ACCOUNT_ID.eq(keAccountId)
                    .and(i.KE_ACCOUNT_SHOP_ID.eq(keAccountShopId))
                    .and(i.LAST_UPDATE.lessThan(lastUpdateTime))
            ).execute()
    }

    fun updatePriceChangeOptions(
        keAccountId: UUID,
        keAccountShopItemId: UUID,
        step: Int,
        minimumThreshold: Long,
        maximumThreshold: Long,
        discount: BigDecimal,
    ): Int {
        val i = KE_ACCOUNT_SHOP_ITEM
        return dsl.update(i)
            .set(
                mapOf(
                    i.STEP to step,
                    i.MINIMUM_THRESHOLD to minimumThreshold,
                    i.MAXIMUM_THRESHOLD to maximumThreshold,
                    i.DISCOUNT to discount,
                )
            )
            .where(i.KE_ACCOUNT_ID.eq(keAccountId).and(i.ID.eq(keAccountShopItemId)))
            .execute()
    }

}
