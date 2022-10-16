package dev.crashteam.repricer.service

import dev.crashteam.repricer.ContainerConfiguration
import dev.crashteam.repricer.client.ke.model.web.*
import dev.crashteam.repricer.repository.postgre.KeShopItemRepository
import dev.crashteam.repricer.service.loader.RemoteImageLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal

@Testcontainers
class KeShopItemServiceTest : ContainerConfiguration() {

    @Autowired
    lateinit var keShopItemService: KeShopItemService

    @Autowired
    lateinit var keShopItemRepository: KeShopItemRepository

    @MockBean
    lateinit var remoteImageLoader: RemoteImageLoader

    @Value("classpath:cc1j1sp1ati4tcj33p5g-original.jpg")
    lateinit var keImage: Resource

    @Value("classpath:c9to5uua4v5ivhop2260.jpg")
    lateinit var firstProductImage: Resource

    @Value("classpath:cb0tdv59c87ulfd00oug.jpg")
    lateinit var secondProductImage: Resource

    @Test
    fun `add shop item from KazanExpress data`() {
        // Given
        val productId = 4315422L
        val skuId = 5424121L
        val product = buildProductResponse(productId, skuId)
        whenever(remoteImageLoader.loadResource(any())).then { keImage.inputStream.readAllBytes() }

        // When
        keShopItemService.addShopItemFromKeData(product.payload!!.data)
        val kazanExpressShopItemEntity = keShopItemRepository.findByProductIdAndSkuId(productId, skuId)

        // Then
        assertNotNull(kazanExpressShopItemEntity)
        assertEquals(productId, kazanExpressShopItemEntity?.productId)
        assertEquals(skuId, kazanExpressShopItemEntity?.skuId)
    }

//    @Test
//    fun `find similar item`() {
//        // Given
//        val firstProductId = 4315422L
//        val firstSkuId = 5424121L
//        val firstProduct =
//            buildProductResponse(firstProductId, firstSkuId, "Батарейка крона 6LR61 Smartbuy Ultra алкалиновая")
//        val secondProductId = 764253532L
//        val secondSkuId = 123523543632L
//        val secondProduct = buildProductResponse(
//            secondProductId,
//            secondSkuId,
//            "Батарейка SmartBuy Ultra Alkaline КРОНА 9V алкалиновая 6LR61"
//        )
//        whenever(remoteImageLoader.loadResource(any()))
//            .then { firstProductImage.inputStream.readAllBytes() }
//            .then { secondProductImage.inputStream.readAllBytes() }
//
//        // When
//        keShopItemService.addShopItemFromKeData(firstProduct.payload?.data!!)
//        keShopItemService.addShopItemFromKeData(secondProduct.payload?.data!!)
//        val similarItems = keShopItemService.findSimilarItemsByName(firstProductId, firstSkuId, firstProduct.payload!!.data.title)
//
//        // Then
//        assertEquals(1, similarItems.size)
//        assertEquals(secondProductId, similarItems.first().productId)
//        assertEquals(secondSkuId, similarItems.first().skuId)
//    }

    private fun buildProductResponse(productId: Long, skuId: Long, title: String = "test"): ProductResponse {
        return ProductResponse(
            payload = ProductDataWrapper(
                data = ProductData(
                    id = productId,
                    title = title,
                    category = ProductCategory(
                        id = 12343,
                        title = "test",
                        productAmount = 10,
                    ),
                    reviewsAmount = 10,
                    ordersAmount = 100,
                    rOrdersAmount = 150,
                    rating = BigDecimal.valueOf(5),
                    totalAvailableAmount = 350,
                    charityCommission = 50,
                    description = "test",
                    attributes = listOf("test"),
                    tags = listOf("test"),
                    photos = listOf(
                        ProductPhoto(
                            photo = mapOf("test" to ProductPhotoQuality("800", "400")),
                            photoKey = "test",
                            color = "black"
                        )
                    ),
                    characteristics = listOf(
                        ProductCharacteristic(
                            "black",
                            listOf(CharacteristicValue("black", "black"))
                        )
                    ),
                    skuList = listOf(
                        ProductSplit(
                            id = skuId,
                            characteristics = listOf(ProductSplitCharacteristic(charIndex = 0, valueIndex = 0)),
                            availableAmount = 10,
                            fullPrice = BigDecimal.TEN,
                            purchasePrice = BigDecimal.ONE
                        )
                    ),
                    seller = Seller(
                        id = 12341,
                        title = "test",
                        link = "test",
                        description = "test",
                        rating = BigDecimal.TEN,
                        sellerAccountId = 423542161,
                        isEco = false,
                        adultCategory = true,
                        contacts = listOf(Contact("test", "test"))
                    )
                )
            )
        )
    }


}
