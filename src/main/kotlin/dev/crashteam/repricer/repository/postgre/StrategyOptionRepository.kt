package dev.crashteam.repricer.repository.postgre

import dev.crashteam.openapi.kerepricer.model.Strategy
import dev.crashteam.repricer.db.model.enums.StrategyType
import java.util.UUID

interface StrategyOptionRepository {

    fun <T: Strategy> save(t: T): Long

    fun <T: Strategy> update(id: Long, t: T): Int

    fun strategyType(): StrategyType
}