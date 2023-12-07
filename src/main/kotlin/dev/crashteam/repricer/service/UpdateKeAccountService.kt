package dev.crashteam.repricer.service

import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.job.UpdateAccountDataJob
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemEntity
import mu.KotlinLogging
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.SimpleTrigger
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val log = KotlinLogging.logger {}

@Service
class UpdateKeAccountService(
    private val keAccountRepository: KeAccountRepository,
    private val keAccountShopRepository: KeAccountShopRepository,
    private val keAccountShopItemRepository: KeAccountShopItemRepository,
    private val kazanExpressSecureService: KazanExpressSecureService,
    private val kazanExpressWebClient: KazanExpressWebClient,
    private val keShopItemService: KeShopItemService,
    private val scheduler: Scheduler,
    private val retryTemplate: RetryTemplate
) {

    val executorService: ExecutorService = Executors.newFixedThreadPool(5)

    @Transactional
    fun executeUpdateJob(userId: String, keAccountId: UUID): Boolean {
        val kazanExpressAccount = keAccountRepository.getKazanExpressAccount(userId, keAccountId)!!
        if (kazanExpressAccount.updateState == UpdateState.in_progress) {
            log.debug { "Ke account update already in progress. userId=$userId;keAccountId=$keAccountId" }
            return false
        }
        if (kazanExpressAccount.lastUpdate != null) {
            val lastUpdate = kazanExpressAccount.lastUpdate.plusMinutes(10)
            if (lastUpdate?.isBefore(LocalDateTime.now()) == false) {
                log.debug { "Ke account update data was done recently. Need to try again later. userId=$userId;keAccountId=$keAccountId" }
                return false
            }
        }
        val jobIdentity = "ke-account-update-job-$keAccountId"
        val jobDetail =
            JobBuilder.newJob(UpdateAccountDataJob::class.java).withIdentity(jobIdentity).build()
        val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
            setName(jobIdentity)
            setStartTime(Date())
            setRepeatInterval(0L)
            setRepeatCount(0)
            setPriority(Int.MAX_VALUE / 2)
            setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
            afterPropertiesSet()
        }.getObject()
        jobDetail.jobDataMap["userId"] = userId
        jobDetail.jobDataMap["keAccountId"] = keAccountId
        scheduler.scheduleJob(jobDetail, triggerFactoryBean)

        return true
    }

    @Transactional
    fun updateShops(userId: String, keAccountId: UUID) {
        val accountShops = kazanExpressSecureService.getAccountShops(userId, keAccountId)
        val shopIdSet = accountShops.map { it.id }.toHashSet()
        val keAccountShops = keAccountShopRepository.getKeAccountShops(userId, keAccountId)
        val kazanExpressAccountShopIdsToRemove =
            keAccountShops.filter { !shopIdSet.contains(it.externalShopId) }.map { it.externalShopId }
        keAccountShopRepository.deleteByShopIds(kazanExpressAccountShopIdsToRemove)
        for (accountShop in accountShops) {
            val kazanExpressAccountShopEntity =
                keAccountShopRepository.getKeAccountShopByShopId(keAccountId, accountShop.id)
            if (kazanExpressAccountShopEntity != null) {
                val updateKeShopEntity =
                    kazanExpressAccountShopEntity.copy(
                        externalShopId = accountShop.id,
                        name = accountShop.shopTitle,
                        skuTitle = accountShop.skuTitle
                    )
                keAccountShopRepository.save(updateKeShopEntity)
            } else {
                keAccountShopRepository.save(
                    KazanExpressAccountShopEntity(
                        id = UUID.randomUUID(),
                        keAccountId = keAccountId,
                        externalShopId = accountShop.id,
                        name = accountShop.shopTitle,
                        skuTitle = accountShop.skuTitle
                    )
                )
            }
        }
    }

    @Transactional
    fun updateShopItems(userId: String, keAccountId: UUID) {
        val keAccountShops = keAccountShopRepository.getKeAccountShops(userId, keAccountId)
        try {
            for (keAccountShop in keAccountShops) {
                processAccountShop(userId, keAccountId, keAccountShop)
            }
        } finally {
            executorService.shutdown()
        }
    }

    fun processAccountShop(userId: String, keAccountId: UUID, keAccountShop: KazanExpressAccountShopEntity) {
        executorService.submit {
            var page = 0
            val shopUpdateTime = LocalDateTime.now()
            var isActive = true
            while (isActive) {
                log.debug { "Iterate through keAccountShop. shopId=${keAccountShop.externalShopId}; page=$page" }
                retryTemplate.execute<Void, Exception> {
                    Thread.sleep(Random().nextLong(1000, 4000))
                    log.debug { "Update account shop items by shopId=${keAccountShop.externalShopId}" }
                    val accountShopItems = kazanExpressSecureService.getAccountShopItems(
                        userId,
                        keAccountId,
                        keAccountShop.externalShopId,
                        page
                    )

                    if (accountShopItems.isEmpty()) {
                        log.debug { "The list of shops is over. shopId=${keAccountShop.externalShopId}" }
                        isActive = false
                        return@execute null
                    }
                    log.debug { "Iterate through accountShopItems. shopId=${keAccountShop.externalShopId}; size=${accountShopItems.size}" }
                    val shopItemEntities = accountShopItems.flatMap { accountShopItem ->
                        // Update product data from web KE
                        val productResponse = kazanExpressWebClient.getProductInfo(accountShopItem.productId.toString())
                        if (productResponse?.payload?.data != null) {
                            keShopItemService.addShopItemFromKeData(productResponse.payload.data)
                        }
                        // Update product data from LK KE
                        val productInfo = kazanExpressSecureService.getProductInfo(
                            userId,
                            keAccountId,
                            keAccountShop.externalShopId,
                            accountShopItem.productId
                        )
                        val kazanExpressAccountShopItemEntities = accountShopItem.skuList.map { shopItemSku ->
                            val kazanExpressAccountShopItemEntity = keAccountShopItemRepository.findShopItem(
                                keAccountId,
                                keAccountShop.id!!,
                                accountShopItem.productId,
                                shopItemSku.skuId
                            )
                            val photoKey = accountShopItem.image.split("/")[3]
                            KazanExpressAccountShopItemEntity(
                                id = kazanExpressAccountShopItemEntity?.id ?: UUID.randomUUID(),
                                keAccountId = keAccountId,
                                keAccountShopId = keAccountShop.id,
                                categoryId = productInfo.category.id,
                                productId = accountShopItem.productId,
                                skuId = shopItemSku.skuId,
                                name = shopItemSku.productTitle,
                                photoKey = photoKey,
                                purchasePrice = shopItemSku.purchasePrice?.movePointRight(2)?.toLong(),
                                price = shopItemSku.price.movePointRight(2).toLong(),
                                barCode = shopItemSku.barcode,
                                productSku = accountShopItem.skuTitle,
                                skuTitle = shopItemSku.skuFullTitle,
                                availableAmount = shopItemSku.quantityActive + shopItemSku.quantityAdditional,
                                lastUpdate = shopUpdateTime,
                                strategyId = kazanExpressAccountShopItemEntity?.strategyId
                            )
                        }
                        kazanExpressAccountShopItemEntities
                    }
                    log.debug { "Save new shop items by shopId=${keAccountShop.externalShopId}. size=${shopItemEntities.size}" }
                    keAccountShopItemRepository.saveBatch(shopItemEntities)
                    page += 1
                    null
                }
                val oldItemDeletedCount = keAccountShopItemRepository.deleteWhereOldLastUpdate(
                    keAccountId,
                    keAccountShop.id!!,
                    shopUpdateTime
                )
                log.debug { "Deleted $oldItemDeletedCount old products" }
            }
        }
    }

}
