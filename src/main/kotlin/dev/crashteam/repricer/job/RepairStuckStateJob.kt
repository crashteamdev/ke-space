package dev.crashteam.repricer.job

import dev.crashteam.repricer.db.model.enums.InitializeState
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import java.time.LocalDateTime

@DisallowConcurrentExecution
class RepairStuckStateJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val keAccountRepository = applicationContext.getBean(KeAccountRepository::class.java)
        keAccountRepository.findAccountByUpdateStateInProgressAndLastUpdateLessThan(
            LocalDateTime.now().minusMinutes(60)
        ).forEach {
            keAccountRepository.changeUpdateState(it.userId, it.keAccountEntity.id!!, UpdateState.error)
        }
        keAccountRepository.findAccountByInitializeStateInProgressAndLastUpdateLessThan(
            LocalDateTime.now().minusMinutes(60)
        ).forEach {
            keAccountRepository.changeInitializeState(it.userId, it.keAccountEntity.id!!, InitializeState.error)
        }
    }
}
