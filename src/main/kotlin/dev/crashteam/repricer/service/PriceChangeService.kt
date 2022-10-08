package dev.crashteam.repricer.service

import dev.crashteam.repricer.client.ke.model.lk.*
import dev.crashteam.repricer.price.PriceChangeCalculatorStrategy
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemPoolRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemRepository
import dev.crashteam.repricer.repository.postgre.KeShopItemPriceHistoryRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemPriceHistoryEntity
import mu.KotlinLogging
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class PriceChangeService(
    private val keAccountShopItemPoolRepository: KeAccountShopItemPoolRepository,
    private val kazanExpressSecureService: KazanExpressSecureService,
    private val keShopItemPriceHistoryRepository: KeShopItemPriceHistoryRepository,
    private val keAccountShopItemRepository: KeAccountShopItemRepository,
    private val priceChangeCalculatorStrategy: PriceChangeCalculatorStrategy,
    private val retryTemplate: RetryTemplate
) {

    @Transactional
    fun recalculateUserShopItemPrice(userId: String, keAccountId: UUID) {
        val poolItem = keAccountShopItemPoolRepository.findShopItemInPool(userId, keAccountId)
        for (poolFilledEntity in poolItem) {
            val calculationResult = priceChangeCalculatorStrategy.calculatePrice(
                poolFilledEntity.keAccountShopItemId,
                BigDecimal.valueOf(poolFilledEntity.price),
                CalculatorOptions(
                    step = poolFilledEntity.step,
                    minimumThreshold = poolFilledEntity.minimumThreshold,
                    maximumThreshold = poolFilledEntity.maximumThreshold
                )
            )
            if (calculationResult == null) {
                log.info { "No need to change item price. productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                return
            }
            val accountProductDescription = retryTemplate.execute<AccountProductDescription, Exception> {
                kazanExpressSecureService.getProductDescription(
                    userId = userId,
                    keAccountId = keAccountId,
                    shopId = poolFilledEntity.externalShopId,
                    productId = poolFilledEntity.productId
                )
            }
            val skuList = accountProductDescription.skuList.filter { it.id != poolFilledEntity.skuId }.map {
                SkuPriceChangeSku(
                    id = it.id,
                    fullPrice = it.fullPrice,
                    sellPrice = it.sellPrice,
                    skuTitle = it.skuTitle,
                    barCode = it.barcode,
                    skuCharacteristicList = it.skuCharacteristicList.map {
                        SkuCharacteristic(
                            it.characteristicTitle,
                            it.definedType,
                            it.characteristicValue
                        )
                    }
                )
            }
            val changeSku = SkuPriceChangeSku(
                id = poolFilledEntity.skuId,
                fullPrice = calculationResult.newPrice.movePointLeft(2).toLong(),
                sellPrice = if (poolFilledEntity.discount != null) {
                    (calculationResult.newPrice - ((calculationResult.newPrice * poolFilledEntity.discount.toBigDecimal()) / BigDecimal(
                        100
                    ))).movePointLeft(2).toLong()
                } else calculationResult.newPrice.movePointLeft(2).toLong(),
                skuTitle = poolFilledEntity.skuTitle,
                barCode = poolFilledEntity.barcode.toString(),
            )
            val allSkuList = skuList.toMutableList().apply {
                add(changeSku)
            }
            val changeAccountShopItemPrice = kazanExpressSecureService.changeAccountShopItemPrice(
                userId = userId,
                keAccountId = keAccountId,
                shopId = poolFilledEntity.externalShopId,
                payload = ShopItemPriceChangePayload(
                    productId = poolFilledEntity.productId,
                    skuForProduct = poolFilledEntity.productSku,
                    skuList = allSkuList,
                    skuTitlesForCustomCharacteristics = if (accountProductDescription.hasCustomCharacteristics) {
                        accountProductDescription.customCharacteristicList.map { customCharacteristic ->
                            SkuTitleCharacteristic(
                                customCharacteristic.characteristicTitle,
                                customCharacteristic.characteristicValues.map {
                                    CustomCharacteristicSkuValue(
                                        it.title,
                                        it.skuValue
                                    )
                                })
                        }
                    } else emptyList()
                )
            )
            if (!changeAccountShopItemPrice) {
                log.warn {
                    "Failed to change price for item." +
                            " id=${poolFilledEntity.keAccountShopItemId}; productId=${poolFilledEntity.productId}; skuId=${poolFilledEntity.skuId}"
                }
            } else {
                val lastCheckTime = LocalDateTime.now()
                keShopItemPriceHistoryRepository.save(
                    KazanExpressShopItemPriceHistoryEntity(
                        keAccountShopItemId = poolFilledEntity.keAccountShopItemId,
                        keAccountShopItemCompetitorId = calculationResult.competitorId,
                        changeTime = lastCheckTime,
                        oldPrice = poolFilledEntity.price,
                        price = calculationResult.newPrice.toLong()
                    )
                )
                val shopItemEntity =
                    keAccountShopItemRepository.findShopItem(
                        keAccountId,
                        poolFilledEntity.keAccountShopItemId
                    )!!
                keAccountShopItemRepository.save(shopItemEntity.copy(price = calculationResult.newPrice.toLong()))
                keAccountShopItemPoolRepository.updateLastCheck(
                    poolFilledEntity.keAccountShopItemId,
                    lastCheckTime
                )
                log.info {
                    "Successfully change price for item. " +
                            "id=${poolFilledEntity.keAccountShopItemId}; productId=${poolFilledEntity.productId}; skuId=${poolFilledEntity.skuId}"
                }
            }
        }
    }

}
