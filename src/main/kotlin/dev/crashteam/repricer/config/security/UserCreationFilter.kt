package dev.crashteam.repricer.config.security

import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.security.Principal

@Component
class UserCreationFilter(
    private val accountRepository: AccountRepository
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val principal = exchange.getPrincipal<Principal>().block()
        if (principal?.name == null) {
            exchange.response.rawStatusCode = HttpStatus.UNAUTHORIZED.value()
            return exchange.response.setComplete()
        }
        val accountEntity = accountRepository.getAccount(principal.name)
        if (accountEntity == null) {
            accountRepository.save(AccountEntity(userId = principal.name))
        }
        return chain.filter(exchange)
    }
}
