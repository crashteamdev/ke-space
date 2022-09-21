package dev.crashteam.repricer.job

import dev.crashteam.repricer.db.model.enums.PaymentStatus
import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.PaymentRepository
import mu.KotlinLogging
import org.quartz.*
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class PaymentMasterJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val paymentRepository = applicationContext.getBean(PaymentRepository::class.java)
        val paymentEntities = paymentRepository.findByStatus(PaymentStatus.pending)
        log.debug { "Found pending payment count=${paymentEntities.size}" }
        for (paymentEntity in paymentEntities) {
            log.info { "Create job for payment. $paymentEntity" }
            val jobIdentity = "${paymentEntity.id}-payment-job"
            val jobDetail =
                JobBuilder.newJob(PaymentJob::class.java).withIdentity(jobIdentity).build()
            val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
                setName(jobIdentity)
                setStartTime(Date())
                setRepeatInterval(0L)
                setRepeatCount(0)
                setPriority(Int.MAX_VALUE / 2)
                setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
                afterPropertiesSet()
            }.getObject()
            jobDetail.jobDataMap["paymentId"] = paymentEntity.id
            try {
                val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
                schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
            } catch (e: ObjectAlreadyExistsException) {
                log.warn { "Task still in progress: $jobIdentity" }
            } catch (e: Exception) {
                log.error(e) { "Failed to start scheduler job" }
            }
        }
    }
}
