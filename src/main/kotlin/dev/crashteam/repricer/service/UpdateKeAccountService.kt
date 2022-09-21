package dev.crashteam.repricer.service

import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.job.UpdateAccountDataJob
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopRepository
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountShopItemEntity
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.SimpleTrigger
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class UpdateKeAccountService(
    private val keAccountRepository: KeAccountRepository,
    private val keAccountShopRepository: KeAccountShopRepository,
    private val keAccountShopItemRepository: KeAccountShopItemRepository,
    private val kazanExpressSecureService: KazanExpressSecureService,
    private val scheduler: Scheduler,
) {

    @Transactional
    fun executeUpdateJob(userId: String, keAccountId: UUID): Boolean {
        val kazanExpressAccount = keAccountRepository.getKazanExpressAccount(userId, keAccountId)!!
        if (kazanExpressAccount.updateState == UpdateState.in_progress) return false
        if (kazanExpressAccount.lastUpdate != null) {
            val lastUpdate = kazanExpressAccount.lastUpdate.plusMinutes(10)
            if (lastUpdate?.isAfter(LocalDateTime.now()) == false) {
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
                    kazanExpressAccountShopEntity.copy(externalShopId = accountShop.id, name = accountShop.shopTitle)
                keAccountShopRepository.save(updateKeShopEntity)
            } else {
                keAccountShopRepository.save(
                    KazanExpressAccountShopEntity(
                        id = UUID.randomUUID(),
                        keAccountId = keAccountId,
                        externalShopId = accountShop.id,
                        name = accountShop.shopTitle
                    )
                )
            }
        }
    }

    @Transactional
    fun updateShopItems(userId: String, keAccountId: UUID) {
        val keAccountShops = keAccountShopRepository.getKeAccountShops(userId, keAccountId)
        for (keAccountShop in keAccountShops) {
            var page = 0
            val shopUpdateTime = LocalDateTime.now()
            while (true) {
                val accountShopItems = kazanExpressSecureService.getAccountShopItems(
                    userId,
                    keAccountId,
                    keAccountShop.externalShopId,
                    page
                )

                if (accountShopItems.isEmpty()) break

                for (accountShopItem in accountShopItems) {
                    val productInfo = kazanExpressSecureService.getProductInfo(
                        userId,
                        keAccountId,
                        keAccountShop.externalShopId,
                        accountShopItem.productId
                    )
                    val kazanExpressAccountShopItemEntities = accountShopItem.skuList.mapNotNull { shopItemSku ->
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
                            purchasePrice = shopItemSku.purchasePrice?.toLong(),
                            price = shopItemSku.price.toLong(),
                            barCode = shopItemSku.barcode,
                            productSku = accountShopItem.skuTitle,
                            skuTitle = shopItemSku.skuFullTitle,
                            availableAmount = shopItemSku.quantityActive + shopItemSku.quantityAdditional,
                            lastUpdate = shopUpdateTime
                        )
                    }
                    keAccountShopItemRepository.saveBatch(kazanExpressAccountShopItemEntities)
                }
                page += 1
            }
            keAccountShopItemRepository.deleteWhereOldLastUpdate(
                keAccountId,
                keAccountShop.id!!,
                shopUpdateTime
            )
        }
    }

}
