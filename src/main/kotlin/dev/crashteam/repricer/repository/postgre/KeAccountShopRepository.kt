package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.db.model.tables.Account.ACCOUNT
import dev.crashteam.repricer.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.repricer.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.repricer.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.repricer.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.repricer.db.model.tables.KeAccountShopItemPool.KE_ACCOUNT_SHOP_ITEM_POOL
import dev.crashteam.repricer.db.model.tables.Subscription.SUBSCRIPTION
import dev.crashteam.repricer.db.model.tables.records.KeAccountShopRecord
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntityWithData
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountShopEntityDataMapper
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountShopEntityMapper
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.util.*
import org.jooq.*
import org.jooq.impl.*


@Repository
class KeAccountShopRepository(
    private val dsl: DSLContext,
    private val recordToKazanExpressAccountShopEntityMapper: RecordToKazanExpressAccountShopEntityMapper,
    private val recordToKazanExpressAccountShopEntityDataMapper: RecordToKazanExpressAccountShopEntityDataMapper
) {

    fun save(keAccountShopEntity: KazanExpressAccountShopEntity): UUID? {
        val s = KE_ACCOUNT_SHOP
        return dsl.insertInto(
            s,
            s.ID,
            s.KE_ACCOUNT_ID,
            s.EXTERNAL_SHOP_ID,
            s.NAME,
            s.SKU_TITLE
        ).values(
            keAccountShopEntity.id,
            keAccountShopEntity.keAccountId,
            keAccountShopEntity.externalShopId,
            keAccountShopEntity.name,
            keAccountShopEntity.skuTitle
        ).onDuplicateKeyUpdate()
            .set(
                mapOf(
                    s.EXTERNAL_SHOP_ID to keAccountShopEntity.externalShopId,
                    s.NAME to keAccountShopEntity.name,
                    s.SKU_TITLE to keAccountShopEntity.skuTitle
                )
            )
            .returningResult(s.ID)
            .fetchOne()!!.getValue(s.ID)
    }

    fun saveBatch(keAccountShopEntity: List<KazanExpressAccountShopEntity>) {
        val records = keAccountShopEntity.map {
            KeAccountShopRecord(
                it.id,
                it.keAccountId,
                it.externalShopId,
                it.name,
                it.skuTitle
            )
        }
        dsl.batchInsert(records).execute()
    }

    fun getKeAccountShops(userId: String, keAccountId: UUID): List<KazanExpressAccountShopEntity> {
        val s = KE_ACCOUNT_SHOP
        val k = KE_ACCOUNT
        val a = ACCOUNT
        val records = dsl.select()
            .from(s)
            .innerJoin(k).on(k.ID.eq(s.KE_ACCOUNT_ID))
            .innerJoin(a).on(k.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId).and(s.KE_ACCOUNT_ID.eq(keAccountId)))
            .fetch()
        return records.map { recordToKazanExpressAccountShopEntityMapper.convert(it) }.toList()
    }

    fun getKeAccountShopsWithData(userId: String, keAccountId: UUID): List<KazanExpressAccountShopEntityWithData> {
        val s = KE_ACCOUNT_SHOP
        val k = KE_ACCOUNT
        val p = KE_ACCOUNT_SHOP_ITEM_POOL
        val i = KE_ACCOUNT_SHOP_ITEM
        val a = ACCOUNT

        val productCount: Field<Int> = field(
            select(countDistinct(i.PRODUCT_ID))
                .from(i)
                .where(
                    i.KE_ACCOUNT_SHOP_ID.eq(KE_ACCOUNT_SHOP.ID)
                        .and(i.KE_ACCOUNT_ID.eq(KE_ACCOUNT.ID))
                )
        ).`as`("product_count")

        val skuCount = field(
            select(countDistinct(i.SKU_ID))
                .from(i)
                .where(
                    i.KE_ACCOUNT_SHOP_ID.eq(KE_ACCOUNT_SHOP.ID)
                        .and(i.KE_ACCOUNT_ID.eq(KE_ACCOUNT.ID))
                )
        ).`as`("sku_count")

        val poolCount = field(
            select(countDistinct(p.KE_ACCOUNT_SHOP_ITEM_ID))
                .from(p)
                .join(i)
                .on(i.ID.eq(p.KE_ACCOUNT_SHOP_ITEM_ID))
                .where(
                    i.KE_ACCOUNT_SHOP_ID.eq(KE_ACCOUNT_SHOP.ID)
                        .and(i.KE_ACCOUNT_ID.eq(KE_ACCOUNT.ID))
                )
        ).`as`("pool_count")

        val records = dsl.select(
            KE_ACCOUNT_SHOP.ID,
            KE_ACCOUNT_SHOP.KE_ACCOUNT_ID,
            KE_ACCOUNT_SHOP.EXTERNAL_SHOP_ID,
            KE_ACCOUNT_SHOP.NAME,
            KE_ACCOUNT_SHOP.SKU_TITLE,
            productCount,
            skuCount,
            poolCount
        ).from(s)
            .innerJoin(k).on(k.ID.eq(s.KE_ACCOUNT_ID))
            .innerJoin(a).on(k.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId).and(s.KE_ACCOUNT_ID.eq(keAccountId)))
            .fetch()

        return records.map { recordToKazanExpressAccountShopEntityDataMapper.convert(it) }.toList()
    }

    fun countKeAccountShopItemsInPool(userId: String): Int {
        val p = KE_ACCOUNT_SHOP_ITEM_POOL
        val a = ACCOUNT
        val i = KE_ACCOUNT_SHOP_ITEM
        val ka = KE_ACCOUNT

        return dsl.select(countDistinct(p.KE_ACCOUNT_SHOP_ITEM_ID))
            .from(p)
            .innerJoin(i).on(i.ID.eq(p.KE_ACCOUNT_SHOP_ITEM_ID))
            .innerJoin(ka).on(ka.ID.eq(i.KE_ACCOUNT_ID))
            .innerJoin(a).on(ka.ACCOUNT_ID.eq(a.ID))
            .where(a.USER_ID.eq(userId)).fetchOne(0, Int::class.java) ?: 0
    }

    fun countAccounts(userId: String): Int {
        val a = ACCOUNT
        val ka = KE_ACCOUNT
        return dsl.select(countDistinct(ka.ACCOUNT_ID))
            .from(ka)
            .innerJoin(a).on(a.ID.eq(ka.ACCOUNT_ID))
            .where(a.USER_ID.eq(userId)).fetchOne(0, Int::class.java) ?: 0
    }

    fun countCompetitors(userId: String): Int {
        val c = KE_ACCOUNT_SHOP_ITEM_COMPETITOR
        val si = KE_ACCOUNT_SHOP_ITEM
        val ka = KE_ACCOUNT
        val a = ACCOUNT

        return dsl.select(countDistinct(c.ID))
            .from(c)
            .innerJoin(si).on(si.ID.eq(c.KE_ACCOUNT_SHOP_ITEM_ID))
            .innerJoin(ka).on(ka.ID.eq(si.KE_ACCOUNT_ID))
            .innerJoin(a).on(a.ID.eq(ka.ACCOUNT_ID))
            .where(a.USER_ID.eq(userId)).fetchOne(0, Int::class.java) ?: 0
    }

    fun getKeAccountShops(keAccountId: UUID): List<KazanExpressAccountShopEntity> {
        val s = KE_ACCOUNT_SHOP
        val k = KE_ACCOUNT
        val records = dsl.select()
            .from(s)
            .innerJoin(k).on(k.ID.eq(s.KE_ACCOUNT_ID))
            .where(s.KE_ACCOUNT_ID.eq(keAccountId))
            .fetch()
        return records.map { recordToKazanExpressAccountShopEntityMapper.convert(it) }.toList()
    }

    fun getKeAccountShopByShopId(keAccountId: UUID, shopId: Long): KazanExpressAccountShopEntity? {
        val s = KE_ACCOUNT_SHOP
        val k = KE_ACCOUNT
        val record = dsl.select(*s.fields())
            .from(s)
            .join(k).on(s.KE_ACCOUNT_ID.eq(k.ID))
            .where(k.ID.eq(keAccountId).and(s.EXTERNAL_SHOP_ID.eq(shopId)))
            .fetchOne() ?: return null
        return recordToKazanExpressAccountShopEntityMapper.convert(record)
    }

    fun deleteByShopIds(shopIds: List<Long>): Int {
        val s = KE_ACCOUNT_SHOP
        return dsl.deleteFrom(s)
            .where(s.EXTERNAL_SHOP_ID.`in`(shopIds))
            .execute()
    }

}
