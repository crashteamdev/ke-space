package dev.crashteam.repricer.config

import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class MdcFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        try {
            MDC.put("RequestId", exchange.request.headers["X-Request-ID"]?.first())
            return chain.filter(exchange)
        } finally {
            MDC.remove("RequestId")
        }
    }
}
