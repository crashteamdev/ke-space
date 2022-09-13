package dev.crashteam.repricer.service

import dev.crashteam.repricer.ContainerConfiguration
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
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.util.*

@Testcontainers
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
            name = "Test"
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
                fullPrice = 50000,
                sellPrice = 10000,
                barCode = 4535643512893379581L,
                availableAmount = 10,
                lastUpdate = LocalDateTime.now(),
                productSku = "testProductSku",
                skuTitle = "testSkuTitle",
                minimumThreshold = 1000,
                maximumThreshold = 6000,
                step = 10
            )
        )
    }

    @Test
    fun `change user pool item price`() {
        // Given
        val kazanExpressAccountShopItemCompetitorEntity = KazanExpressAccountShopItemCompetitorEntity(
            id = UUID.randomUUID(),
            keAccountShopItemId = keAccountShopItemId,
            productId = 635242L,
            skuId = 4231456L
        )
        val keShopItemEntity = KazanExpressShopItemEntity(
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
        whenever(kazanExpressSecureService.changeAccountShopItemPrice(any(), any(), any(), any())).then { true }

        // When
        keShopItemRepository.save(keShopItemEntity)
        keAccountShopItemCompetitorRepository.save(kazanExpressAccountShopItemCompetitorEntity)
        priceChangeService.changeUserShopItemPrice(userId, keAccountId)
        val paginateEntities =
            priceHistoryRepository.findHistoryByShopItemId(keAccountShopItemId, limit = 10, offset = 0)
        val shopItemEntity = keAccountShopItemRepository.findShopItem(keAccountId, keAccountShopId, keAccountShopItemId)
        val shopItemPoolFilledEntity = keShopItemPoolRepository.findShopItemInPool(userId, keAccountId).first()

        // Then
        assertEquals(1, paginateEntities.size)
        assertEquals(10000, paginateEntities.first().item.oldPrice)
        assertEquals(4000, paginateEntities.first().item.price)
        assertTrue(shopItemPoolFilledEntity.lastCheck != null)
        assertEquals(4000, shopItemEntity?.sellPrice)
    }

}
