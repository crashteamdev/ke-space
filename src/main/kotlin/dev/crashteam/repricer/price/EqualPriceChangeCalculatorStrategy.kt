package dev.crashteam.repricer.price

import dev.crashteam.mp.base.Date
import dev.crashteam.mp.base.DateRange
import dev.crashteam.mp.external.analytics.category.ExternalCategoryAnalyticsServiceGrpc
import dev.crashteam.mp.external.analytics.category.GetProductDailyAnalyticsRequest
import dev.crashteam.mp.external.analytics.category.GetProductDailyAnalyticsResponse
import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.service.AnalyticsService
import dev.crashteam.repricer.service.KeShopItemService
import dev.crashteam.repricer.service.error.GrpcIntegrationException
import dev.crashteam.repricer.service.model.ShopItemCompetitor
import mu.KotlinLogging
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class EqualPriceChangeCalculatorStrategy(
    private val keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository,
    private val keShopItemService: KeShopItemService
) : PriceChangeCalculatorStrategy {

    private lateinit var analyticsService: AnalyticsService

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

        val competitorPrice: BigDecimal = keShopItemService.getRecentPrice(minimalPriceCompetitor.shopItemEntity)!!
        val competitorPriceMinor = competitorPrice.movePointRight(2)

        var newPriceMinor: BigDecimal? = null
        if ((options?.minimumThreshold != null && options.maximumThreshold != null)
            && (competitorPriceMinor >= BigDecimal.valueOf(options.minimumThreshold)
                    && competitorPriceMinor <= BigDecimal.valueOf(options.maximumThreshold))
        ) {
            newPriceMinor = competitorPriceMinor
        } else if (options?.minimumThreshold != null && competitorPriceMinor < BigDecimal.valueOf(options.minimumThreshold)) {
            newPriceMinor = BigDecimal.valueOf(options.minimumThreshold)
        } else if (options?.maximumThreshold != null && competitorPriceMinor > BigDecimal.valueOf(options.maximumThreshold)) {
            newPriceMinor = BigDecimal.valueOf(options.maximumThreshold)
        }
        if (newPriceMinor != null && newPriceMinor.compareTo(sellPriceMinor) == 0) {
            log.info { "New price $newPriceMinor equal to current price $sellPriceMinor for shop item $keAccountShopItemId" }
            return null
        } else if (newPriceMinor != null) {
            return CalculationResult(
                newPriceMinor = newPriceMinor,
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }

        if ((options?.maximumThreshold == null && competitorPriceMinor > sellPriceMinor)
            || (options?.minimumThreshold == null && competitorPriceMinor < sellPriceMinor)
        ) {
            return CalculationResult(
                newPriceMinor = competitorPriceMinor,
                competitorId = minimalPriceCompetitor.competitorEntity.id
            )
        }
        log.debug {
            "Competitor price $competitorPriceMinor, ours min price - ${options?.minimumThreshold}, " +
                    "max price ${options?.maximumThreshold}. Current sell price - $sellPriceMinor. Shop item id - $keAccountShopItemId"
        }
        return null;
    }
}