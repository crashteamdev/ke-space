package dev.crashteam.repricer.repository.postgre

import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.openapi.kerepricer.model.Strategy
import dev.crashteam.repricer.db.model.enums.StrategyType
import dev.crashteam.repricer.db.model.tables.KeAccountShopItemStrategy.KE_ACCOUNT_SHOP_ITEM_STRATEGY
import dev.crashteam.repricer.db.model.tables.StrategyOption.STRATEGY_OPTION
import dev.crashteam.repricer.repository.postgre.entity.strategy.KazanExpressAccountShopItemStrategyEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKeAccountShopItemStrategyEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class KeAccountShopItemStrategyRepository(
    private val dsl: DSLContext,
    private val strategiesMap: Map<StrategyType, StrategyOptionRepository>,
    private val strategyMapper: RecordToKeAccountShopItemStrategyEntityMapper
) {

    fun save(strategyRequest: AddStrategyRequest): Long {
        val itemStrategy = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        val strategyType = StrategyType.valueOf(strategyRequest.strategy.strategyType)
        val strategyId = dsl.insertInto(
            itemStrategy,
            itemStrategy.STRATEGY_TYPE,
            itemStrategy.KE_ACCOUNT_SHOP_ITEM_ID
        ).values(
            strategyType,
            strategyRequest.keAccountShopItemId
        ).returningResult(itemStrategy.ID)
            .fetchOne()!!
            .getValue(itemStrategy.ID)
        saveOption(strategyId, strategyRequest.strategy)
        return strategyId
    }

    fun update(id: UUID, patchStrategy: PatchStrategy): Long {
        val strategyType = StrategyType.valueOf(patchStrategy.strategy.strategyType)
        val itemStrategy = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        val o = STRATEGY_OPTION

        dsl.update(itemStrategy)
            .set(itemStrategy.STRATEGY_TYPE, strategyType)
            .where(itemStrategy.KE_ACCOUNT_SHOP_ITEM_ID.eq(id))
            .returningResult(itemStrategy.ID)
            .execute()

        val strategyOptionId = dsl.select()
            .from(itemStrategy.innerJoin(o).on(itemStrategy.ID.eq(o.KE_ACCOUNT_SHOP_ITEM_STRATEGY_ID)))
            .fetchOne()!!.getValue(o.ID)
        updateOption(strategyOptionId, patchStrategy.strategy)
        return strategyOptionId
    }

    fun saveOption(id: Long, strategy: Strategy): Long {
        val strategyEntityType = StrategyType.valueOf(strategy.strategyType)
        return strategiesMap[strategyEntityType]!!.save(id, strategy)
    }

    fun updateOption(strategyOptionId: Long, strategy: Strategy): Int {
        val strategyEntityType = StrategyType.valueOf(strategy.strategyType)
        return strategiesMap[strategyEntityType]!!.update(strategyOptionId, strategy)
    }

    fun findById(keAccountShopItemId: UUID): KazanExpressAccountShopItemStrategyEntity? {
        val i = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        val o = STRATEGY_OPTION
        return dsl.select()
            .from(i.leftJoin(o).on(i.ID.eq(o.KE_ACCOUNT_SHOP_ITEM_STRATEGY_ID)))
            .where(i.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId))
            .fetchOne()?.map { strategyMapper.convert(it) }
    }

    fun deleteById(keAccountShopItemId: UUID): Int {
        val i = KE_ACCOUNT_SHOP_ITEM_STRATEGY
        return dsl.deleteFrom(i).where(i.KE_ACCOUNT_SHOP_ITEM_ID.eq(keAccountShopItemId)).execute();
    }
}