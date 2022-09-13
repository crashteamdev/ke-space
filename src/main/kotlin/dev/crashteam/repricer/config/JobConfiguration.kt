package dev.crashteam.repricer.config

import dev.crashteam.repricer.config.properties.RepricerProperties
import dev.crashteam.repricer.job.KeAccountInitializeMasterJob
import dev.crashteam.repricer.job.KeShopItemMasterJob
import dev.crashteam.repricer.job.KeShopItemPriceChangeMasterJob
import dev.crashteam.repricer.job.UpdateAccountDataMasterJob
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
        schedulerFactoryBean.addJob(paymentMasterJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(PAYMENT_TRIGGER_KEY))) {
            schedulerFactoryBean.scheduleJob(paymentMasterTrigger())
        }
        schedulerFactoryBean.addJob(keAccountInitializeMasterJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(ACCOUNT_INITIALIZE_TRIGGER_KEY))) {
            schedulerFactoryBean.scheduleJob(keAccountInitializeMasterTrigger())
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

    fun paymentMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey("master-payment-job")
        jobDetail.jobClass = KeShopItemPriceChangeMasterJob::class.java

        return jobDetail
    }

    fun paymentMasterTrigger(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(paymentMasterJob())
            .withIdentity(PAYMENT_TRIGGER_KEY)
            .withPriority(Int.MAX_VALUE / 2)
            .withSchedule(CronScheduleBuilder.cronSchedule(repricerProperties.paymentCron))
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

    companion object {
        const val SHOP_TRIGGER_KEY = "master-shop-item-trigger"
        const val ACCOUNT_DATA_UPDATE_TRIGGER_KEY = "master-account-data-update-trigger"
        const val PRICE_CHANGE_TRIGGER_KEY = "master-price-change-trigger"
        const val PAYMENT_TRIGGER_KEY = "master-payment-trigger"
        const val ACCOUNT_INITIALIZE_TRIGGER_KEY = "master-account-initialize-trigger"
    }


}
