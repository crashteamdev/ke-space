package dev.crashteam.repricer.config.security

import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.security.Principal

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
                if (accountEntity == null) {
                    accountRepository.save(AccountEntity(userId = it.name))
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
