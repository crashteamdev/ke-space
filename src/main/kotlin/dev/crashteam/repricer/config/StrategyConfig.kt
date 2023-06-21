package dev.crashteam.repricer.config

import dev.crashteam.repricer.db.model.enums.StrategyType
import dev.crashteam.repricer.repository.postgre.CloseToMinimalStrategyOptionRepository
import dev.crashteam.repricer.repository.postgre.QuantityDependentStrategyOptionRepository
import dev.crashteam.repricer.repository.postgre.StrategyOptionRepository
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StrategyConfig {

    @Bean
    fun strategies(dsl: DSLContext): Map<StrategyType, StrategyOptionRepository> {
        return mapOf(
            StrategyType.close_to_minimal to CloseToMinimalStrategyOptionRepository(dsl),
            StrategyType.quantity_dependent to QuantityDependentStrategyOptionRepository(dsl)
        )
    }
}