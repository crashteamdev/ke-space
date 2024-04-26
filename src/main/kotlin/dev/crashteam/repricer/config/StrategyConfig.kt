package dev.crashteam.repricer.config

import dev.crashteam.repricer.db.model.enums.StrategyType
import dev.crashteam.repricer.price.CloseToMinimalPriceChangeCalculatorStrategy
import dev.crashteam.repricer.price.EqualPriceChangeCalculatorStrategy
import dev.crashteam.repricer.price.PriceChangeCalculatorStrategy
import dev.crashteam.repricer.price.QuantityDependentPriceChangeCalculatorStrategy
import dev.crashteam.repricer.repository.postgre.EqualPriceStrategyOptionRepository
import dev.crashteam.repricer.repository.postgre.CloseToMinimalStrategyOptionRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.QuantityDependentStrategyOptionRepository
import dev.crashteam.repricer.repository.postgre.StrategyOptionRepository
import dev.crashteam.repricer.service.AnalyticsService
import dev.crashteam.repricer.service.KeShopItemService
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StrategyConfig {

    @Bean
    fun strategies(dsl: DSLContext): Map<StrategyType, StrategyOptionRepository> {
        return mapOf(
            StrategyType.close_to_minimal to CloseToMinimalStrategyOptionRepository(dsl),
            StrategyType.quantity_dependent to QuantityDependentStrategyOptionRepository(dsl),
            StrategyType.equal_price to EqualPriceStrategyOptionRepository(dsl)
        )
    }

    @Bean
    fun calculators(
        competitorRepository: KeAccountShopItemCompetitorRepository,
        keShopItemService: KeShopItemService,
        analyticsService: AnalyticsService
    ): Map<StrategyType, PriceChangeCalculatorStrategy> {
        return mapOf(
            StrategyType.close_to_minimal to CloseToMinimalPriceChangeCalculatorStrategy(
                competitorRepository,
                keShopItemService,
                analyticsService
            ),
            StrategyType.quantity_dependent to QuantityDependentPriceChangeCalculatorStrategy(
                competitorRepository,
                keShopItemService,
                analyticsService
            ),
            StrategyType.equal_price to EqualPriceChangeCalculatorStrategy(
                competitorRepository,
                keShopItemService,
                analyticsService
            )
        )
    }
}