package dev.crashteam.repricer.price

import dev.crashteam.repricer.ContainerConfiguration
import dev.crashteam.repricer.price.model.CalculatorOptions
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemCompetitorEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressShopItemEntity
import dev.crashteam.repricer.service.AnalyticsService
import dev.crashteam.repricer.service.KeShopItemService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.util.*

@Testcontainers
@SpringBootTest
class CloseToMinimalPriceChangeCalculatorStrategyTest : ContainerConfiguration() {

    @MockBean
    lateinit var keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository

    @MockBean
    lateinit var keShopItemService: KeShopItemService

    @MockBean
    lateinit var analyticsService: AnalyticsService

    @Test
    fun `calculate price change with multiple competitors`() {
        // Given
        val firstItemProductId = 12345L
        val firstItemSkuId = 94845L
        val secondItemProductId = 543243L
        val secondItemSkuId = 9213945L
        val thirdItemProductId = 1459841L
        val thirdItemSkuId = 6824212L
        val accountShopItemId = UUID.randomUUID()
        val competitorEntity = listOf(
            KazanExpressAccountShopItemCompetitorEntity(
                id = UUID.randomUUID(),
                keAccountShopItemId = accountShopItemId,
                productId = firstItemProductId,
                skuId = firstItemSkuId,
            ),
            KazanExpressAccountShopItemCompetitorEntity(
                id = UUID.randomUUID(),
                keAccountShopItemId = accountShopItemId,
                productId = secondItemProductId,
                skuId = secondItemSkuId,
            ),
            KazanExpressAccountShopItemCompetitorEntity(
                id = UUID.randomUUID(),
                keAccountShopItemId = accountShopItemId,
                productId = thirdItemProductId,
                skuId = thirdItemSkuId,
            )
        )
        val firstShopItem = KazanExpressShopItemEntity(
            productId = firstItemProductId,
            skuId = firstItemSkuId,
            categoryId = 1L,
            name = "test",
            photoKey = "PDFKWL",
            price = 837000,
            availableAmount = 10,
            avgHashFingerprint = null,
            pHashFingerprint = null,
        )
        val secondShopItem = KazanExpressShopItemEntity(
            productId = secondItemProductId,
            skuId = secondItemSkuId,
            categoryId = 1L,
            name = "test 2",
            photoKey = "FDCDMS",
            price = 197000,
            availableAmount = 20,
            avgHashFingerprint = null,
            pHashFingerprint = null,
        )
        val thirdShopItem = KazanExpressShopItemEntity(
            productId = thirdItemProductId,
            skuId = thirdItemSkuId,
            categoryId = 1L,
            name = "test 3",
            photoKey = "ERMDASd",
            price = 21000,
            availableAmount = 0,
            avgHashFingerprint = null,
            pHashFingerprint = null,
        )


        // When
        whenever(keAccountShopItemCompetitorRepository.findShopItemCompetitors(any())).thenReturn(competitorEntity)
        whenever(keShopItemService.findShopItem(firstItemProductId, firstItemSkuId)).thenReturn(firstShopItem)
        whenever(keShopItemService.findShopItem(secondItemProductId, secondItemSkuId)).thenReturn(secondShopItem)
        whenever(keShopItemService.findShopItem(thirdItemProductId, thirdItemSkuId)).thenReturn(thirdShopItem)
        whenever(keShopItemService.getRecentPrice(firstShopItem)).thenReturn(BigDecimal(8370))
        whenever(keShopItemService.getRecentPrice(secondShopItem)).thenReturn(BigDecimal(1970))
        whenever(keShopItemService.getRecentPrice(thirdShopItem)).thenReturn(BigDecimal(4210))
        val priceChangeCalculatorStrategy =
            CloseToMinimalPriceChangeCalculatorStrategy(keAccountShopItemCompetitorRepository, keShopItemService, analyticsService)
        val calculatePrice = priceChangeCalculatorStrategy.calculatePrice(
            accountShopItemId,
            BigDecimal(297000L),
            CalculatorOptions(
                step = 10,
                minimumThreshold = 187000,
                maximumThreshold = 287000
            )
        )

        // Then
        assertTrue(calculatePrice?.newPriceMinor?.toLong() == 196000L)
    }

}
