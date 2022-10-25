package dev.crashteam.repricer.price

import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.service.KeShopItemService
import dev.crashteam.repricer.service.model.ShopItemCompetitor
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class CloseToMinimalPriceChangeCalculatorStrategy(
    private val keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository,
    private val keShopItemService: KeShopItemService,
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
        log.debug { "Minimal price competitor: $minimalPriceCompetitor" }
        val competitorPrice: BigDecimal = keShopItemService.getRecentPrice(minimalPriceCompetitor.shopItemEntity)!!

        if (competitorPrice.movePointRight(2) >= sellPriceMinor) {
            log.debug { "Competitor price is the same or higher." +
                    " competitorPrice=${competitorPrice.movePointRight(2)}; sellPrice=$sellPriceMinor" }
            return null
        }

        val newPrice: BigDecimal = (competitorPrice - (options?.step?.toBigDecimal() ?: BigDecimal.ZERO))
        log.debug { "Competitor price = $competitorPrice. New price = $newPrice. Current sell price = $sellPriceMinor" }

        var newPriceMinor = newPrice.movePointRight(2)
        if (options?.minimumThreshold != null && newPriceMinor < BigDecimal.valueOf(options.minimumThreshold)) {
            newPriceMinor = BigDecimal.valueOf(options.minimumThreshold)
        } else if (options?.maximumThreshold != null && newPriceMinor > BigDecimal.valueOf(options.maximumThreshold)) {
            newPriceMinor = BigDecimal.valueOf(options.maximumThreshold)
        }

        if (newPriceMinor == sellPriceMinor) return null // No need to change price

        return CalculationResult(newPrice = newPrice, competitorId = minimalPriceCompetitor.competitorEntity.id)
    }
}
