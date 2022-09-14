package dev.crashteam.repricer.service

import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash
import dev.crashteam.repricer.client.ke.model.web.ProductData
import dev.crashteam.repricer.client.ke.model.web.ProductPhoto
import dev.crashteam.repricer.repository.postgre.KeShopItemRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemEntity
import dev.crashteam.repricer.service.loader.RemoteImageLoader
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import javax.imageio.ImageIO

private val log = KotlinLogging.logger {}

@Service
class KeShopItemService(
    private val keShopItemRepository: KeShopItemRepository,
    private val remoteImageLoader: RemoteImageLoader
) {

    private val avgHash = AverageHash(32)

    private val pHash = PerceptiveHash(32)

    @Transactional
    fun addShopItemFromKeData(productData: ProductData) {
        val kazanExpressShopItemEntities = productData.skuList!!.map { productSplit ->
            val photo: ProductPhoto? = productSplit.characteristics.firstNotNullOfOrNull {
                val productCharacteristic = productData.characteristics[it.charIndex]
                val characteristicValue = productCharacteristic.values[it.valueIndex]
                val value = characteristicValue.value
                productData.photos.filter { photo -> photo.color != null }
                    .find { photo -> photo.color == value }
            } ?: productData.photos.firstOrNull()
            val url =
                "https://ke-images.servicecdn.ru/${photo!!.photoKey}/original.jpg" // TODO: avoid static url
            val imageByteArray = remoteImageLoader.loadResource(url)
            val avgHashFingerprint = generateFingerprint(imageByteArray, avgHash)
            val pHashFingerprint = generateFingerprint(imageByteArray, pHash)
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
                avgHashFingerprint = avgHashFingerprint,
                pHashFingerprint = pHashFingerprint,
                price = productSplit.purchasePrice.movePointRight(2).toLong(),
                availableAmount = productSplit.availableAmount,
                lastUpdate = LocalDateTime.now()
            )
        }
        keShopItemRepository.saveBatch(kazanExpressShopItemEntities)
    }

    fun findSimilarItems(productId: Long, skuId: Long): List<KazanExpressShopItemEntity> {
        val targetShopItemEntity = keShopItemRepository.findByProductIdAndSkuId(productId, skuId) ?: return emptyList()
        return keShopItemRepository.findSimilarItems(
            productId,
            skuId,
            targetShopItemEntity.avgHashFingerprint,
            targetShopItemEntity.pHashFingerprint,
            targetShopItemEntity.name
        )
    }

    private fun generateFingerprint(byteArray: ByteArray, hashAlgorithm: HashingAlgorithm): String {
        val image = ImageIO.read(ByteArrayInputStream(byteArray))
        return hashAlgorithm.hash(image).hashValue.toString(16).uppercase()
    }

}
