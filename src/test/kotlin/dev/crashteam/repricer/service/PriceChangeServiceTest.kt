package dev.crashteam.repricer.service

import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.EqualPriceStrategy
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.repricer.ContainerConfiguration
import dev.crashteam.repricer.client.ke.model.lk.AccountProductDescription
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.repository.postgre.*
import dev.crashteam.repricer.repository.postgre.entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.util.*

@Testcontainers
@SpringBootTest
class PriceChangeServiceTest : ContainerConfiguration() {

    @Autowired
    lateinit var priceChangeService: PriceChangeService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var keAccountRepository: KeAccountRepository

    @Autowired
    lateinit var keAccountShopRepository: KeAccountShopRepository

    @Autowired
    lateinit var keAccountShopItemRepository: KeAccountShopItemRepository

    @Autowired
    lateinit var strategyRepository: KeAccountShopItemStrategyRepository

    @Autowired
    lateinit var keShopItemPoolRepository: KeAccountShopItemPoolRepository

    @Autowired
    lateinit var keShopItemRepository: KeShopItemRepository

    @Autowired
    lateinit var keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository

    @Autowired
    lateinit var priceHistoryRepository: KeShopItemPriceHistoryRepository

    @MockBean
    lateinit var kazanExpressSecureService: KazanExpressSecureService

    val userId = UUID.randomUUID().toString()

    val keAccountId = UUID.randomUUID()

    val keAccountShopId = UUID.randomUUID()

