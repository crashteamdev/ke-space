package dev.crashteam.repricer.controller

import dev.crashteam.openapi.kerepricer.api.PaymentApi
import dev.crashteam.openapi.kerepricer.model.CreateSubsriptionPayment201Response
import dev.crashteam.openapi.kerepricer.model.CreateSubsriptionPaymentRequest
import dev.crashteam.openapi.kerepricer.model.SubscriptionPlan
import dev.crashteam.repricer.service.PaymentService
import dev.crashteam.repricer.service.error.PaymentRestrictionException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("repricer/v1")
class PaymentController(
    private val paymentService: PaymentService
) : PaymentApi {

    override fun createSubsriptionPayment(
        xRequestID: UUID,
        idempotencyKey: UUID,
        createSubsriptionPaymentRequest: Mono<CreateSubsriptionPaymentRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<CreateSubsriptionPayment201Response>> {
        return exchange.getPrincipal<Principal>().flatMap { principal ->
            createSubsriptionPaymentRequest.flatMap { request ->
                val plan = when (request.subscriptionPlan!!) {
                    SubscriptionPlan.DEFAULT -> dev.crashteam.repricer.db.model.enums.SubscriptionPlan.default_
                    SubscriptionPlan.ADVANCED -> dev.crashteam.repricer.db.model.enums.SubscriptionPlan.advanced
                    SubscriptionPlan.PRO -> dev.crashteam.repricer.db.model.enums.SubscriptionPlan.pro
                }
                try {
                    try {
                        val paymentUrl = paymentService.createPaymentForSubscription(
                            principal.name,
                            request.multiply,
                            request.redirectUrl,
                            plan,
                            idempotencyKey.toString()
                        )
                        val response = CreateSubsriptionPayment201Response().apply {
                            this.paymentUrl = paymentUrl
                        }
                        ResponseEntity.ok(response).toMono()
                    } catch (e: PaymentRestrictionException) {
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build<CreateSubsriptionPayment201Response>().toMono()
                    }
                } catch (e: IllegalArgumentException) {
                    return@flatMap ResponseEntity.badRequest().build<CreateSubsriptionPayment201Response>().toMono()
                }
            }
        }
    }

}
