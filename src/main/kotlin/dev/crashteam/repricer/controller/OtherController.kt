package dev.crashteam.repricer.controller

import dev.crashteam.openapi.kerepricer.api.SimilarApi
import dev.crashteam.openapi.kerepricer.model.SimilarItem
import dev.crashteam.repricer.service.KeShopItemService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.security.Principal
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1")
class OtherController(
    private val keShopItemService: KeShopItemService
) : SimilarApi {

    override fun getSimilarProducts(
        xRequestID: UUID,
        productId: Long,
        skuId: Long,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<SimilarItem>>> {
        return exchange.getPrincipal<Principal>().flatMap<ResponseEntity<Flux<SimilarItem>>?> { principal ->
            log.info { "Find similar items by productId=$productId; skuId=$skuId" }
            val similarItems = keShopItemService.findSimilarItemsByProductIdSkuId(productId, skuId).map {
                SimilarItem().apply {
                    this.productId = it.productId
                    this.skuId = it.skuId
                    this.name = it.name
                    this.photoKey = it.photoKey
                }
            }
            ResponseEntity.ok(similarItems.toFlux()).toMono()
        }.doOnError {
            log.warn(it) {
                "Failed to get similar items. productId=$productId;skuId=$skuId;"
            }
        }
    }
}
