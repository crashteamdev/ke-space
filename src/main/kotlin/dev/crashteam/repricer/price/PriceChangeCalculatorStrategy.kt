package dev.crashteam.repricer.price

import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import java.math.BigDecimal
import java.util.*

interface PriceChangeCalculatorStrategy {
    fun calculatePrice(
        keAccountShopItemId: UUID,
        sellPriceMinor: BigDecimal,
        options: CalculatorOptions? = null
    ): CalculationResult?
}
