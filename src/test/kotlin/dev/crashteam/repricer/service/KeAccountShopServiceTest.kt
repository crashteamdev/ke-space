package dev.crashteam.repricer.service

import dev.crashteam.repricer.ContainerConfiguration
import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.client.ke.model.web.*
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.repository.postgre.*
import dev.crashteam.repricer.repository.postgre.entity.*
import dev.crashteam.repricer.service.loader.RemoteImageLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Testcontainers
class KeAccountShopServiceTest : ContainerConfiguration() {

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var keAccountRepository: KeAccountRepository

    @Autowired
    lateinit var keAccountShopRepository: KeAccountShopRepository

    @Autowired
    lateinit var keAccountShopItemRepository: KeAccountShopItemRepository

    @Autowired
    lateinit var keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository

    @Autowired
    lateinit var accountSubscriptionRepository: SubscriptionRepository

    @Autowired
    lateinit var keShopItemRepository: KeShopItemRepository

    @Autowired
    lateinit var keAccountShopService: KeAccountShopService

    @MockBean
    lateinit var kazanExpressWebClient: KazanExpressWebClient

    @MockBean
    lateinit var remoteImageLoader: RemoteImageLoader

    @Value("classpath:cc1j1sp1ati4tcj33p5g-original.jpg")
    lateinit var keImage: Resource

    val userId = UUID.randomUUID().toString()

    val keAccountId = UUID.randomUUID()

    val keAccountShopId = UUID.randomUUID()

    val keAccountShopItemId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.deleteByUserId(userId)
        accountRepository.save(
            AccountEntity(
                userId = userId,
                subscription = subscriptionEntity,
                subscriptionValidUntil = LocalDateTime.now().plusDays(30)
            )
        )
        val accountId = accountRepository.getAccount(userId)!!.id!!
        val kazanExpressAccountEntity = KazanExpressAccountEntity(
            id = keAccountId,
            accountId = accountId,
            externalAccountId = 14,
            name = "account name",
            lastUpdate = LocalDateTime.now(),
            monitorState = MonitorState.suspended,
            login = "test",
            password = "test",
            updateState = UpdateState.not_started
        )
        keAccountRepository.save(kazanExpressAccountEntity)
        val kazanExpressAccountShopEntity = KazanExpressAccountShopEntity(
            id = keAccountShopId,
            keAccountId = kazanExpressAccountEntity.id!!,
            externalShopId = 123432,
            name = "Test",
            skuTitle = "TEST-SHOP"
        )
        keAccountShopRepository.save(kazanExpressAccountShopEntity)
        keAccountShopItemRepository.save(
            KazanExpressAccountShopItemEntity(
                id = keAccountShopItemId,
                keAccountId = kazanExpressAccountEntity.id!!,
                keAccountShopId = kazanExpressAccountShopEntity.id!!,
                categoryId = 123,
                productId = 123456,
                skuId = 789,
                name = "testName",
                photoKey = "gfdfqkowef",
                purchasePrice = 50000,
                price = 10000,
                barCode = 4535643512893379581L,
                availableAmount = 10,
                lastUpdate = LocalDateTime.now(),
                productSku = "testProductSku",
                skuTitle = "testSkuTitle",
                minimumThreshold = 1000,
                maximumThreshold = 2000,
                step = 10
            )
        )
    }

    @Test
    fun `add shop item into pool`() {
        // When
        keAccountShopService.addShopItemIntoPool(userId, keAccountId, keAccountShopId, keAccountShopItemId)
        val keAccountShopItems =
            keAccountShopService.getShopItemsInPool(userId, keAccountId, keAccountShopId, limit = 10, offset = 0)
        val keAccountShopItem = keAccountShopItems.first().item

        // Then
        assertEquals(1, keAccountShopItems.size)
        assertEquals(keAccountShopItemId, keAccountShopItem.id)
    }

    @Test
    fun `remove shop item from pool`() {
        // When
        keAccountShopService.addShopItemIntoPool(userId, keAccountId, keAccountShopId, keAccountShopItemId)
        keAccountShopService.removeShopItemFromPool(userId, keAccountId, keAccountShopId, keAccountShopItemId)
        val shopItemPoolCount = keAccountShopService.getShopItemPoolCount(userId)

        // Then
        assertEquals(0, shopItemPoolCount)
    }

    @Test
    fun `add shop item competitor with exists shop item`() {
        // Given
        val productId = 5462623L
        val skuId = 7456462345L
        val keShopItem = KazanExpressShopItemEntity(
            productId = productId,
            skuId = skuId,
            categoryId = 12345,
            name = "testName",
            photoKey = "43Fkk10LE",
            avgHashFingerprint = "test",
            pHashFingerprint = "test",
            price = 1000,
            availableAmount = 10,
        )

        // When
        keShopItemRepository.save(keShopItem)
        keAccountShopService.addShopItemCompetitor(
            userId,
            keAccountId,
            keAccountShopId,
            keAccountShopItemId,
            productId,
            skuId
        )
        val shopItemCompetitors = keAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)

        // Then
        assertEquals(1, shopItemCompetitors.size)
        assertEquals(productId, shopItemCompetitors.first().productId)
        assertEquals(skuId, shopItemCompetitors.first().skuId)
    }

    @Test
    fun `add shop item competitor without exist shop item`() {
        // Given
        val productId = 5462623L
        val skuId = 7564265423L
        whenever(kazanExpressWebClient.getProductInfo(any())).then { buildProductResponse(productId, skuId) }
        whenever(remoteImageLoader.loadResource(any())).then { keImage.inputStream.readAllBytes() }

        // When
        keAccountShopService.addShopItemCompetitor(
            userId,
            keAccountId,
            keAccountShopId,
            keAccountShopItemId,
            productId,
            skuId
        )
        val shopItemCompetitors = keAccountShopItemCompetitorRepository.findShopItemCompetitors(keAccountShopItemId)

        // Then
        assertEquals(1, shopItemCompetitors.size)
        assertEquals(productId, shopItemCompetitors.first().productId)
        assertEquals(skuId, shopItemCompetitors.first().skuId)

    }

    private fun buildProductResponse(productId: Long, skuId: Long): ProductResponse {
        return ProductResponse(
            payload = ProductDataWrapper(
                data = ProductData(
                    id = productId,
                    title = "test",
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
