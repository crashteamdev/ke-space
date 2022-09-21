package dev.crashteam.repricer.job

import dev.crashteam.repricer.client.ke.KazanExpressLkClient
import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.lk.CheckTokenResponse
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.service.KazanExpressSecureService
import dev.crashteam.repricer.service.UpdateKeAccountService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

class UpdateAccountDataJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val keAccountRepository = applicationContext.getBean(KeAccountRepository::class.java)
        val updateKeAccountService = applicationContext.getBean(UpdateKeAccountService::class.java)
        val transactionManager = applicationContext.getBean(PlatformTransactionManager::class.java)
        val kazanExpressSecureService = applicationContext.getBean(KazanExpressSecureService::class.java)
        val kazanExpressLkClient = applicationContext.getBean(KazanExpressLkClient::class.java)
        val userId = context.jobDetail.jobDataMap["userId"] as? String
            ?: throw IllegalStateException("userId can't be null")
        val keAccountId = context.jobDetail.jobDataMap["keAccountId"] as? UUID
            ?: throw IllegalStateException("keAccountId can't be null")
        try {
            log.info { "Execute update ke account job. userId=$userId;keAccountId=$keAccountId" }
            keAccountRepository.changeUpdateState(userId, keAccountId, UpdateState.in_progress)
            TransactionTemplate(transactionManager).execute {
                val accessToken = kazanExpressSecureService.authUser(userId, keAccountId)
                val checkToken = kazanExpressLkClient.checkToken(userId, accessToken).body!!
                val kazanExpressAccount = keAccountRepository.getKazanExpressAccount(userId, keAccountId)!!.copy(
                    externalAccountId = checkToken.accountId,
                    name = checkToken.firstName,
                    email = checkToken.email
                )
                keAccountRepository.save(kazanExpressAccount)
                log.info { "Update shops. userId=$userId;keAccountId=$keAccountId" }
                updateKeAccountService.updateShops(userId, keAccountId)
                log.info { "Update shop items. userId=$userId;keAccountId=$keAccountId" }
                updateKeAccountService.updateShopItems(userId, keAccountId)
                log.info { "Change update state to finished. userId=$userId;keAccountId=$keAccountId" }
                keAccountRepository.changeUpdateState(userId, keAccountId, UpdateState.finished, LocalDateTime.now())
            }
        } catch (e: Exception) {
            log.warn(e) { "Failed to update account data for userId=$userId;keAccountId=$keAccountId"  }
            keAccountRepository.changeUpdateState(userId, keAccountId, UpdateState.error)
        }
    }
}
