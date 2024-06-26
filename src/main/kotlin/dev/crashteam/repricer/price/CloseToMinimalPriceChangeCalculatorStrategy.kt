package dev.crashteam.repricer.price

import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.service.AnalyticsService
import dev.crashteam.repricer.service.KeShopItemService
import dev.crashteam.repricer.service.model.ShopItemCompetitor
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class CloseToMinimalPriceChangeCalculatorStrategy(
    private val keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository,
    private val keShopItemService: KeShopItemService,
    private val analyticsService: AnalyticsService
) : PriceChangeCalculatorStrategy {

    override fun calculatePrice(
        keAccountShopItemId: UUID,
        sellPriceMinor: BigDecimal,
        options: CalculatorOptions?
    ): CalculationResult? {
        val shopItemCompetitors: List<KazanExpressAccountShopItemCompetitorEntity> =
            keAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)
        if (CollectionUtils.isEmpty(shopItemCompetitors)) {
            log.info { "Not found competitors for shop item with id $keAccountShopItemId pool items." }
            return null
        }
        val minimalPriceCompetitor: ShopItemCompetitor = shopItemCompetitors.mapNotNull {
            val shopItemEntity = keShopItemService.findShopItem(
                it.productId,
                it.skuId
            ) ?: return@mapNotNull null
            ShopItemCompetitor(shopItemEntity, it)
        }.filter {
            if (options?.competitorAvailableAmount != null) {
                it.shopItemEntity.availableAmount > options.competitorAvailableAmount
            } else {
                true
            }
        }.minByOrNull {
            keShopItemService.getRecentPrice(it.shopItemEntity)!!
        } ?: return null

        if (minimalPriceCompetitor.shopItemEntity.availableAmount.toInt() <= 0 && options?.changeNotAvailableItemPrice == false) {
            log.info { "Competitor available amount is 0, not changing price" }
            return null
        }

        if (options?.competitorSalesAmount != null) {
            val competitorSales = analyticsService.getCompetitorSales(minimalPriceCompetitor.competitorEntity.productId)
                ?: return null
            if (competitorSales <= options.competitorSalesAmount) {
                log.info { "Last sale value of competitor is $competitorSales and our barrier is ${options.competitorSalesAmount}." }
                return null
            }
        }


        log.debug { "Minimal price competitor: $minimalPriceCompetitor" }
        val competitorPrice: BigDecimal = keShopItemService.getRecentPrice(minimalPriceCompetitor.shopItemEntity)!!
        val competitorPriceMinor = competitorPrice.movePointRight(2)
        log.debug { "Recent competitor price: $competitorPrice. keAccountShopItemId=$keAccountShopItemId" }

        if (competitorPriceMinor >= sellPriceMinor) {
            log.debug {
                "Competitor price is the same or higher." +
                        " competitorPrice=${competitorPriceMinor}; sellPrice=$sellPriceMinor. keAccountShopItemId=$keAccountShopItemId"
            }
            // If price too much higher than our we need to rise our price
            val expectedPriceMinor =
                competitorPriceMinor - (options?.step?.toBigDecimal() ?: BigDecimal.ZERO).movePointRight(2)
            if (expectedPriceMinor > sellPriceMinor && options?.maximumThreshold != null
                && expectedPriceMinor <= BigDecimal.valueOf(options.maximumThreshold)
            ) {
                return CalculationResult(
                    newPriceMinor = expectedPriceMinor,
                    competitorId = minimalPriceCompetitor.competitorEntity.id
                )
            }
            return null // No need to change price
        } else {
            val newPrice: BigDecimal = (competitorPrice - (options?.step?.toBigDecimal() ?: BigDecimal.ZERO))
            log.debug { "Competitor price = $competitorPrice. New price = $newPrice. Current sell price = $sellPriceMinor. keAccountShopItemId=$keAccountShopItemId" }

            var newPriceMinor = newPrice.movePointRight(2)
            if (options?.minimumThreshold != null && newPriceMinor < BigDecimal.valueOf(options.minimumThreshold)) {
                newPriceMinor = BigDecimal.valueOf(options.minimumThreshold)
            } else if (options?.maximumThreshold != null && newPriceMinor > BigDecimal.valueOf(options.maximumThreshold)) {
                newPriceMinor = BigDecimal.valueOf(options.maximumThreshold)
            }
            log.debug { "newPriceMinor=$newPriceMinor;sellPriceMinor=$sellPriceMinor;keAccountShopItemId=$keAccountShopItemId" }

            if (newPriceMinor == sellPriceMinor) return null // No need to change price

            return CalculationResult(
                newPriceMinor = newPriceMinor,
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }
    }
}
