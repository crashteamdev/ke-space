package dev.crashteam.repricer.service

import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.client.ke.model.lk.AccountProductInfo
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
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream

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
    private val transactionTemplate: TransactionTemplate,
) {

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

    fun updateShopItems(userId: String, keAccountId: UUID, accountShopEntity: KazanExpressAccountShopEntity) {
        var page = 0
        val shopUpdateTime = LocalDateTime.now()
        var isActive = true
        while (isActive) {
            log.debug { "Iterate through keAccountShop. shopId=${accountShopEntity.externalShopId}; page=$page" }

            Thread.sleep(Random().nextLong(1000, 4000))
            log.debug { "Update account shop items by shopId=${accountShopEntity.externalShopId}" }
            val accountShopItems = try {
                kazanExpressSecureService.getAccountShopItems(
                    userId,
                    keAccountId,
                    accountShopEntity.externalShopId,
                    page
                )
            } catch (e: Exception) {
                log.warn(e) { "Failed to get user account shop items. userId=$userId; keAccountId=$keAccountId;" +
                        " shopId=${accountShopEntity.externalShopId}; page=$page" }
                null
            }
            if (accountShopItems.isNullOrEmpty()) {
                log.debug { "The list of shops is over. shopId=${accountShopEntity.externalShopId}" }
                break
            }
            log.debug { "Iterate through accountShopItems. shopId=${accountShopEntity.externalShopId}; size=${accountShopItems.size}" }
            val shopItemEntities = accountShopItems.parallelStream().flatMap { accountShopItem ->
                val productInfo =
                    getProductInfo(userId, keAccountId, accountShopEntity.externalShopId, accountShopItem.productId)
                        ?: return@flatMap null
                val kazanExpressAccountShopItemEntities = accountShopItem.skuList.map { shopItemSku ->
                    val kazanExpressAccountShopItemEntity = keAccountShopItemRepository.findShopItem(
                        keAccountId,
                        accountShopEntity.id!!,
                        accountShopItem.productId,
                        shopItemSku.skuId
                    )
                    val photoKey = accountShopItem.image.split("/")[3]
                    KazanExpressAccountShopItemEntity(
                        id = kazanExpressAccountShopItemEntity?.id ?: UUID.randomUUID(),
                        keAccountId = keAccountId,
                        keAccountShopId = accountShopEntity.id,
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
                        lastUpdate = shopUpdateTime
                    )
                }
                Stream.of(kazanExpressAccountShopItemEntities)
            }.toList().flatten()
            log.debug { "Save new shop items. size=${shopItemEntities.size}" }
            keAccountShopItemRepository.saveBatch(shopItemEntities)
            page += 1
        }
        val oldItemDeletedCount = keAccountShopItemRepository.deleteWhereOldLastUpdate(
            keAccountId,
            accountShopEntity.id!!,
            shopUpdateTime
        )
        log.debug { "Deleted $oldItemDeletedCount old products" }
    }

    private fun getProductInfo(
        userId: String,
        keAccountId: UUID,
        accountExternalShopId: Long,
        productId: Long
    ): AccountProductInfo? {
        // Update product data from web KE
        try {
            val productResponse = kazanExpressWebClient.getProductInfo(productId.toString())
            if (productResponse?.payload?.data != null) {
                keShopItemService.addShopItemFromKeData(productResponse.payload.data)
            }
        } catch (e: Exception) {
            log.warn(e) { "Failed to get product info. productId=$productId" }
        }
        return try {
            kazanExpressSecureService.getProductInfo(
                userId,
                keAccountId,
                accountExternalShopId,
                productId
            )
        } catch (e: Exception) {
            log.warn(e) { "Failed to get user product info. productId=$productId" }
            null
        }
    }

}

