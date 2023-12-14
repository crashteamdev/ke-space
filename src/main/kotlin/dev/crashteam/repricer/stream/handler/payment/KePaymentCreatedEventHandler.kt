package dev.crashteam.repricer.stream.handler.payment

import dev.crashteam.payment.KeRepricerContext
import dev.crashteam.payment.PaymentEvent
import dev.crashteam.repricer.db.model.enums.PaymentStatus
import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.repository.postgre.PaymentRepository
import dev.crashteam.repricer.repository.postgre.entity.PaymentEntity
import kotlinx.coroutines.runBlocking
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import java.util.*

@Component
class KePaymentCreatedEventHandler(
    private val paymentRepository: PaymentRepository,
) : PaymentEventHandler {

    override fun handle(events: List<PaymentEvent>) {
        runBlocking {
            for (paymentEvent in events) {
                val paymentCreated = paymentEvent.payload.paymentChange.paymentCreated
                val paymentId = UUID.fromString(paymentCreated.paymentId)
                val paymentDocument = paymentRepository.findById(paymentId)

                if (paymentDocument != null) continue

                val subscriptionPlan =
                    mapProtoSubscriptionPlan(paymentCreated.userPaidService.paidService.context.keRepricerContext.plan.planCase)
                paymentRepository.save(
                    PaymentEntity(
                        id = paymentId,
                        userId = paymentCreated.userId,
                        externalId = "none",
                        amount = paymentCreated.amount.value,
                        subscriptionPlan = subscriptionPlan,
                        status = PaymentStatus.pending,
                        multiply = paymentCreated.userPaidService.paidService.context.multiply.toShort()
                    )
                )
            }
        }
    }

    override fun isHandle(event: PaymentEvent): Boolean {
        return event.payload.hasPaymentChange() &&
                event.payload.paymentChange.hasPaymentCreated() &&
                event.payload.paymentChange.paymentCreated.hasUserPaidService() &&
                event.payload.paymentChange.paymentCreated.userPaidService.paidService.context.hasKeRepricerContext()
    }

    private fun mapProtoSubscriptionPlan(keRepricerPlan: KeRepricerContext.KeRepricerPlan.PlanCase): SubscriptionPlan {
        return when (keRepricerPlan) {
            KeRepricerContext.KeRepricerPlan.PlanCase.DEFAULT_PLAN -> SubscriptionPlan.default_
            else -> throw IllegalStateException("Unknown plan type: $")
        }
    }
}
