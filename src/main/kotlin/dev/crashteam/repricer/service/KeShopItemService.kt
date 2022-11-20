package dev.crashteam.repricer.service

import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash
import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.client.ke.model.web.ProductData
import dev.crashteam.repricer.client.ke.model.web.ProductPhoto
import dev.crashteam.repricer.repository.postgre.KeShopItemRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemEntity
import dev.crashteam.repricer.service.loader.RemoteImageLoader
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.imageio.ImageIO

private val log = KotlinLogging.logger {}

@Service
class KeShopItemService(
    private val keShopItemRepository: KeShopItemRepository,
    private val remoteImageLoader: RemoteImageLoader,
    private val kazanExpressWebClient: KazanExpressWebClient,
) {

    private val avgHash = AverageHash(64)

    private val pHash = PerceptiveHash(64)

    @Transactional
    fun addShopItemFromKeData(productData: ProductData) {
        val kazanExpressShopItemEntities = productData.skuList!!.mapNotNull { productSplit ->
            val photo: ProductPhoto = productSplit.characteristics.firstNotNullOfOrNull {
                val productCharacteristic = productData.characteristics[it.charIndex]
                val characteristicValue = productCharacteristic.values[it.valueIndex]
                val value = characteristicValue.value
                productData.photos.filter { photo -> photo.color != null }
                    .find { photo -> photo.color == value }
            } ?: productData.photos.firstOrNull() ?: return@mapNotNull null // Ignore empty photo item

            val url =
                "https://ke-images.servicecdn.ru/${photo.photoKey}/t_product_240_high.jpg" // TODO: avoid static url
            val imageFingerprints = generateImageFingerprints(url)
            val characteristics = productSplit.characteristics.joinToString {
                val productCharacteristic = productData.characteristics[it.charIndex]
                productCharacteristic.values[it.valueIndex].title
            }
            val productTitle = productData.title + " " + characteristics
            KazanExpressShopItemEntity(
                productId = productData.id,
                skuId = productSplit.id,
                categoryId = productData.category.id,
                name = productTitle,
                photoKey = photo.photoKey,
                avgHashFingerprint = imageFingerprints?.avgHash,
                pHashFingerprint = imageFingerprints?.pHash,
                price = productSplit.purchasePrice.movePointRight(2).toLong(),
                availableAmount = productSplit.availableAmount,
                lastUpdate = LocalDateTime.now()
            )
        }
        keShopItemRepository.saveBatch(kazanExpressShopItemEntities)
    }

    fun getRecentPrice(shopItemEntity: KazanExpressShopItemEntity): BigDecimal? {
        return if (shopItemEntity.lastUpdate.isBefore(LocalDateTime.now().minusHours(4))) {
            log.info {
                "Product last update too old. Trying to get info from KE." +
                        " productId=${shopItemEntity.productId}; skuId=${shopItemEntity.skuId}"
            }
            val productInfo =
                kazanExpressWebClient.getProductInfo(shopItemEntity.productId.toString())
            if (productInfo?.payload == null) {
                log.error {
                    "Error during try to change price case can't get product info from KE." +
                            " productId=${shopItemEntity.productId}; skuId=${shopItemEntity.skuId}"
                }
                return null
            }

            // Update recent price
            val keShopItemEntity = findShopItem(shopItemEntity.productId, shopItemEntity.skuId)
                ?: throw IllegalArgumentException("Unknown product. productId=${shopItemEntity.productId};skuId=${shopItemEntity.skuId}")
            val productSplit =
                productInfo.payload.data.skuList!!.find { it.id == shopItemEntity.skuId }!!
            keShopItemRepository.save(
                keShopItemEntity.copy(
                    price = productSplit.purchasePrice.movePointRight(2).toLong(),
                    lastUpdate = LocalDateTime.now()
                )
            )

            productSplit.purchasePrice
        } else {
            shopItemEntity.price.toBigDecimal().movePointLeft(2)
        }
    }

    fun findShopItem(productId: Long, skuId: Long): KazanExpressShopItemEntity? {
        val keShopItemEntity = keShopItemRepository.findByProductIdAndSkuId(productId, skuId)
        if (keShopItemEntity == null) {
            val productInfo = kazanExpressWebClient.getProductInfo(productId.toString())
            if (productInfo?.payload == null)
                throw IllegalStateException("Failed to get product info from KE")
            addShopItemFromKeData(productInfo.payload.data)
        }
        return keShopItemRepository.findByProductIdAndSkuId(productId, skuId)
    }

    // TODO: Refactor and optimize
    fun findSimilarItems(
        shopItemId: UUID,
        productId: Long,
        skuId: Long,
        categoryId: Long,
        productName: String
    ): List<KazanExpressShopItemEntity> {
        val kazanExpressShopItemEntity = keShopItemRepository.findByProductIdAndSkuId(productId, skuId)
        val similarItems = if (kazanExpressShopItemEntity?.pHashFingerprint != null) {
            keShopItemRepository.findSimilarItemsByNameAndHashAndCategoryId(
                shopItemId,
                productId,
                skuId,
                kazanExpressShopItemEntity.name,
                kazanExpressShopItemEntity.categoryId
            )
        } else {
            keShopItemRepository.findSimilarItemsByNameAndCategoryId(
                shopItemId,
                productId,
                skuId,
                productName,
                categoryId,
            )
        }

        return similarItems
    }

    fun findSimilarItemsByName(
        shopItemId: UUID,
        productId: Long,
        skuId: Long,
        name: String
    ): List<KazanExpressShopItemEntity> {
        val targetShopItemEntity = keShopItemRepository.findByProductIdAndSkuId(productId, skuId) ?: return emptyList()
        return keShopItemRepository.findSimilarItemsByNameAndCategoryId(
            shopItemId,
            productId,
            skuId,
            name,
            targetShopItemEntity.categoryId
        )
    }

    fun findSimilarItemsByProductIdSkuId(productId: Long, skuId: Long): List<KazanExpressShopItemEntity> {
        val targetShopItemEntity = keShopItemRepository.findByProductIdAndSkuId(productId, skuId) ?: return emptyList()
        return keShopItemRepository.findSimilarItemsByProductIdAndSkuId(
            productId,
            skuId,
            targetShopItemEntity.name,
            targetShopItemEntity.categoryId
        )
    }

    private fun generateImageFingerprints(url: String): ImageFingerprintHolder? {
        val imageByteArray = remoteImageLoader.loadResource(url)
        return try {
            val avgHashFingerprint = generateFingerprint(imageByteArray, avgHash)
            val pHashFingerprint = generateFingerprint(imageByteArray, pHash)

            ImageFingerprintHolder(avgHashFingerprint, pHashFingerprint)
        } catch (e: Exception) {
            log.warn(e) { "Failed to generate fingerprint from url=${url}; imageByteSize=${imageByteArray.size}" }
            null
        }
    }

    private fun generateFingerprint(byteArray: ByteArray, hashAlgorithm: HashingAlgorithm): String {
        val image = ImageIO.read(ByteArrayInputStream(byteArray))
        return hashAlgorithm.hash(image).hashValue.toString(16).uppercase()
    }

    data class ImageFingerprintHolder(
        val avgHash: String,
        val pHash: String,
    )

}
