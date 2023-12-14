package dev.crashteam.repricer.stream.handler.payment

import dev.crashteam.payment.PaymentEvent
import dev.crashteam.payment.PaymentStatus
import dev.crashteam.repricer.repository.postgre.PaymentRepository
import dev.crashteam.repricer.service.PaymentService
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class KePaymentStatusEventHandler(
    private val paymentRepository: PaymentRepository,
    private val paymentService: PaymentService,
    private val conversionService: ConversionService,
) : PaymentEventHandler {

    override fun handle(events: List<PaymentEvent>) {
        runBlocking {
            for (event in events) {
                val paymentStatusChanged = event.payload.paymentChange.paymentStatusChanged
                val paymentId = UUID.fromString(paymentStatusChanged.paymentId)
                val paymentEntity = paymentRepository.findById(paymentId) ?: continue
                when (paymentStatusChanged.status) {
                    PaymentStatus.PAYMENT_STATUS_PENDING -> {}
                    PaymentStatus.PAYMENT_STATUS_CANCELED -> {
                        paymentService.savePaymentAccountSubscriptionState(
                            paymentEntity.copy(status = dev.crashteam.repricer.db.model.enums.PaymentStatus.canceled)
                        )
                    }

                    PaymentStatus.PAYMENT_STATUS_FAILED -> {
                        paymentService.savePaymentAccountSubscriptionState(
                            paymentEntity.copy(status = dev.crashteam.repricer.db.model.enums.PaymentStatus.error)
                        )
                    }

                    PaymentStatus.PAYMENT_STATUS_SUCCESS -> {
                        paymentService.savePaymentAccountSubscriptionState(
                            paymentEntity.copy(status = dev.crashteam.repricer.db.model.enums.PaymentStatus.success)
                        )
                    }

                    PaymentStatus.PAYMENT_STATUS_UNKNOWN, PaymentStatus.UNRECOGNIZED -> {
                        log.warn { "Received payment event with unknown status: ${paymentStatusChanged.status}" }
                    }
                }
            }
        }
    }

    override fun isHandle(event: PaymentEvent): Boolean {
        return event.payload.hasPaymentChange() && event.payload.paymentChange.hasPaymentStatusChanged()
    }
}
