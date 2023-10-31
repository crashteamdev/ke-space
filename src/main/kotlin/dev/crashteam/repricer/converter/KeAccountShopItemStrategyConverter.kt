package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.CloseToMinimalStrategy
import dev.crashteam.openapi.kerepricer.model.EqualPriceStrategy
import dev.crashteam.openapi.kerepricer.model.KeAccountShopItemStrategy
import dev.crashteam.openapi.kerepricer.model.QuantityDependentStrategy
import dev.crashteam.openapi.kerepricer.model.Strategy
import dev.crashteam.repricer.db.model.enums.StrategyType
import dev.crashteam.repricer.repository.postgre.entity.strategy.KazanExpressAccountShopItemStrategyEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class KeAccountShopItemStrategyConverter: DataConverter<KazanExpressAccountShopItemStrategyEntity, KeAccountShopItemStrategy> {
    override fun convert(source: KazanExpressAccountShopItemStrategyEntity): KeAccountShopItemStrategy {
        return KeAccountShopItemStrategy().apply {
            id = source.id
            strategy = getStrategy(source)
        }
    }

    private fun getStrategy(source: KazanExpressAccountShopItemStrategyEntity): Strategy {
        val minimumThreshold = (source.minimumThreshold?.toBigDecimal() ?: BigDecimal.ZERO).movePointLeft(2)
        val maximumThreshold = (source.maximumThreshold?.toBigDecimal() ?: BigDecimal.ZERO).movePointLeft(2)
        return when(source.strategyType) {
            StrategyType.close_to_minimal.name -> CloseToMinimalStrategy(source.step, source.strategyType,
                minimumThreshold.toDouble(), maximumThreshold.toDouble())
            StrategyType.quantity_dependent.name -> QuantityDependentStrategy(source.step, source.strategyType,
                minimumThreshold.toDouble(), maximumThreshold.toDouble())
            StrategyType.equal_price.name -> EqualPriceStrategy(source.strategyType, minimumThreshold.toDouble(),
                maximumThreshold.toDouble())
            else -> throw IllegalArgumentException("No such strategy - ${source.strategyType}")
        }
    }
}