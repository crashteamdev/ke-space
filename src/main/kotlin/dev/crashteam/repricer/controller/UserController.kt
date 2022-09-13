package dev.crashteam.repricer.controller

import dev.crashteam.openapi.kerepricer.api.UserApi
import dev.crashteam.openapi.kerepricer.model.AccountSubscription
import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.repository.postgre.AccountRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.security.Principal
import java.time.ZoneOffset
import java.util.*

@RestController
@RequestMapping("/v1")
class UserController(
    private val accountRepository: AccountRepository
) : UserApi {

    override fun getUserSubscription(
        xRequestID: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<AccountSubscription>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val accountEntity = accountRepository.getAccount(principal.name)
            ?: return@runBlocking ResponseEntity.notFound().build()
        if (accountEntity.subscription == null) {
            return@runBlocking ResponseEntity.notFound().build()
        }
        val accountSubscription = AccountSubscription().apply {
            this.plan = when (accountEntity.subscription.plan) {
                SubscriptionPlan.default_ -> dev.crashteam.openapi.kerepricer.model.SubscriptionPlan.DEFAULT
                SubscriptionPlan.pro -> dev.crashteam.openapi.kerepricer.model.SubscriptionPlan.PRO
                SubscriptionPlan.advanced -> dev.crashteam.openapi.kerepricer.model.SubscriptionPlan.ADVANCED
            }
            this.validUntil = accountEntity.subscriptionValidUntil!!.atOffset(ZoneOffset.UTC)
        }
        ResponseEntity.ok(accountSubscription)
    }.toMono()
}
