package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.tables.Account.ACCOUNT
import dev.crashteam.repricer.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.repricer.db.model.tables.KeAccountShop.KE_ACCOUNT_SHOP
import dev.crashteam.repricer.db.model.tables.records.KeAccountShopRecord
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountShopEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class KeAccountShopRepository(
    private val dsl: DSLContext,
    private val recordToKazanExpressAccountShopEntityMapper: RecordToKazanExpressAccountShopEntityMapper
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
                    s.NAME to keAccountShopEntity.name
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
