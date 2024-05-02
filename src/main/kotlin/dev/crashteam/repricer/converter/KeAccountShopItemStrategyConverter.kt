package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.KeAccountShopItemStrategy
import dev.crashteam.repricer.repository.postgre.entity.strategy.KazanExpressAccountShopItemStrategyEntity
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class KeAccountShopItemStrategyConverter: DataConverter<KazanExpressAccountShopItemStrategyEntity, KeAccountShopItemStrategy> {
    override fun convert(source: KazanExpressAccountShopItemStrategyEntity): KeAccountShopItemStrategy {
        return KeAccountShopItemStrategy().apply {
            id = source.id
            minimumThreshold = (source.minimumThreshold?.toBigDecimal() ?: BigDecimal.ZERO).movePointLeft(2).toDouble()
            maximumThreshold = (source.maximumThreshold?.toBigDecimal() ?: BigDecimal.ZERO).movePointLeft(2).toDouble()
            step = source.step
            strategyType = source.strategyType
            discount = source.discount?.toBigDecimal()
            accountShopItemId = source.accountShopItemId
            competitorAvailableAmount = source.competitorAvailableAmount
            competitorSalesAmount = source.competitorSalesAmount
            changeNotAvailableItemPrice = source.changeNotAvailableItemPrice
        }
    }
}