    val keAccountShopItemId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        accountRepository.deleteByUserId(userId)
        accountRepository.save(AccountEntity(userId = userId))
        val accountEntity = accountRepository.getAccount(userId)
        keAccountRepository.save(
            KazanExpressAccountEntity(
                id = keAccountId,
                accountId = accountEntity?.id!!,
                externalAccountId = 634542345L,
                name = "test",
                login = "test",
                password = "test",
                monitorState = MonitorState.active,
                updateState = UpdateState.not_started,
            )
        )
        val kazanExpressAccountShopEntity = KazanExpressAccountShopEntity(
            id = keAccountShopId,
            keAccountId = keAccountId,
            externalShopId = 123432,
            name = "Test",
            skuTitle = "TEST-SHOP"
        )
        keAccountShopRepository.save(kazanExpressAccountShopEntity)
        keAccountShopItemRepository.save(
            KazanExpressAccountShopItemEntity(
                id = keAccountShopItemId,
                keAccountId = keAccountId,
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
                maximumThreshold = 6000,
                step = 10)
        )
    }

    @Test
    fun `check equal price strategy`() {

        val kazanExpressAccountShopItemCompetitorEntity = KazanExpressAccountShopItemCompetitorEntity(
            id = UUID.randomUUID(),
            keAccountShopItemId = keAccountShopItemId,
            productId = 635243L,
            skuId = 4231453L
        )
        val competitorKeShopItemEntity = KazanExpressShopItemEntity(
            productId = 635243L,
            skuId = 4231453L,
            categoryId = 556231L,
            name = "test",
            photoKey = "test",
            avgHashFingerprint = "test",
            pHashFingerprint = "test",
            price = 7000,
            availableAmount = 10,
            lastUpdate = LocalDateTime.now()
        )
        keShopItemPoolRepository.save(KazanExpressAccountShopItemPoolEntity(keAccountShopItemId))

        keShopItemRepository.save(competitorKeShopItemEntity)
        keAccountShopItemCompetitorRepository.save(kazanExpressAccountShopItemCompetitorEntity)

        val equalPriceStrategy = EqualPriceStrategy("equal_price", 10.0, 60.0)

        val strategyRequest = AddStrategyRequest(keAccountShopItemId, equalPriceStrategy)
        strategyRepository.save(strategyRequest)

        val patchStrategy = PatchStrategy()
        patchStrategy.strategy = equalPriceStrategy
        strategyRepository.update(keAccountShopItemId, patchStrategy)

        whenever(kazanExpressSecureService.getProductDescription(any(), any(), any(), any() )).then {
            AccountProductDescription(
                id = 12345L,
                shopSkuTitle = "skuTitle",
                title = "justTitle",
                productSkuTitle = "productSkuTitle",
                commission = 1,
                hasActiveCalendarEvents = true,
                hasCustomCharacteristics = false,
                definedCharacteristicList = emptyList(),
                customCharacteristicList = emptyList(),
                skuList = emptyList()
            )
        }
        whenever(kazanExpressSecureService.changeAccountShopItemPrice(any(), any(), any(), any())).then { true }

        priceChangeService.recalculateUserShopItemPrice(userId, keAccountId)
        val paginateEntities =
            priceHistoryRepository.findHistoryByShopItemId(keAccountShopItemId, limit = 10, offset = 0)
        val shopItemEntity = keAccountShopItemRepository.findShopItem(keAccountId, keAccountShopId, keAccountShopItemId)
        val shopItemPoolFilledEntity = keShopItemPoolRepository.findShopItemInPool(userId, keAccountId).first()

        // Then
        assertEquals(1, paginateEntities.size)
        assertEquals(10000, paginateEntities.first().item.oldPrice)
        assertEquals(6000, paginateEntities.first().item.price)
        assertTrue(shopItemPoolFilledEntity.lastCheck != null)
        assertEquals(6000, shopItemEntity?.price)

    }

    @Test
    fun `change user pool item price`() {

        val kazanExpressAccountShopItemCompetitorEntity = KazanExpressAccountShopItemCompetitorEntity(
            id = UUID.randomUUID(),
            keAccountShopItemId = keAccountShopItemId,
            productId = 635242L,
            skuId = 4231456L
        )
        val competitorKeShopItemEntity = KazanExpressShopItemEntity(
            productId = 635242L,
            skuId = 4231456L,
            categoryId = 556235L,
            name = "test",
            photoKey = "test",
            avgHashFingerprint = "test",
            pHashFingerprint = "test",
            price = 5000,
            availableAmount = 10,
            lastUpdate = LocalDateTime.now()
        )
        keShopItemPoolRepository.save(KazanExpressAccountShopItemPoolEntity(keAccountShopItemId))

        keShopItemRepository.save(competitorKeShopItemEntity)
        keAccountShopItemCompetitorRepository.save(kazanExpressAccountShopItemCompetitorEntity)

        val equalPriceStrategy = EqualPriceStrategy("equal_price", 10.0, 40.0)

        val strategyRequest = AddStrategyRequest(keAccountShopItemId, equalPriceStrategy)
        strategyRepository.save(strategyRequest)

        whenever(kazanExpressSecureService.getProductDescription(any(), any(), any(), any() )).then {
            AccountProductDescription(
                id = 12345L,
                shopSkuTitle = "skuTitle",
                title = "justTitle",
                productSkuTitle = "productSkuTitle",
                commission = 1,
                hasActiveCalendarEvents = true,
                hasCustomCharacteristics = false,
                definedCharacteristicList = emptyList(),
                customCharacteristicList = emptyList(),
                skuList = emptyList()
            )
        }
        whenever(kazanExpressSecureService.changeAccountShopItemPrice(any(), any(), any(), any())).then { true }

        priceChangeService.recalculateUserShopItemPrice(userId, keAccountId)
        val paginateEntities =
            priceHistoryRepository.findHistoryByShopItemId(keAccountShopItemId, limit = 10, offset = 0)
        val shopItemEntity = keAccountShopItemRepository.findShopItem(keAccountId, keAccountShopId, keAccountShopItemId)
        val shopItemPoolFilledEntity = keShopItemPoolRepository.findShopItemInPool(userId, keAccountId).first()

        // Then
        assertEquals(1, paginateEntities.size)
        assertEquals(10000, paginateEntities.first().item.oldPrice)
        assertEquals(4000, paginateEntities.first().item.price)
        assertTrue(shopItemPoolFilledEntity.lastCheck != null)
        assertEquals(4000, shopItemEntity?.price)
    }

}
