package dev.crashteam.repricer.job

import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.service.KeAccountService
import mu.KotlinLogging
import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class KeAccountInitializeMasterJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val keAccountRepository = applicationContext.getBean(KeAccountRepository::class.java)
        val keAccountService = applicationContext.getBean(KeAccountService::class.java)
        val keAccounts = keAccountRepository.findNotInitializedAccount()
        for (keAccount in keAccounts) {
            val initializeKeAccount =
                keAccountService.initializeKeAccountJob(keAccount.userId, keAccount.keAccountEntity.id!!)
            if (initializeKeAccount) {
                log.info { "KE account initialization job successfully created" }
            } else {
                log.info { "Can't create KE account initialization job. Maybe its already created" }
            }
        }
    }
}
