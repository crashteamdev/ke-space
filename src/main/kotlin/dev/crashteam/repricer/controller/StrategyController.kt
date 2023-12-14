package dev.crashteam.repricer.controller

import dev.crashteam.openapi.kerepricer.api.StrategiesApi
import dev.crashteam.openapi.kerepricer.model.AddStrategyRequest
import dev.crashteam.openapi.kerepricer.model.KeAccountShopItemStrategy
import dev.crashteam.openapi.kerepricer.model.PatchStrategy
import dev.crashteam.openapi.kerepricer.model.StrategyType
import dev.crashteam.repricer.service.KeShopItemStrategyService
import mu.KotlinLogging
import org.springframework.core.convert.ConversionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.security.Principal
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1")
class StrategyController(
    private val keShopItemStrategyService: KeShopItemStrategyService,
    private val conversionService: ConversionService
) : StrategiesApi {

    override fun addStrategy(
        xRequestID: UUID?,
        addStrategyRequest: Mono<AddStrategyRequest>?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<KeAccountShopItemStrategy>>? {
        return addStrategyRequest?.flatMap {
            val strategyId = keShopItemStrategyService.saveStrategy(it)
            val strategy = keShopItemStrategyService.findStrategy(strategyId)
            val itemStrategy = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.status(HttpStatus.CREATED).body(itemStrategy).toMono()
        }?.doOnError {
            log.warn(it) { "Failed to add strategy" }
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
            }?.doOnError {
                log.warn(it) { "Failed to delete strategy. strategyId=$shopItemStrategyId" }
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
            exchange?.getPrincipal<Principal>()?.flatMap {
                val strategy = keShopItemStrategyService.findStrategy(shopItemStrategyId)
                val strategyDto = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)
                return@flatMap ResponseEntity.ok().body(strategyDto).toMono()
            }?.doOnError {
                log.warn(it) { "Failed to get strategy. strategyId=$shopItemStrategyId" }
            }
        }
        throw IllegalArgumentException("shopItemStrategyId can't be null")
    }

    override fun patchStrategy(
        xRequestID: UUID?,
        shopItemStrategyId: Long?,
        patchStrategy: Mono<PatchStrategy>?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<KeAccountShopItemStrategy>> {
        if (shopItemStrategyId != null) {
            patchStrategy?.flatMap {
                keShopItemStrategyService.updateStrategy(shopItemStrategyId, it)
                val strategy = keShopItemStrategyService.findStrategy(shopItemStrategyId)
                val itemStrategy = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)
                return@flatMap ResponseEntity.ok().body(itemStrategy).toMono()
            }?.doOnError {
                log.warn(it) { "Failed to patch strategy. strategyId=$shopItemStrategyId" }
            }
        }
        return Mono.just(ResponseEntity.badRequest().build())
    }


    override fun getStrategyTypes(exchange: ServerWebExchange?): Mono<ResponseEntity<Flux<StrategyType>>> {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(StrategyType.values().toList())))
    }
}