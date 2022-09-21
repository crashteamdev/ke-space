package dev.crashteam.repricer.job

import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.extensions.getApplicationContext
import mu.KotlinLogging
import org.quartz.*
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*

private val log = KotlinLogging.logger {}

@DisallowConcurrentExecution
class KeShopItemMasterJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val kazanExpressClient = applicationContext.getBean(KazanExpressWebClient::class.java)
        val categories = kazanExpressClient.getRootCategories()
        val payload = categories?.payload
        if (payload == null) {
            log.warn { "Empty root categories response" }
            return
        }
        for (category in categories.payload) {
            val jobIdentity = "${category.id}-category-job"
            val jobDetail =
                JobBuilder.newJob(KeShopItemJob::class.java).withIdentity(jobIdentity).build()
            val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
                setPriority(1)
                setName(jobIdentity)
                setStartTime(Date())
                setRepeatInterval(0L)
                setRepeatCount(0)
                setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
                afterPropertiesSet()
            }.getObject()
            jobDetail.jobDataMap["categoryId"] = category.id
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
