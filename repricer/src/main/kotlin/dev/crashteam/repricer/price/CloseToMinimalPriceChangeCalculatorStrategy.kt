package dev.crashteam.repricer.price

import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.KeShopItemRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class CloseToMinimalPriceChangeCalculatorStrategy(
    private val keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository,
    private val keShopItemRepository: KeShopItemRepository,
    private val kazanExpressWebClient: KazanExpressWebClient,
) : PriceChangeCalculatorStrategy {

    override fun calculatePrice(
        keAccountShopItemId: UUID,
        sellPrice: BigDecimal,
        options: CalculatorOptions?
    ): CalculationResult? {
        val shopItemCompetitors =
            keAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)
        val minimalPriceCompetitor = shopItemCompetitors.minByOrNull {
            getCompetitorPrice(it)!!
        } ?: return null
        val competitorPrice = getCompetitorPrice(minimalPriceCompetitor)!!
        var newPrice = (competitorPrice - (options?.step?.toBigDecimal() ?: BigDecimal.ZERO)).movePointRight(2)

        if (options?.minimumThreshold != null && newPrice < BigDecimal.valueOf(options.minimumThreshold)) {
            newPrice = BigDecimal.valueOf(options.minimumThreshold)
        } else if (options?.maximumThreshold != null && newPrice > BigDecimal.valueOf(options.maximumThreshold)) {
            newPrice = BigDecimal.valueOf(options.maximumThreshold)
        }

        if (newPrice == sellPrice) return null // No need to change price

        return CalculationResult(newPrice = newPrice, competitorId = minimalPriceCompetitor.id)
    }

    private fun getCompetitorPrice(competitor: KazanExpressAccountShopItemCompetitorEntity): BigDecimal? {
        val kazanExpressShopItemEntity = keShopItemRepository.findByProductIdAndSkuId(
            competitor.productId,
            competitor.skuId
        )!!
        return if (kazanExpressShopItemEntity.lastUpdate.isBefore(LocalDateTime.now().minusMinutes(30))) {
            log.info {
                "Product last update too old. Trying to get info from KE." +
                        " productId=${competitor.productId}; skuId=${competitor.skuId}"
            }
            val productInfo =
                kazanExpressWebClient.getProductInfo(competitor.productId.toString())
            if (productInfo?.payload == null) {
                log.error {
                    "Error during try to change price case can't get product info from KE." +
                            " productId=${competitor.productId}; skuId=${competitor.skuId}"
                }
                return null
            }
            val productSplit =
                productInfo.payload.data.skuList!!.find { it.id == competitor.skuId }!!
            productSplit.purchasePrice
        } else {
            kazanExpressShopItemEntity.price.toBigDecimal().movePointLeft(2)
        }
    }
}
