package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.repricer.db.model.tables.KeShopItem.KE_SHOP_ITEM
import dev.crashteam.repricer.extensions.paginate
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity
import dev.crashteam.repricer.repository.postgre.entity.PaginateEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntityMapper
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountShopItemCompetitorEntityMapper
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class KeAccountShopItemCompetitorRepository(
    private val dsl: DSLContext,
    private val recordToKazanExpressAccountShopItemCompetitorMapper: RecordToKazanExpressAccountShopItemCompetitorEntityMapper,
    private val recordToKazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntityMapper: RecordToKazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntityMapper
) {

    fun save(competitorEntity: KazanExpressAccountShopItemCompetitorEntity): Int {
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        return dsl.insertInto(
            c,
            c.ID,
            c.KE_ACCOUNT_SHOP_ITEM_ID,
            c.PRODUCT_ID,
            c.SKU_ID
        ).values(
            competitorEntity.id,
            competitorEntity.keAccountShopItemId,
            competitorEntity.productId,
            competitorEntity.skuId
        ).execute()
    }

    fun findShopItemCompetitors(
        keAccountShopItemId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long,
    ): List<PaginateEntity<KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity>> {
        val s = KE_SHOP_ITEM
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        var select = dsl.select(
            s.PRODUCT_ID,
            s.SKU_ID,
            s.CATEGORY_ID,
            s.NAME,
            s.AVAILABLE_AMOUNT,
            s.PRICE,
            s.PHOTO_KEY,
            c.ID,
            c.KE_ACCOUNT_SHOP_ITEM_ID,
        )
            .from(c.join(s).on(s.PRODUCT_ID.eq(c.PRODUCT_ID).and(s.SKU_ID.eq(c.SKU_ID))))
            .where(c.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId))
        if (filter != null) {
            select = select.and(filter)
        }
        val sortFields = sortFields ?: listOf(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID to SortType.ASC)
        val records = dsl.paginate(select, sortFields, limit, offset).fetch()
        return records.map {
            PaginateEntity(
                item = recordToKazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntityMapper.convert(it),
                limit = limit,
                offset = offset,
                total = it.get("total_rows", Long::class.java),
                row = it.get("row", Long::class.java)
            )
        }
    }

    fun findShopItemCompetitors(
        keAccountShopItemId: UUID,
    ): List<KazanExpressAccountShopItemCompetitorEntity> {
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        val records = dsl.selectFrom(c)
            .where(c.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId))
            .fetch()

        return records.map { recordToKazanExpressAccountShopItemCompetitorMapper.convert(it) }
    }

    fun findShopItemCompetitorForUpdate(
        keAccountShopItemId: UUID,
        productId: Long,
        skuId: Long,
    ): KazanExpressAccountShopItemCompetitorEntity? {
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        return dsl.selectFrom(c)
            .where(
                c.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId)
                    .and(c.PRODUCT_ID.eq(productId).and(c.SKU_ID.eq(skuId)))
            )
            .fetchOne()
            ?.map { recordToKazanExpressAccountShopItemCompetitorMapper.convert(it) }
    }

    fun findShopItemCompetitorsCount(
        keAccountShopItemId: UUID
    ): Int {
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        return dsl.selectCount()
            .from(KE_ACCOUNT_SHOP_ITEM_COMPETITOR)
            .where(c.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId))
            .fetchOne(0, Int::class.java) ?: 0
    }

    fun findShopItemCompetitorsWithData(
        keAccountShopItemId: UUID,
    ): List<KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity> {
        val s = KE_SHOP_ITEM
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        val records = dsl.selectFrom(c.join(s).on(s.PRODUCT_ID.eq(c.PRODUCT_ID).and(s.SKU_ID.eq(c.SKU_ID))))
            .where(c.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId))
            .fetch()

        return records.map { recordToKazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntityMapper.convert(it) }
    }

    fun removeShopItemCompetitor(
        keAccountShopItemId: UUID,
        competitorId: UUID
    ): Int {
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        return dsl.deleteFrom(c)
            .where(c.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId).and(c.ID.eq(competitorId)))
            .execute()
    }

}
