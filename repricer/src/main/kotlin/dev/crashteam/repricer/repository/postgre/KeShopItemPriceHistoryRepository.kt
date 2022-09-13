package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.repricer.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.repricer.db.model.tables.KeAccountShopItemPriceHistory.KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
import dev.crashteam.repricer.extensions.paginate
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemPriceHistoryEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity
import dev.crashteam.repricer.repository.postgre.entity.PaginateEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountShopItemPriceHistoryEntityJoinShopItemAndShopMapper
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountShopItemPriceHistoryEntityMapper
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class KeShopItemPriceHistoryRepository(
    private val dsl: DSLContext,
    private val recordMapper: RecordToKazanExpressAccountShopItemPriceHistoryEntityMapper,
    private val recordPriceHistoryShopItemShopMapper: RecordToKazanExpressAccountShopItemPriceHistoryEntityJoinShopItemAndShopMapper
) {

    fun save(keShopItemPriceHistoryEntity: KazanExpressShopItemPriceHistoryEntity): Int {
        val s = KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
        return dsl.insertInto(
            s,
            s.KE_ACCOUNT_SHOP_ITEM_ID,
            s.KE_ACCOUNT_SHOP_ITEM_COMPETITOR_ID,
            s.CHANGE_TIME,
            s.OLD_PRICE,
            s.PRICE
        )
            .values(
                keShopItemPriceHistoryEntity.keAccountShopItemId,
                keShopItemPriceHistoryEntity.keAccountShopItemCompetitorId,
                keShopItemPriceHistoryEntity.changeTime,
                keShopItemPriceHistoryEntity.oldPrice,
                keShopItemPriceHistoryEntity.price
            ).execute()
    }

    fun findHistoryByShopItemId(
        shopItemId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<KazanExpressShopItemPriceHistoryEntityJointItemAndShopEntity>> {
        val i = KE_ACCOUNT_SHOP_ITEM
        val s = KE_ACCOUNT_SHOP
        val p = KE_ACCOUNT_SHOP_ITEM_PRICE_HISTORY
        var select = dsl.select(
            p.KE_ACCOUNT_SHOP_ITEM_ID,
            p.KE_ACCOUNT_SHOP_ITEM_COMPETITOR_ID,
            p.OLD_PRICE,
            p.PRICE,
            p.CHANGE_TIME,
            i.PRODUCT_ID,
            i.SKU_ID,
            i.NAME.`as`("item_name"),
            i.BARCODE,
            s.NAME.`as`("shop_name"),
        )
            .from(p)
            .join(i).on(p.KE_ACCOUNT_SHOP_ITEM_ID.eq(i.ID))
            .join(s).on(i.KE_ACCOUNT_SHOP_ID.eq(s.ID))
            .where(p.KE_ACCOUNT_SHOP_ITEM_ID.eq(shopItemId))
        if (filter != null) {
            select = select.and(filter)
        }
        val sortFields = sortFields ?: listOf(p.CHANGE_TIME to SortType.ASC)
        val records = dsl.paginate(select, sortFields, limit, offset).fetch()

        return records.map {
            PaginateEntity(
                item = recordPriceHistoryShopItemShopMapper.convert(it),
                limit = limit,
                offset = offset,
                total = it.get("total_rows", Long::class.java),
                row = it.get("row", Long::class.java)
            )
        }
    }

}
