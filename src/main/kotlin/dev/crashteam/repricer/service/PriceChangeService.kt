package dev.crashteam.repricer.service

import dev.crashteam.repricer.client.ke.model.lk.*
import dev.crashteam.repricer.db.model.enums.StrategyType
import dev.crashteam.repricer.price.CloseToMinimalPriceChangeCalculatorStrategy
import dev.crashteam.repricer.price.PriceChangeCalculatorStrategy
import dev.crashteam.repricer.price.model.CalculationResult
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemPoolRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemRepository
import dev.crashteam.repricer.repository.postgre.KeShopItemPriceHistoryRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemPoolFilledEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemPriceHistoryEntity
import mu.KotlinLogging
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class PriceChangeService(
    private val keAccountShopItemPoolRepository: KeAccountShopItemPoolRepository,
    private val kazanExpressSecureService: KazanExpressSecureService,
    private val keShopItemPriceHistoryRepository: KeShopItemPriceHistoryRepository,
    private val keAccountShopItemRepository: KeAccountShopItemRepository,
    private val retryTemplate: RetryTemplate,
    private val strategyService: KeShopItemStrategyService,
    private val calculators: Map<StrategyType, PriceChangeCalculatorStrategy>
) {

    @Transactional
    fun recalculateUserShopItemPrice(userId: String, keAccountId: UUID) {
        val poolItem = keAccountShopItemPoolRepository.findShopItemInPool(userId, keAccountId)
        log.debug { "Found ${poolItem.size} pool items. userId=$userId;keAccountId=$keAccountId" }
        for (poolFilledEntity in poolItem) {
            try {
                log.debug { "Begin calculate item price. keAccountShopItemId=${poolFilledEntity.keAccountShopItemId};" +
                        ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                val calculationResult = calculationResult(poolFilledEntity)
                log.debug { "Calculation result = $calculationResult. keAccountShopItemId=${poolFilledEntity.keAccountShopItemId};" +
                        ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                if (calculationResult == null) {
                    log.info { "No need to change item price. keAccountShopItemId=${poolFilledEntity.keAccountShopItemId};" +
                            "productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                    continue
                }
                val accountProductDescription = retryTemplate.execute<AccountProductDescription, Exception> {
                    kazanExpressSecureService.getProductDescription(
                        userId = userId,
                        keAccountId = keAccountId,
                        shopId = poolFilledEntity.externalShopId,
                        productId = poolFilledEntity.productId
                    )
                }

                val strategy = strategyService.findStrategy(poolFilledEntity.strategyId!!)

                val newSkuList = buildNewSkuList(userId, keAccountId, poolFilledEntity,
                    calculationResult, strategy?.minimumThreshold, strategy?.discount?.toBigDecimal())
                log.debug { "Trying to change account shop item price. " +
                        "keAccountShopItemId=${poolFilledEntity.keAccountShopItemId};" +
                        ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}" }
                val changeAccountShopItemPrice = kazanExpressSecureService.changeAccountShopItemPrice(
                    userId = userId,
                    keAccountId = keAccountId,
                    shopId = poolFilledEntity.externalShopId,
                    payload = ShopItemPriceChangePayload(
                        productId = poolFilledEntity.productId,
                        skuForProduct = poolFilledEntity.productSku,
                        skuList = newSkuList,
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
                            price = calculationResult.newPriceMinor.toLong()
                        )
                    )
                    val shopItemEntity =
                        keAccountShopItemRepository.findShopItem(
                            keAccountId,
                            poolFilledEntity.keAccountShopItemId
                        )!!
                    keAccountShopItemRepository.save(shopItemEntity.copy(price = calculationResult.newPriceMinor.toLong()))
                    keAccountShopItemPoolRepository.updateLastCheck(
                        poolFilledEntity.keAccountShopItemId,
                        lastCheckTime
                    )
                    log.info {
                        "Successfully change price for item. " +
                                "id=${poolFilledEntity.keAccountShopItemId}; productId=${poolFilledEntity.productId}; skuId=${poolFilledEntity.skuId}"
                    }
                }
            } catch (e: Exception) {
                log.warn(e) { "Failed to change item price. keAccountShopItemId=${poolFilledEntity.keAccountShopItemId}" +
                        ";productId=${poolFilledEntity.productId};skuId=${poolFilledEntity.skuId}, cause - ${e.cause?.message}" }
            }
        }
    }

    private fun buildNewSkuList(
        userId: String,
        keAccountId: UUID,
        poolFilledEntity: KazanExpressAccountShopItemPoolFilledEntity,
        calculationResult: CalculationResult,
        minimumThreshold: Long?,
        discount: BigDecimal?
    ): List<SkuPriceChangeSku> {
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
            fullPrice = calculationResult.newPriceMinor.movePointLeft(2).toLong(),
            sellPrice = calculateDiscountPrice(discount, minimumThreshold, calculationResult.newPriceMinor),
            skuTitle = poolFilledEntity.skuTitle,
            barCode = poolFilledEntity.barcode.toString(),
        )

        return skuList.toMutableList().apply {
            add(changeSku)
        }
    }

    private fun calculationResult(poolFilledEntity: KazanExpressAccountShopItemPoolFilledEntity): CalculationResult? {
        if (poolFilledEntity.strategyId != null) {
            val strategy = strategyService.findStrategy(poolFilledEntity.strategyId)
            val calculatorStrategy = calculators[StrategyType.valueOf(strategy!!.strategyType)]
            return calculatorStrategy!!.calculatePrice(
                poolFilledEntity.keAccountShopItemId,
                BigDecimal.valueOf(poolFilledEntity.price),
                CalculatorOptions(
                    step = strategy.step,
                    minimumThreshold = strategy.minimumThreshold,
                    maximumThreshold = strategy.maximumThreshold
                )
            )
        } else {
            throw IllegalArgumentException("Strategy not exist for pool entity with keAccountShopItemId = ${poolFilledEntity.keAccountShopItemId}")
        }
    }

    private fun calculateDiscountPrice(discount: BigDecimal?, minimumThreshold: Long?, newPriceMinor: BigDecimal): Long {
        return if (discount != null && !discount.equals(0)) {
            val discountedPrice = (newPriceMinor - ((newPriceMinor * discount) / BigDecimal(100)))
                .movePointLeft(2).toLong()
            if (minimumThreshold != null && discountedPrice < minimumThreshold) {
                newPriceMinor.movePointLeft(2).toLong()
            } else {
                discountedPrice
            }
        } else newPriceMinor.movePointLeft(2).toLong()
    }

}
