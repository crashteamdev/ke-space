package dev.crashteam.repricer.price

import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.KeShopItemRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemEntity
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
        val minimalPriceCompetitor = shopItemCompetitors.map {
            val shopItemEntity = keShopItemRepository.findByProductIdAndSkuId(
                it.productId,
                it.skuId
            )!!
            ShopItemCompetitor(shopItemEntity, it)
        }.filter {
            it.shopItemEntity.availableAmount > 0
        }.minByOrNull {
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

        return CalculationResult(newPrice = newPrice, competitorId = minimalPriceCompetitor.competitorEntity.id)
    }

    // TODO: вынести отсюда получение цены конкурента что еще обновляло в базе запись
    private fun getCompetitorPrice(competitor: ShopItemCompetitor): BigDecimal? {
        return if (competitor.shopItemEntity.lastUpdate.isBefore(LocalDateTime.now().minusHours(4))) {
            log.info {
                "Product last update too old. Trying to get info from KE." +
                        " productId=${competitor.shopItemEntity.productId}; skuId=${competitor.shopItemEntity.skuId}"
            }
            val productInfo =
                kazanExpressWebClient.getProductInfo(competitor.shopItemEntity.productId.toString())
            if (productInfo?.payload == null) {
                log.error {
                    "Error during try to change price case can't get product info from KE." +
                            " productId=${competitor.shopItemEntity.productId}; skuId=${competitor.shopItemEntity.skuId}"
                }
                return null
            }
            val productSplit =
                productInfo.payload.data.skuList!!.find { it.id == competitor.shopItemEntity.skuId }!!
            productSplit.purchasePrice
        } else {
            competitor.shopItemEntity.price.toBigDecimal().movePointLeft(2)
        }
    }

    private data class ShopItemCompetitor(
        val shopItemEntity: KazanExpressShopItemEntity,
        val competitorEntity: KazanExpressAccountShopItemCompetitorEntity
    )
}
