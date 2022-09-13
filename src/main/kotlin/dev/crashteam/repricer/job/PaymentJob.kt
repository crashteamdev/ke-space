package dev.crashteam.repricer.job

import dev.crashteam.repricer.client.youkassa.YooKassaClient
import dev.crashteam.repricer.client.youkassa.model.PaymentStatusResponse
import dev.crashteam.repricer.db.model.enums.PaymentStatus
import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.repository.postgre.PaymentRepository
import dev.crashteam.repricer.service.PaymentService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.quartz.QuartzJobBean
import java.util.*

private val log = KotlinLogging.logger {}

class PaymentJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val paymentRepository = applicationContext.getBean(PaymentRepository::class.java)
        val yooKassaClient = applicationContext.getBean(YooKassaClient::class.java)
        val paymentService = applicationContext.getBean(PaymentService::class.java)
        val retryTemplate = applicationContext.getBean(RetryTemplate::class.java)
        val paymentId = context.jobDetail.jobDataMap["paymentId"] as? UUID
            ?: throw IllegalStateException("paymentId can't be null")
        log.info { "Check payment status paymentId=$paymentId" }
        val paymentEntity = paymentRepository.findById(paymentId)!!
        try {
            val response = retryTemplate.execute<PaymentStatusResponse, Exception> {
                val paymentStatusResponse = yooKassaClient.checkStatus(paymentEntity.externalId)
                paymentStatusResponse
            }
            if (response.status == "canceled") {
                log.info { "Payment canceled paymentId=$paymentId" }
                paymentService.savePaymentAccountSubscriptionState(paymentEntity.copy(status = PaymentStatus.canceled))
            } else if (response.status == "succeeded") {
                log.info { "Payment succeeded paymentId=$paymentId" }
                paymentService.savePaymentAccountSubscriptionState(paymentEntity.copy(status = PaymentStatus.success))
            }
        } catch (e: Exception) {
            log.warn { "Payment failed paymentId=$paymentId" }
            paymentService.savePaymentAccountSubscriptionState(paymentEntity.copy(status = PaymentStatus.error))
        }
    }
}
