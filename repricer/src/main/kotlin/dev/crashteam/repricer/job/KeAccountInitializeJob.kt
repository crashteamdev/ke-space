package dev.crashteam.repricer.job

import dev.crashteam.repricer.client.ke.KazanExpressLkClient
import dev.crashteam.repricer.db.model.enums.InitializeState
import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.service.UpdateKeAccountService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import java.util.*

private val log = KotlinLogging.logger {}

class KeAccountInitializeJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val keAccountRepository = applicationContext.getBean(KeAccountRepository::class.java)
        val kazanExpressLkClient = applicationContext.getBean(KazanExpressLkClient::class.java)
        val updateKeAccountService = applicationContext.getBean(UpdateKeAccountService::class.java)
        val keAccountId = context.jobDetail.jobDataMap["keAccountId"] as? UUID
            ?: throw IllegalStateException("keAccountId can't be null")
        val userId = context.jobDetail.jobDataMap["userId"] as? String
            ?: throw IllegalStateException("userId can't be null")
        try {
            val kazanExpressAccount = keAccountRepository.getKazanExpressAccount(keAccountId)!!
            val authResponse = kazanExpressLkClient.auth(
                userId,
                kazanExpressAccount.keAccountEntity.login,
                kazanExpressAccount.keAccountEntity.password
            )
            val checkTokenResponse = kazanExpressLkClient.checkToken(userId, authResponse.accessToken)!!.body!!
            keAccountRepository.save(
                kazanExpressAccount.keAccountEntity.copy(
                    name = checkTokenResponse.firstName,
                    externalAccountId = checkTokenResponse.accountId,
                    email = checkTokenResponse.email,
                )
            )
            keAccountRepository.changeInitializeState(userId, keAccountId, InitializeState.finished)
            updateKeAccountService.executeUpdateJob(userId, keAccountId)
        } catch (e: Exception) {
            log.warn(e) { "Failed to initialize KazanExpress account" }
            keAccountRepository.changeInitializeState(userId, keAccountId, InitializeState.error)
        }

    }
}
