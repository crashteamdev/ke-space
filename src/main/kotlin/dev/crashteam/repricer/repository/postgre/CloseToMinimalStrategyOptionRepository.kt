package dev.crashteam.repricer.repository.postgre

import dev.crashteam.openapi.kerepricer.model.CloseToMinimalStrategy
import dev.crashteam.openapi.kerepricer.model.Strategy
import dev.crashteam.repricer.db.model.enums.StrategyType
import dev.crashteam.repricer.db.model.tables.StrategyOption
import org.jooq.DSLContext


class CloseToMinimalStrategyOptionRepository(private val dsl: DSLContext) :
    StrategyOptionRepository {
    override fun <T: Strategy> save(id: Long, t: T): Long {
        val strategy = t as CloseToMinimalStrategy
        val strategyOption = StrategyOption.STRATEGY_OPTION
        return dsl.insertInto(
            strategyOption,
            strategyOption.MAXIMUM_THRESHOLD,
            strategyOption.MINIMUM_THRESHOLD,
            strategyOption.STEP,
            strategyOption.DISCOUNT,
            strategyOption.KE_ACCOUNT_SHOP_ITEM_STRATEGY_ID
        ).values(
            strategy.maximumThreshold.toBigDecimal().movePointRight(2).toLong(),
            strategy.minimumThreshold.toBigDecimal().movePointRight(2).toLong(),
            strategy.step,
            strategy.discount?.intValueExact(),
            id
        ).returningResult(strategyOption.ID)
            .fetchOne()!!.getValue(strategyOption.ID)
    }

    override fun <T : Strategy> update(id: Long, t: T): Int {
        val strategy = t as CloseToMinimalStrategy
        val strategyOption = StrategyOption.STRATEGY_OPTION
        return dsl.update(strategyOption)
            .set(strategyOption.MAXIMUM_THRESHOLD, strategy.minimumThreshold.toBigDecimal().movePointRight(2).toLong())
            .set(strategyOption.MINIMUM_THRESHOLD, strategy.minimumThreshold.toBigDecimal().movePointRight(2).toLong())
            .set(strategyOption.STEP, strategy.step)
            .set(strategyOption.DISCOUNT, strategy.discount?.intValueExact())
            .where(strategyOption.ID.eq(id))
            .execute()
    }

    override fun strategyType(): StrategyType {
        return StrategyType.close_to_minimal
    }
}