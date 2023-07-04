package dev.crashteam.repricer.repository.postgre

import dev.crashteam.openapi.kerepricer.model.EqualPriceStrategy
import dev.crashteam.openapi.kerepricer.model.QuantityDependentStrategy
import dev.crashteam.openapi.kerepricer.model.Strategy
import dev.crashteam.repricer.db.model.enums.StrategyType
import dev.crashteam.repricer.db.model.tables.StrategyOption
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class EqualPriceStrategyOptionRepository(private val dsl: DSLContext) :
    StrategyOptionRepository {
    override fun <T: Strategy> save(t: T): Long {
        val strategyOption = StrategyOption.STRATEGY_OPTION
        val strategy = t as EqualPriceStrategy
        return dsl.insertInto(
            strategyOption,
            strategyOption.MAXIMUM_THRESHOLD,
            strategyOption.MINIMUM_THRESHOLD
        ).values(
            strategy.maximumThreshold.toLong(),
            strategy.minimumThreshold.toLong()
        ).returningResult(strategyOption.ID)
            .fetchOne()!!.getValue(strategyOption.ID)
    }

    override fun <T : Strategy> update(id: Long, t: T): Int {
        val strategy = t as QuantityDependentStrategy
        val strategyOption = StrategyOption.STRATEGY_OPTION
        return dsl.update(strategyOption)
            .set(strategyOption.MAXIMUM_THRESHOLD, strategy.minimumThreshold.toLong())
            .set(strategyOption.MINIMUM_THRESHOLD, strategy.minimumThreshold.toLong())
            .where(strategyOption.ID.eq(id))
            .execute()
    }

    override fun strategyType(): StrategyType {
        return StrategyType.equal_price
    }

}