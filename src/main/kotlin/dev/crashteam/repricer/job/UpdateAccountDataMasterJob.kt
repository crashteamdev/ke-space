package dev.crashteam.repricer.job

import dev.crashteam.repricer.config.properties.RepricerProperties
import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.service.UpdateKeAccountService
import mu.KotlinLogging
import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class UpdateAccountDataMasterJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val keAccountRepository = applicationContext.getBean(KeAccountRepository::class.java)
        val updateKeAccountService = applicationContext.getBean(UpdateKeAccountService::class.java)
        val repricerProperties = applicationContext.getBean(RepricerProperties::class.java)
        val keAccountUpdateInProgressCount = keAccountRepository.findAccountUpdateInProgressCount()

        if (keAccountUpdateInProgressCount >= (repricerProperties.maxUpdateInProgress ?: 3)) {
            log.info { "Too much account update in progress - $keAccountUpdateInProgressCount" }
            return
        }

        val kazanExpressAccountEntities =
            keAccountRepository.findAccountUpdateNotInProgress(LocalDateTime.now().minusHours(6))
        log.info { "Execute update account job for ${kazanExpressAccountEntities.size} ke account" }
        val accountToUpdateList = kazanExpressAccountEntities.stream()
            .limit(repricerProperties.maxUpdateInProgress?.toLong() ?: 3L).toList()
        for (kazanExpressAccountEntity in accountToUpdateList) {
            val updateJob = updateKeAccountService.executeUpdateJob(
                kazanExpressAccountEntity.userId,
                kazanExpressAccountEntity.keAccountEntity.id!!
            )
            if (!updateJob) {
                log.info {
                    "Update ke account job already started for account" +
                            " userId=${kazanExpressAccountEntity.userId}; keAccountId=${kazanExpressAccountEntity.keAccountEntity.id}"
                }
            } else {
                log.info {
                    "Add update ke account job " +
                            "userId=${kazanExpressAccountEntity.userId}; keAccountId=${kazanExpressAccountEntity.keAccountEntity.id}"
                }
            }
        }
    }
}
