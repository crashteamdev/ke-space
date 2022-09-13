package dev.crashteam.repricer.price.model

import java.math.BigDecimal
import java.util.UUID

data class CalculationResult(
    val newPrice: BigDecimal,
    val competitorId: UUID
)
