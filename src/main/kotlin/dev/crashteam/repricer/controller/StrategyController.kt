package dev.crashteam.repricer.controller

import dev.crashteam.openapi.kerepricer.api.StrategiesApi
import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.KeAccountShopItemStrategy
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.openapi.kerepricer.model.StrategyType
import dev.crashteam.repricer.service.KeShopItemStrategyService
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
@RequestMapping("/v1")
class StrategyController(
    private val keShopItemStrategyService: KeShopItemStrategyService
) : StrategiesApi {

    override fun addStrategy(
        xRequestID: UUID?,
        addStrategyRequest: Mono<AddStrategyRequest>?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<Long>>? {
        return addStrategyRequest?.flatMap {
            val strategyId = keShopItemStrategyService.saveStrategy(it)
            return@flatMap ResponseEntity.status(HttpStatus.CREATED).body(strategyId).toMono()
        }
    }

    override fun deleteStrategy(
        xRequestID: UUID?,
        shopItemStrategyId: Long?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<Void>>? {
        if (shopItemStrategyId != null) {
            return exchange?.getPrincipal<Principal>()?.flatMap {
                keShopItemStrategyService.deleteStrategy(shopItemStrategyId)
                return@flatMap ResponseEntity.noContent().build<Void>().toMono()
            }
        }
        return Mono.just(ResponseEntity.noContent().build())

    }

    override fun getStrategy(
        xRequestID: UUID?,
        shopItemStrategyId: Long?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<KeAccountShopItemStrategy>> {
        if (shopItemStrategyId != null) {
            val strategy = keShopItemStrategyService.findStrategy(shopItemStrategyId)

            val keAccountShopItemStrategy = KeAccountShopItemStrategy
            keAccountShopItemStrategy.

        }
        throw IllegalArgumentException("shopItemStrategyId can't be null")
    }

    override fun patchStrategy(
        xRequestID: UUID?,
        shopItemStrategyId: Long?,
        patchStrategy: Mono<PatchStrategy>?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<Void>> {
        return super.patchStrategy(xRequestID, shopItemStrategyId, patchStrategy, exchange)
    }

    override fun getStrategyTypes(exchange: ServerWebExchange?): Mono<ResponseEntity<StrategyType>> {
        return super.getStrategyTypes(exchange)
    }
}