package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.enums.PaymentStatus
import dev.crashteam.repricer.db.model.tables.Payment.PAYMENT
import dev.crashteam.repricer.repository.postgre.entity.PaymentEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToPaymentEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class PaymentRepository(
    private val dsl: DSLContext,
    private val recordToPaymentEntityMapper: RecordToPaymentEntityMapper
) {

    fun save(paymentEntity: PaymentEntity): Int {
        val p = PAYMENT
        return dsl.insertInto(
            p,
            p.ID,
            p.USER_ID,
            p.EXTERNAL_ID,
            p.AMOUNT,
            p.STATUS,
            p.SUBSCRIPTION_PLAN,
            p.MULTIPLY
        ).values(
            paymentEntity.id,
            paymentEntity.userId,
            paymentEntity.externalId,
            paymentEntity.amount,
            paymentEntity.status,
            paymentEntity.subscriptionPlan,
            paymentEntity.multiply
        ).onDuplicateKeyUpdate()
            .set(
                mapOf(
                    p.STATUS to paymentEntity.status
                )
            ).execute()
    }

    fun findById(id: UUID): PaymentEntity? {
        val p = PAYMENT
        return dsl.selectFrom(p)
            .where(p.ID.eq(id))
            .fetchOne()?.map { recordToPaymentEntityMapper.convert(it) }
    }

    fun findByExternalId(externalId: String): PaymentEntity? {
        val p = PAYMENT
        return dsl.selectFrom(p)
            .where(p.EXTERNAL_ID.eq(externalId))
            .fetchOne()?.map { recordToPaymentEntityMapper.convert(it) }
    }

    fun findByStatus(status: PaymentStatus): List<PaymentEntity> {
        val p = PAYMENT
        return dsl.selectFrom(p)
            .where(p.STATUS.eq(status))
            .fetch().map { recordToPaymentEntityMapper.convert(it) }
    }

}
