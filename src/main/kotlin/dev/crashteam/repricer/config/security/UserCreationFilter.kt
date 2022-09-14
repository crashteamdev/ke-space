package dev.crashteam.repricer.config.security

import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.security.Principal

@Component
class UserCreationFilter(
    private val accountRepository: AccountRepository
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        if (principal?.name == null) {
            exchange.response.rawStatusCode = HttpStatus.UNAUTHORIZED.value()
            return@runBlocking exchange.response.setComplete().awaitSingle()
        }
        val accountEntity = accountRepository.getAccount(principal.name)
        if (accountEntity == null) {
            accountRepository.save(AccountEntity(userId = principal.name))
        }
        return@runBlocking chain.filter(exchange).awaitSingle()
    }.toMono()
}
