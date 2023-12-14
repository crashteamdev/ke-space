package dev.crashteam.repricer.config

import dev.crashteam.repricer.config.properties.RepricerProperties
import dev.crashteam.repricer.job.*
import dev.crashteam.repricer.stream.scheduler.PendingMessageScheduler
import org.quartz.*
import org.quartz.impl.JobDetailImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
@ConditionalOnProperty(
    value = ["repricer.jobEnabled"],
    havingValue = "true",
    matchIfMissing = true
)
class JobConfiguration(
    private val repricerProperties: RepricerProperties,
) {

    @Autowired
    private lateinit var schedulerFactoryBean: Scheduler

    @PostConstruct
    fun init() {
        schedulerFactoryBean.addJob(keShopItemMasterJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(SHOP_TRIGGER_KEY))) {
            schedulerFactoryBean.scheduleJob(keShopItemMasterTrigger())
        }
        schedulerFactoryBean.addJob(updateAccountDataMasterJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(ACCOUNT_DATA_UPDATE_TRIGGER_KEY))) {
            schedulerFactoryBean.scheduleJob(updateAccountDataMasterTrigger())
        }
        schedulerFactoryBean.addJob(priceChangeMasterJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(PRICE_CHANGE_TRIGGER_KEY))) {
            schedulerFactoryBean.scheduleJob(priceChangeMasterTrigger())
        }
        schedulerFactoryBean.addJob(keAccountInitializeMasterJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(ACCOUNT_INITIALIZE_TRIGGER_KEY))) {
            schedulerFactoryBean.scheduleJob(keAccountInitializeMasterTrigger())
        }
        schedulerFactoryBean.addJob(repairStuckStateJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(REPAIR_STUCK_STATE_TRIGGER_KEY))) {
            schedulerFactoryBean.scheduleJob(repairStuckStateTrigger())
        }
        schedulerFactoryBean.addJob(pendingMessageJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(PENDING_MESSAGE_JOB, PENDING_MESSAGE_GROUP))) {
            schedulerFactoryBean.scheduleJob(triggerPendingMessageJob())
        }
    }

    fun keShopItemMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey("master-shop-item-job")
        jobDetail.jobClass = KeShopItemMasterJob::class.java

        return jobDetail
    }

    fun keShopItemMasterTrigger(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(keShopItemMasterJob())
            .withIdentity(SHOP_TRIGGER_KEY)
            .withSchedule(CronScheduleBuilder.cronSchedule(repricerProperties.productCron))
            .build()
    }

    fun updateAccountDataMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey("master-account-data-update-job")
        jobDetail.jobClass = UpdateAccountDataMasterJob::class.java

        return jobDetail
    }

    fun updateAccountDataMasterTrigger(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(updateAccountDataMasterJob())
            .withIdentity(ACCOUNT_DATA_UPDATE_TRIGGER_KEY)
            .withPriority(Int.MAX_VALUE)
            .withSchedule(CronScheduleBuilder.cronSchedule(repricerProperties.accountUpdateDataCron))
            .build()
    }

    fun priceChangeMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey("master-price-change-job")
        jobDetail.jobClass = KeShopItemPriceChangeMasterJob::class.java

        return jobDetail
    }

    fun priceChangeMasterTrigger(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(priceChangeMasterJob())
            .withIdentity(PRICE_CHANGE_TRIGGER_KEY)
            .withPriority(Int.MAX_VALUE)
            .withSchedule(CronScheduleBuilder.cronSchedule(repricerProperties.priceChangeCron))
            .build()
    }

    fun keAccountInitializeMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey("master-account-initialize-job")
        jobDetail.jobClass = KeAccountInitializeMasterJob::class.java

        return jobDetail
    }

    fun keAccountInitializeMasterTrigger(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(keAccountInitializeMasterJob())
            .withIdentity(ACCOUNT_INITIALIZE_TRIGGER_KEY)
            .withPriority(Int.MAX_VALUE)
            .withSchedule(CronScheduleBuilder.cronSchedule(repricerProperties.accountInitializeCron))
            .build()
    }

    fun repairStuckStateJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey("repair-stuck-state-job")
        jobDetail.jobClass = RepairStuckStateJob::class.java

        return jobDetail
    }

    fun repairStuckStateTrigger(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(repairStuckStateJob())
            .withIdentity(REPAIR_STUCK_STATE_TRIGGER_KEY)
            .withPriority(Int.MAX_VALUE)
            .withSchedule(CronScheduleBuilder.cronSchedule(repricerProperties.accountInitializeCron))
            .build()
    }

    private fun pendingMessageJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(PENDING_MESSAGE_JOB, PENDING_MESSAGE_GROUP)
        jobDetail.jobClass = PendingMessageScheduler::class.java

        return jobDetail
    }

    private fun triggerPendingMessageJob(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(pendingMessageJob())
            .withIdentity(PENDING_MESSAGE_JOB, PENDING_MESSAGE_GROUP)
            .withSchedule(CronScheduleBuilder.cronSchedule(repricerProperties.pendingMessageCron))
            .withPriority(Int.MAX_VALUE)
            .build()
    }

    companion object {
        const val SHOP_TRIGGER_KEY = "master-shop-item-trigger"
        const val ACCOUNT_DATA_UPDATE_TRIGGER_KEY = "master-account-data-update-trigger"
        const val PRICE_CHANGE_TRIGGER_KEY = "master-price-change-trigger"
        const val PAYMENT_TRIGGER_KEY = "master-payment-trigger"
        const val ACCOUNT_INITIALIZE_TRIGGER_KEY = "master-account-initialize-trigger"
        const val REPAIR_STUCK_STATE_TRIGGER_KEY = "repair-stuck-state-trigger"
        const val PENDING_MESSAGE_JOB = "pendingMessageJob"
        const val PENDING_MESSAGE_GROUP = "pendingMessageGroup"
    }
}
