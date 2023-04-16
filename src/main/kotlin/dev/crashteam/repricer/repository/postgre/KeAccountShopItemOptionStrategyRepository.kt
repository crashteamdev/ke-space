package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.tables.KeAccountShopItemCompetitor
import dev.crashteam.repricer.db.model.tables.KeAccountShopItemStrategy.KE_ACCOUNT_SHOP_ITEM_STRATEGY
import dev.crashteam.repricer.db.model.tables.StrategyOption.STRATEGY_OPTION
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemStrategyEntity
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class KeAccountShopItemOptionStrategyRepository(
    private val dsl: DSLContext,
) {

    fun save(strategyEntity: KazanExpressAccountShopItemStrategyEntity): Int {
        val i = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        return dsl.insertInto(
            i,
            i.ID,
            i.STRATEGY_OPTION_ID,
            i.STRATEGY_NAME
        ).values(
            strategyEntity.id,
            strategyEntity.strategyOptionId,
            strategyEntity.strategyName
        ).execute()
    }

    fun findById(id: Long): KazanExpressAccountShopItemStrategyEntity? {
        val i = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        val o = STRATEGY_OPTION
        dsl.select()
            .from(i.leftJoin(o).on(i.STRATEGY_OPTION_ID.eq(o.ID)))
            .where(i.ID.eq(id))
            .fetchOne() ?: return null
        return null
    }
}