package dev.crashteam.repricer.job

import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.service.KeAccountService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import java.util.*

private val log = KotlinLogging.logger {}

class UpdateAccountDataJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val keAccountRepository = applicationContext.getBean(KeAccountRepository::class.java)
        val keAccountService = applicationContext.getBean(KeAccountService::class.java)
        val userId = context.jobDetail.jobDataMap["userId"] as? String
            ?: throw IllegalStateException("userId can't be null")
        val keAccountId = context.jobDetail.jobDataMap["keAccountId"] as? UUID
            ?: throw IllegalStateException("keAccountId can't be null")
        try {
            log.info { "Execute update ke account job. userId=$userId;keAccountId=$keAccountId" }
            keAccountRepository.changeUpdateState(userId, keAccountId, UpdateState.in_progress)
            keAccountService.syncAccount(userId, keAccountId)
        } catch (e: Exception) {
            log.warn(e) { "Failed to update account data for userId=$userId;keAccountId=$keAccountId"  }
            keAccountRepository.changeUpdateState(userId, keAccountId, UpdateState.error)
        }
    }
}
