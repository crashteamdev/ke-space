package dev.crashteam.repricer.job

import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.service.PriceChangeService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import java.util.*

private val log = KotlinLogging.logger {}

class KeShopItemPriceChangeJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val priceChangeService = applicationContext.getBean(PriceChangeService::class.java)
        val keAccountRepository = applicationContext.getBean(KeAccountRepository::class.java)
        val keAccountId = context.jobDetail.jobDataMap["keAccountId"] as? UUID
            ?: throw IllegalStateException("keAccountId can't be null")
        val kazanExpressAccountEntity = keAccountRepository.getKazanExpressInitializedAccount(keAccountId)!!
        log.info { "Check account items price for userId=${kazanExpressAccountEntity.userId}; keAccountId=$keAccountId" }
        priceChangeService.recalculateUserShopItemPrice(kazanExpressAccountEntity.userId, keAccountId)
    }
}
