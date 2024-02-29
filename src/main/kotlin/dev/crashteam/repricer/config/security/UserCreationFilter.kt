package dev.crashteam.repricer.config.security

import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import dev.crashteam.repricer.repository.postgre.entity.SubscriptionEntity
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.*
import reactor.core.publisher.Mono
import java.security.Principal
import java.time.LocalDateTime

@Component
class UserCreationFilter(
    private val accountRepository: AccountRepository
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return exchange.getPrincipal<Principal>().doOnSuccess {
            if (it?.name == null) {
                throw IllegalStateException("User not authorized")
            }
        }.flatMap {
            if (it is JwtAuthenticationToken) {
                val accountEntity = accountRepository.getAccount(it.name)
                if (accountEntity?.subscription == null || accountEntity.subscriptionValidUntil?.isBefore(LocalDateTime.now()) == true) {
                    accountRepository.save(
                        AccountEntity(
                            userId = it.name,
                            subscription = SubscriptionEntity(
                                id = 1,
                                "Базовый",
                                SubscriptionPlan.default_,
                                price = 600000
                            ),
                            subscriptionValidUntil = LocalDateTime.now().plusDays(60)
                        )
                    )
                }
            }
            chain.filter(exchange)
        }.onErrorResume {
            if (it is IllegalStateException) {
                exchange.response.rawStatusCode = HttpStatus.UNAUTHORIZED.value()
                exchange.response.setComplete()
            } else {
                if (it is ResponseStatusException) {
                    exchange.response.rawStatusCode = it.rawStatusCode
                    exchange.response.setComplete()
                } else {
                    val serverWebInputException = it as? ServerWebInputException
                    exchange.response.rawStatusCode =
                        serverWebInputException?.status?.value() ?: HttpStatus.INTERNAL_SERVER_ERROR.value()
                    exchange.response.setComplete()
                }
            }
        }
    }
}
