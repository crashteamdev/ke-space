package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.Payment.PAYMENT
import dev.crashteam.repricer.repository.postgre.entity.PaymentEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToPaymentEntityMapper : RecordMapper<PaymentEntity> {

    override fun convert(record: Record): PaymentEntity {
        return PaymentEntity(
            id = record.getValue(PAYMENT.ID),
            userId = record.getValue(PAYMENT.USER_ID),
            externalId = record.get(PAYMENT.EXTERNAL_ID),
            amount = record.get(PAYMENT.AMOUNT),
            subscriptionPlan = record.get(PAYMENT.SUBSCRIPTION_PLAN),
            status = record.get(PAYMENT.STATUS),
            multiply = record.get(PAYMENT.MULTIPLY)
        )
    }
}
