package dev.crashteam.repricer.service

import dev.crashteam.repricer.client.youkassa.YooKassaClient
import dev.crashteam.repricer.client.youkassa.model.PaymentAmount
import dev.crashteam.repricer.client.youkassa.model.PaymentConfirmation
import dev.crashteam.repricer.client.youkassa.model.PaymentRequest
import dev.crashteam.repricer.db.model.enums.PaymentStatus
import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.PaymentRepository
import dev.crashteam.repricer.repository.postgre.SubscriptionRepository
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import dev.crashteam.repricer.repository.postgre.entity.PaymentEntity
import dev.crashteam.repricer.restriction.AccountSubscriptionRestrictionValidator
import dev.crashteam.repricer.service.error.PaymentRestrictionException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class PaymentService(
    private val accountRepository: AccountRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val paymentRepository: PaymentRepository,
    private val youkassaClient: YooKassaClient,
    private val accountSubscriptionRestrictionValidator: AccountSubscriptionRestrictionValidator,
) {

    fun createPaymentForSubscription(
        userId: String,
        paymentMultiply: Int,
        paymentRedirectUrl: String,
        subscriptionPlan: SubscriptionPlan,
        idempotencyKey: String
    ): String {
        val accountEntity = accountRepository.getAccount(userId)!!
        if (accountEntity.subscription != null) {
            val isValidTargetSubscription =
                accountSubscriptionRestrictionValidator.validateChangeSubscriptionLevel(userId, subscriptionPlan)
            if (!isValidTargetSubscription) throw PaymentRestrictionException("Target subscription level exceeds limits")
        }

        val subscriptionEntity = subscriptionRepository.findSubscriptionByPlan(subscriptionPlan)
            ?: throw IllegalArgumentException()
        val price = (subscriptionEntity.price.toBigDecimal() * paymentMultiply.toBigDecimal()).movePointLeft(2)
        val createPaymentResponse = youkassaClient.createPayment(
            idempotencyKey = idempotencyKey,
            paymentRequest = PaymentRequest(
                amount = PaymentAmount(price.movePointLeft(2).toString(), "RUB"),
                capture = true,
                confirmation = PaymentConfirmation("redirect", paymentRedirectUrl),
                createdAt = LocalDateTime.now(),
                description = "Покупка подписки ${subscriptionEntity.name} на $paymentMultiply месяц",
            )
        )
        paymentRepository.save(
            PaymentEntity(
                id = UUID.randomUUID(),
                userId = userId,
                externalId = createPaymentResponse.id,
                amount = price.toLong(),
                subscriptionPlan = subscriptionPlan,
                status = PaymentStatus.pending,
                multiply = paymentMultiply.toShort()
            )
        )

        return createPaymentResponse.confirmation.confirmationUrl
    }

    @Transactional
    fun savePaymentAccountSubscriptionState(paymentEntity: PaymentEntity) {
        if (paymentEntity.status == PaymentStatus.canceled) {
            paymentRepository.save(paymentEntity)
        } else if (paymentEntity.status == PaymentStatus.success) {
            paymentRepository.save(paymentEntity)
            val accountEntity =
                accountRepository.getAccount(paymentEntity.userId) ?: AccountEntity(userId = paymentEntity.userId)
            val subscriptionEntity = subscriptionRepository.findSubscriptionByPlan(paymentEntity.subscriptionPlan)
            val validDays = paymentEntity.multiply * 30L
            accountRepository.save(
                accountEntity.copy(
                    subscription = subscriptionEntity,
                    subscriptionValidUntil = LocalDateTime.now().plusDays(validDays)
                )
            )
        }
    }

}
