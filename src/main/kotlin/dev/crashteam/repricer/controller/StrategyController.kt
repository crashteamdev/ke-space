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
            keShopItemStrategyService.saveStrategy(it)
            val strategy = keShopItemStrategyService.findStrategy(it.keAccountShopItemId)
            val itemStrategy = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.status(HttpStatus.CREATED).body(itemStrategy).toMono()
        }?.doOnError {
            log.warn(it) { "Failed to add strategy" }
        }
    }

    override fun deleteStrategy(
        xRequestID: UUID?,
        keAccountShopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>>? {
        return exchange.getPrincipal<Principal>()?.flatMap {
            keShopItemStrategyService.deleteStrategy(keAccountShopItemId)
            return@flatMap ResponseEntity.noContent().build<Void>().toMono()
        }?.doOnError {
            log.warn(it) { "Failed to delete strategy. keAccountShopItemId=$keAccountShopItemId" }
        }
    }

    override fun getStrategy(
        xRequestID: UUID,
        keAccountShopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccountShopItemStrategy>> {
        return exchange.getPrincipal<Principal>().flatMap {
            val strategy = keShopItemStrategyService.findStrategy(keAccountShopItemId)
            val strategyDto = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.ok().body(strategyDto).toMono()
        }.doOnError {
            log.warn(it) { "Failed to get strategy. keAccountShopItemId=$keAccountShopItemId" }
        }
    }

    override fun patchStrategy(
        xRequestID: UUID,
        keAccountShopItemId: UUID,
        patchStrategy: Mono<PatchStrategy>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccountShopItemStrategy>> {
        return patchStrategy.flatMap {
            keShopItemStrategyService.updateStrategy(keAccountShopItemId, it)
            val strategy = keShopItemStrategyService.findStrategy(keAccountShopItemId)
            val itemStrategy = conversionService.convert(strategy, KeAccountShopItemStrategy::class.java)
            return@flatMap ResponseEntity.ok().body(itemStrategy).toMono()
        }.doOnError {
            log.warn(it) { "Failed to patch strategy. keAccountShopItemId=$keAccountShopItemId" }
        }
    }

    override fun getStrategyTypes(exchange: ServerWebExchange?): Mono<ResponseEntity<Flux<StrategyType>>> {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(StrategyType.values().toList())))
    }
}
