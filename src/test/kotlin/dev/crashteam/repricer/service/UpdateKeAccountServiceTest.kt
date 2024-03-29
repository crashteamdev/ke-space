package dev.crashteam.repricer.service

import dev.crashteam.repricer.ContainerConfiguration
import dev.crashteam.repricer.client.ke.KazanExpressLkClient
import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.client.ke.model.lk.*
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.repository.postgre.*
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.ResponseEntity
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Testcontainers
@SpringBootTest
class UpdateKeAccountServiceTest : ContainerConfiguration() {

    @Autowired
    lateinit var updateKeAccountService: UpdateKeAccountService

    @Autowired
    lateinit var keAccountService: KeAccountService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var keAccountRepository: KeAccountRepository

    @Autowired
    lateinit var keAccountShopRepository: KeAccountShopRepository

    @Autowired
    lateinit var keAccountShopItemRepository: KeAccountShopItemRepository

    @Autowired
    lateinit var subscriptionRepository: SubscriptionRepository

    @MockBean
    lateinit var kazanExpressSecureService: KazanExpressSecureService

    @MockBean
    lateinit var kazanExpressWebClient: KazanExpressWebClient

    @MockBean
    lateinit var kazanExpressLkClient: KazanExpressLkClient

    val userId = UUID.randomUUID().toString()

    val keAccountId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        val subscriptionEntity = subscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
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
    }

    @Test
    fun `update ke account shops data with new data`() {
        // Given
        val firstAccountShop = AccountShop(
            id = 1,
            shopTitle = "test",
            urlTitle = "testUrl",
            skuTitle = "testSkuTitle"
        )
        val secondAccountShop = AccountShop(
            id = 2,
            shopTitle = "test2",
            urlTitle = "testUrl2",
            skuTitle = "testSkuTitle2"
        )
        whenever(kazanExpressSecureService.getAccountShops(any(), any())).then {
            listOf(firstAccountShop, secondAccountShop)
        }

        // When
        updateKeAccountService.updateShops(userId, keAccountId)
        val keAccountShops = keAccountShopRepository.getKeAccountShops(keAccountId)

        // Then
        assertEquals(2, keAccountShops.size)
        assertTrue(keAccountShops.find { it.externalShopId == firstAccountShop.id } != null)
        assertTrue(keAccountShops.find { it.externalShopId == secondAccountShop.id } != null)
    }

    @Test
    fun `update ke account shops data with removing old shop`() {
        // Given
        val firstAccountShop = AccountShop(
            id = 1,
            shopTitle = "test",
            urlTitle = "testUrl",
            skuTitle = "testSkuTitle"
        )
        val secondAccountShop = AccountShop(
            id = 2,
            shopTitle = "test2",
            urlTitle = "testUrl2",
            skuTitle = "testSkuTitle2"
        )
        keAccountShopRepository.save(
            KazanExpressAccountShopEntity(
                id = UUID.randomUUID(),
                keAccountId = keAccountId,
                externalShopId = firstAccountShop.id,
                name = firstAccountShop.shopTitle,
                skuTitle = firstAccountShop.skuTitle
            )
        )
        keAccountShopRepository.save(
            KazanExpressAccountShopEntity(
                id = UUID.randomUUID(),
                keAccountId = keAccountId,
                externalShopId = secondAccountShop.id,
                name = secondAccountShop.shopTitle,
                skuTitle = secondAccountShop.skuTitle
            )
        )
        whenever(kazanExpressSecureService.getAccountShops(any(), any())).then {
            listOf(firstAccountShop)
        }

        // When
        updateKeAccountService.updateShops(userId, keAccountId)
        val keAccountShops = keAccountShopRepository.getKeAccountShops(keAccountId)

        // Then
        assertEquals(1, keAccountShops.size)
        assertTrue(keAccountShops.find { it.externalShopId == firstAccountShop.id } != null)
    }

    @Test
    fun `update shop items`() {
        // Given
        val firstAccountShop = AccountShop(
            id = 1,
            shopTitle = "test",
            urlTitle = "testUrl",
            skuTitle = "testSkuTitle"
        )
        val keAccountShopEntity = KazanExpressAccountShopEntity(
            id = UUID.randomUUID(),
            keAccountId = keAccountId,
            externalShopId = 1,
            name = "test",
            skuTitle = "TEST-SHOP"
        )
        keAccountShopRepository.save(keAccountShopEntity)
        val keShopItem = AccountShopItem(
            productId = 4132,
            title = "test",
            skuTitle = "test",
            category = "test",
            status = ShopItemStatus("test", "test"),
            moderationStatus = ShopItemModerationStatus("test", "test"),
            commission = BigDecimal.TEN,
            commissionDto = ShopItemCommission(1000, 2000),
            skuList = listOf(
                ShopItemSku(
                    skuTitle = "test",
                    skuFullTitle = "testFull",
                    productTitle = "test",
                    skuId = 54124,
                    barcode = 54321623L,
                    purchasePrice = BigDecimal.TEN,
                    price = BigDecimal.TEN,
                    quantityActive = 10,
                    quantityAdditional = 10,
                )

            ),
            image = "https://ke-images.servicecdn.ru/cbtma55i6omb975ssukg/t_product_240_low.jpg"
        )
        whenever(kazanExpressSecureService.authUser(any(), any())).thenReturn("test")
        whenever(kazanExpressLkClient.checkToken(any(), any())).thenReturn(
            ResponseEntity.ok(
                CheckTokenResponse(14L, true, "test", "test", 123L)
            )
        )
        whenever(kazanExpressSecureService.getAccountShops(any(), any())).then {
            listOf(firstAccountShop)
        }
        whenever(
            kazanExpressSecureService.getAccountShopItems(any(), any(), any(), any())
        ).then { listOf(keShopItem) }.then { emptyList<AccountShopItem>() }
        val accountProductInfo = AccountProductInfo(
            category = AccountProductCategory(1, "test"),
            title = "test",
            skuTitle = "test"
        )
        whenever(
            kazanExpressSecureService.getProductInfo(any(), any(), any(), any())
        ).then { accountProductInfo }
        whenever(
            kazanExpressWebClient.getProductInfo(any())
        ).then {
            null
        }

        // When
        keAccountService.syncAccount(userId, keAccountId)
        val shopItems = keAccountShopItemRepository.findShopItems(keAccountId, keAccountShopEntity.id!!)

        // Then
        assertEquals(1, shopItems.size)
        assertTrue(shopItems.find { it.productId == keShopItem.productId } != null)
    }
}
