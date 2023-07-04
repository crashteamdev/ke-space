package dev.crashteam.repricer.price

import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.service.KeShopItemService
import dev.crashteam.repricer.service.model.ShopItemCompetitor
import java.math.BigDecimal
import java.util.*

class EqualPriceChangeCalculatorStrategy(
    private val keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository,
    private val keShopItemService: KeShopItemService
) : PriceChangeCalculatorStrategy {
    override fun calculatePrice(
        keAccountShopItemId: UUID,
        sellPriceMinor: BigDecimal,
        options: CalculatorOptions?
    ): CalculationResult? {
        val shopItemCompetitors: List<KazanExpressAccountShopItemCompetitorEntity> =
            keAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)
        val minimalPriceCompetitor: ShopItemCompetitor = shopItemCompetitors.mapNotNull {
            val shopItemEntity = keShopItemService.findShopItem(
                it.productId,
                it.skuId
            ) ?: return@mapNotNull null
            ShopItemCompetitor(shopItemEntity, it)
        }.filter {
            it.shopItemEntity.availableAmount > 0
        }.minByOrNull {
            keShopItemService.getRecentPrice(it.shopItemEntity)!!
        } ?: return null
        val competitorPrice: BigDecimal = keShopItemService.getRecentPrice(minimalPriceCompetitor.shopItemEntity)!!
        val competitorPriceMinor = competitorPrice.movePointRight(2)

        if (options?.minimumThreshold != null && competitorPriceMinor < BigDecimal.valueOf(options.minimumThreshold)) {
            return CalculationResult(
                newPriceMinor = BigDecimal.valueOf(options.minimumThreshold),
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        } else if (options?.maximumThreshold != null && competitorPriceMinor > BigDecimal.valueOf(options.maximumThreshold)) {
            return CalculationResult(
                newPriceMinor =  BigDecimal.valueOf(options.maximumThreshold),
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }

        if (competitorPriceMinor > sellPriceMinor || competitorPriceMinor < sellPriceMinor) {
            return CalculationResult(
                newPriceMinor = competitorPriceMinor,
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }
        return null;
    }
}