package dev.crashteam.repricer.service

import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.repository.postgre.KeShopItemRepository
import dev.crashteam.repricer.service.model.ShopItemCompetitor
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Service
class RecentPriceService(
    private val kazanExpressWebClient: KazanExpressWebClient,
    private val keShopItemRepository: KeShopItemRepository
) {

    fun getCompetitorPrice(competitor: ShopItemCompetitor): BigDecimal? {
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
            keShopItemRepository.save(
                competitor.shopItemEntity.copy(
                    price = productSplit.purchasePrice.movePointRight(2).toLong()
                )
            )

            productSplit.purchasePrice
        } else {
            competitor.shopItemEntity.price.toBigDecimal().movePointLeft(2)
        }
    }
}
