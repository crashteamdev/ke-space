package dev.crashteam.repricer.price

import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.KeShopItemRepository
import dev.crashteam.repricer.service.RecentPriceService
import dev.crashteam.repricer.service.model.ShopItemCompetitor
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class CloseToMinimalPriceChangeCalculatorStrategy(
    private val keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository,
    private val keShopItemRepository: KeShopItemRepository,
    private val recentPriceService: RecentPriceService,
) : PriceChangeCalculatorStrategy {

    override fun calculatePrice(
        keAccountShopItemId: UUID,
        sellPrice: BigDecimal,
        options: CalculatorOptions?
    ): CalculationResult? {
        val shopItemCompetitors =
            keAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)
        val minimalPriceCompetitor = shopItemCompetitors.map {
            val shopItemEntity = keShopItemRepository.findByProductIdAndSkuId(
                it.productId,
                it.skuId
            )!!
            ShopItemCompetitor(shopItemEntity, it)
        }.filter {
            it.shopItemEntity.availableAmount > 0
        }.minByOrNull {
            recentPriceService.getCompetitorPrice(it)!!
        } ?: return null
        val competitorPrice = recentPriceService.getCompetitorPrice(minimalPriceCompetitor)!!
        var newPrice = (competitorPrice - (options?.step?.toBigDecimal() ?: BigDecimal.ZERO)).movePointRight(2)

        if (options?.minimumThreshold != null && newPrice < BigDecimal.valueOf(options.minimumThreshold)) {
            newPrice = BigDecimal.valueOf(options.minimumThreshold)
        } else if (options?.maximumThreshold != null && newPrice > BigDecimal.valueOf(options.maximumThreshold)) {
            newPrice = BigDecimal.valueOf(options.maximumThreshold)
        }

        if (newPrice == sellPrice) return null // No need to change price

        return CalculationResult(newPrice = newPrice, competitorId = minimalPriceCompetitor.competitorEntity.id)
    }
}
