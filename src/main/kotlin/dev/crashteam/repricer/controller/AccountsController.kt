package dev.crashteam.repricer.controller

import dev.crashteam.openapi.kerepricer.api.AccountsApi
import dev.crashteam.openapi.kerepricer.model.*
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.tables.KeAccountShopItem.KE_ACCOUNT_SHOP_ITEM
import dev.crashteam.repricer.db.model.tables.KeAccountShopItemCompetitor.KE_ACCOUNT_SHOP_ITEM_COMPETITOR
import dev.crashteam.repricer.repository.postgre.KeShopItemPriceHistoryRepository
import dev.crashteam.repricer.service.KeAccountService
import dev.crashteam.repricer.service.KeAccountShopService
import dev.crashteam.repricer.service.KeShopItemService
import dev.crashteam.repricer.service.UpdateKeAccountService
import dev.crashteam.repricer.service.error.AccountItemPoolLimitExceededException
import dev.crashteam.repricer.service.error.UserNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.core.convert.ConversionService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
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

@RestController
@RequestMapping("/v1")
class AccountsController(
    private val keAccountService: KeAccountService,
    private val keAccountShopService: KeAccountShopService,
    private val updateKeAccountService: UpdateKeAccountService,
    private val keShopItemService: KeShopItemService,
    private val keShopItemPriceChangeRepository: KeShopItemPriceHistoryRepository,
    private val conversionService: ConversionService
) : AccountsApi {

    override fun addKeAccount(
        xRequestID: UUID,
        addKeAccountRequest: Mono<AddKeAccountRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccount>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val request = addKeAccountRequest.awaitSingle()
        try {
            val keAccountEntity = keAccountService.addKeAccount(principal.name, request.login, request.password)
            val keAccount = conversionService.convert(keAccountEntity, KeAccount::class.java)
            ResponseEntity.ok(keAccount)
        } catch (e: AccountItemPoolLimitExceededException) {
            return@runBlocking ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        } catch (e: IllegalArgumentException) {
            return@runBlocking ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }.toMono()

    override fun addKeAccountShopItemCompetitor(
        xRequestID: UUID,
        id: UUID,
        addKeAccountShopItemCompetitorRequest: Mono<AddKeAccountShopItemCompetitorRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val request = addKeAccountShopItemCompetitorRequest.awaitSingle()
        keAccountShopService.addShopItemCompetitor(
            principal.name,
            id,
            request.shopItemRef.shopId,
            request.shopItemRef.shopItemId,
            request.competitorProductId.toLong(),
            request.competitorSkuId.toLong()
        )
        ResponseEntity.status(HttpStatus.OK).build<Void>()
    }.toMono()

    override fun addKeAccountShopItemPool(
        xRequestID: UUID,
        id: UUID,
        addKeAccountShopItemPoolRequest: Mono<AddKeAccountShopItemPoolRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val request = addKeAccountShopItemPoolRequest.awaitSingle()
        try {
            keAccountShopService.addShopItemIntoPool(principal.name, id, request.shopId, request.shopItemId)
        } catch (e: AccountItemPoolLimitExceededException) {
            return@runBlocking ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
        } catch (e: IllegalArgumentException) {
            return@runBlocking ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
        }
        ResponseEntity.status(HttpStatus.OK).build()
    }.toMono()

    override fun deleteKeAccount(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val removeKeAccountCount = keAccountService.removeKeAccount(principal.name, id)
        if (removeKeAccountCount > 0) {
            ResponseEntity.ok().build<Void>()
        }
        ResponseEntity.notFound().build<Void>()
    }.toMono()

    override fun getKeAccount(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccount>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val kazanExpressAccountEntity = keAccountService.getKeAccount(principal.name, id)
        val keAccount = conversionService.convert(kazanExpressAccountEntity, KeAccount::class.java)
        ResponseEntity.ok(keAccount)
    }.toMono()

    override fun getKeAccountCompetitorShopItems(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        shopItemId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountCompetitorShopItem>>> = runBlocking {
        val limit = limit ?: 10
        val offset = offset ?: 0
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val mapFields = mapOf(
            "name" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.NAME),
            "productId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.PRODUCT_ID),
            "skuId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM_COMPETITOR.SKU_ID),
            "price" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SELL_PRICE),
            "availableAmount" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
        )
        val filterCondition = filter?.let {
            FilterOperation.parse(filter, mapFields)
        }
        val sortFields = if (sort != null) {
            SortOperation.parse(sort, mapFields)
        } else null

        val shopItemCompetitors = keAccountShopService.getShopItemCompetitors(
            principal.name, id, shopId, shopItemId, filterCondition, sortFields, limit.toLong(), offset.toLong()
        )
        val paginateEntity = shopItemCompetitors.first()
        val httpHeaders = HttpHeaders().apply {
            add("Pagination-Total", paginateEntity.total.toString())
            add("Pagination-Limit", paginateEntity.limit.toString())
            add("Pagination-Offset", paginateEntity.offset.toString())
        }
        ResponseEntity(shopItemCompetitors.map {
            conversionService.convert(
                it.item,
                KeAccountCompetitorShopItem::class.java
            )!!
        }.toFlux(), httpHeaders, HttpStatus.OK)
    }.toMono()

    override fun getAccountShopItemPoolCount(
        xRequestID: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<GetAccountShopItemPoolCount200Response>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val shopItemPoolCount = keAccountShopService.getShopItemPoolCount(principal.name)
        val response = GetAccountShopItemPoolCount200Response().apply {
            this.count = shopItemPoolCount
        }
        ResponseEntity.ok(response)
    }.toMono()

    override fun getKeAccountShopItems(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountShopItem>>> = runBlocking {
        val limit = limit ?: 10
        val offset = offset ?: 0
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val mapFields = mapOf(
            "productId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            "skuId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
            "name" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.NAME),
            "photoKey" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
            "fullPrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.FULL_PRICE),
            "sellPrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SELL_PRICE),
            "barCode" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.BARCODE),
            "availableAmount" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
            "minimumThreshold" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
            "maximumThreshold" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
            "step" to IntegerTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.STEP)
        )
        val filterCondition = filter?.let {
            FilterOperation.parse(filter, mapFields)
        }
        val sortFields = if (sort != null) {
            SortOperation.parse(sort, mapFields)
        } else null
        val shopItemCompetitors = keAccountShopService.getKeAccountShopItems(
            principal.name,
            id,
            shopId,
            filterCondition,
            sortFields,
            limit.toLong(),
            offset.toLong()
        )
        val paginateEntity = shopItemCompetitors.first()
        val httpHeaders = HttpHeaders().apply {
            add("Pagination-Total", paginateEntity.total.toString())
            add("Pagination-Limit", paginateEntity.limit.toString())
            add("Pagination-Offset", paginateEntity.offset.toString())
        }
        val keAccountShopItems =
            shopItemCompetitors.map { it.item }
                .map { conversionService.convert(it, KeAccountShopItem::class.java)!! }
        ResponseEntity(keAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK)
    }.toMono()

    override fun getKeAccountShopItemsPool(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountShopItem>>> = runBlocking {
        val limit = limit ?: 10
        val offset = offset ?: 0
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val mapFields = mapOf(
            "productId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            "skuId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
            "name" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.NAME),
            "photoKey" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
            "fullPrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.FULL_PRICE),
            "sellPrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SELL_PRICE),
            "barCode" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.BARCODE),
            "availableAmount" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
            "minimumThreshold" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.MINIMUM_THRESHOLD),
            "maximumThreshold" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.MAXIMUM_THRESHOLD),
            "step" to IntegerTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.STEP)
        )
        val filterCondition = filter?.let {
            FilterOperation.parse(filter, mapFields)
        }
        val sortFields = if (sort != null) {
            SortOperation.parse(sort, mapFields)
        } else null
        val shopItemPaginateEntities =
            keAccountShopService.getShopItemsInPool(
                principal.name,
                id,
                shopId,
                filterCondition,
                sortFields,
                limit.toLong(),
                offset.toLong()
            )
        val paginateEntity = shopItemPaginateEntities.first()
        val httpHeaders = HttpHeaders().apply {
            add("Pagination-Total", paginateEntity.total.toString())
            add("Pagination-Limit", paginateEntity.limit.toString())
            add("Pagination-Offset", paginateEntity.offset.toString())
        }
        val keAccountShopItems =
            shopItemPaginateEntities.map { it.item }
                .map { conversionService.convert(it, KeAccountShopItem::class.java)!! }
        ResponseEntity(keAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK)
    }.toMono()

    override fun getKeAccountShops(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountShop>>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val kazanExpressAccountShopEntities = keAccountShopService.getKeAccountShops(principal.name, id)
        val keAccountShops =
            kazanExpressAccountShopEntities.map { conversionService.convert(it, KeAccountShop::class.java)!! }
        ResponseEntity.ok(keAccountShops.toFlux())
    }.toMono()

    override fun getKeAccounts(
        xRequestID: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccount>>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val keAccounts = keAccountService.getKeAccounts(principal.name)
        val keAccountList = keAccounts.map { conversionService.convert(it, KeAccount::class.java)!! }
        ResponseEntity.ok(keAccountList.toFlux())
    }.toMono()

    override fun patchKeAccount(
        xRequestID: UUID,
        id: UUID,
        patchKeAccount: Mono<PatchKeAccount>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val request = patchKeAccount.awaitSingle()
        val response = try {
            keAccountService.editKeAccount(principal.name, id, request.login, request.password)
            ResponseEntity.ok().build()
        } catch (e: UserNotFoundException) {
            ResponseEntity.notFound().build<Void>()
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
        response
    }.toMono()

    override fun patchKeAccountMonitoringState(
        xRequestID: UUID,
        id: UUID,
        patchKeAccountMonitoringState: Mono<PatchKeAccountMonitoringState>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val request = patchKeAccountMonitoringState.awaitSingle()
        val monitorState = when (request.state) {
            PatchKeAccountMonitoringState.StateEnum.ACTIVATE -> MonitorState.active
            PatchKeAccountMonitoringState.StateEnum.SUSPEND -> MonitorState.suspended
            else -> throw IllegalArgumentException("Unknown request state: ${request.state}")
        }
        val changeKeAccountMonitoringState =
            keAccountService.changeKeAccountMonitoringState(principal.name, id, monitorState)
        return@runBlocking if (changeKeAccountMonitoringState > 0) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build<Void>()
        }
    }.toMono()

    override fun removeKeAccountShopItemCompetitor(
        xRequestID: UUID,
        id: UUID,
        removeKeAccountShopItemCompetitorRequest: Mono<RemoveKeAccountShopItemCompetitorRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val request = removeKeAccountShopItemCompetitorRequest.awaitSingle()
        val removeShopItemCompetitorCount =
            keAccountShopService.removeShopItemCompetitor(principal.name, id, request.shopItemId, request.competitorId)
        if (removeShopItemCompetitorCount > 0) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build<Void>()
        }
    }.toMono()

    override fun removeKeAccountShopItemFromPool(
        xRequestID: UUID,
        id: UUID,
        addKeAccountShopItemPoolRequest: Mono<AddKeAccountShopItemPoolRequest>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val request = addKeAccountShopItemPoolRequest.awaitSingle()
        val removeShopItemFromPoolCount =
            keAccountShopService.removeShopItemFromPool(principal.name, id, request.shopId, request.shopItemId)
        if (removeShopItemFromPoolCount > 0) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build<Void>()
        }
    }.toMono()

    override fun updateKeAccountData(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val result = withContext(Dispatchers.IO) {
            updateKeAccountService.executeUpdateJob(principal.name, id)
        }
        if (!result) {
            return@runBlocking ResponseEntity(HttpStatus.CONFLICT)
        }
        ResponseEntity.ok().build<Void>()
    }.toMono()

    override fun getKeAccountShopItem(
        xRequestID: UUID,
        id: UUID,
        shopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccountShopItem>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val keAccountShopItem = keAccountShopService.getKeAccountShopItem(principal.name, id, shopItemId)
            ?: return@runBlocking ResponseEntity.notFound().build<KeAccountShopItem>()
        ResponseEntity.ok(conversionService.convert(keAccountShopItem, KeAccountShopItem::class.java))
    }.toMono()

    override fun getKeAccountShopItemPriceChangeHistory(
        xRequestID: UUID,
        id: UUID,
        limit: Int?,
        offset: Int?,
        filter: String?,
        sort: MutableList<String>?,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<KeAccountPriceChangeHistory>>> = runBlocking {
        val limit = limit ?: 10
        val offset = offset ?: 0
        val mapFields = mapOf(
            "productId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PRODUCT_ID),
            "skuId" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.SKU_ID),
            "shopName" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.NAME),
            "itemName" to StringTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.PHOTO_KEY),
            "oldPrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.FULL_PRICE),
            "newPrice" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.BARCODE),
            "barcode" to LongTableFieldMapper(KE_ACCOUNT_SHOP_ITEM.AVAILABLE_AMOUNT),
        )
        val filterCondition = filter?.let {
            FilterOperation.parse(filter, mapFields)
        }
        val sortFields = if (sort != null) {
            SortOperation.parse(sort, mapFields)
        } else null

        val shopItemPriceHistoryPaginateEntities = keShopItemPriceChangeRepository.findHistoryByShopItemId(
            id,
            filterCondition,
            sortFields,
            limit.toLong(),
            offset.toLong()
        )
        val paginateEntity = shopItemPriceHistoryPaginateEntities.first()
        val httpHeaders = HttpHeaders().apply {
            add("Pagination-Total", paginateEntity.total.toString())
            add("Pagination-Limit", paginateEntity.limit.toString())
            add("Pagination-Offset", paginateEntity.offset.toString())
        }
        val keAccountShopItems =
            shopItemPriceHistoryPaginateEntities.map { it.item }
                .map { conversionService.convert(it, KeAccountPriceChangeHistory::class.java)!! }
        ResponseEntity(keAccountShopItems.toFlux(), httpHeaders, HttpStatus.OK)
    }.toMono()

    override fun getKeAccountShopItemSimilar(
        xRequestID: UUID,
        id: UUID,
        shopId: UUID,
        shopItemId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<SimilarItem>>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val keAccountShopItem = keAccountShopService.getKeAccountShopItem(principal.name, id, shopItemId)
            ?: return@runBlocking ResponseEntity.notFound().build<Flux<SimilarItem>>()
        val similarItems =
            keShopItemService.findSimilarItems(keAccountShopItem.productId, keAccountShopItem.skuId).map {
                SimilarItem().apply {
                    this.productId = it.productId
                    this.skuId = it.skuId
                    this.name = it.name
                    this.photoKey = it.photoKey
                }
            }
        ResponseEntity.ok(similarItems.toFlux())
    }.toMono()

    override fun patchKeAccountInitializationState(
        xRequestID: UUID,
        id: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        try {
            val initializeKeAccountJob = withContext(Dispatchers.IO) {
                keAccountService.initializeKeAccountJob(principal.name, id)
            }
            if (initializeKeAccountJob) {
                ResponseEntity.ok().build()
            } else ResponseEntity(HttpStatus.CONFLICT)
        } catch (e: IllegalArgumentException) {
            return@runBlocking ResponseEntity.notFound().build<Void>()
        }
    }.toMono()

    override fun patchKeAccountShopitem(
        xRequestID: UUID,
        id: UUID,
        shopItemId: UUID,
        patchKeAccountShopItem: Mono<PatchKeAccountShopItem>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<KeAccountShopItem>> = runBlocking {
        val principal = exchange.getPrincipal<Principal>().awaitSingle()
        val request = patchKeAccountShopItem.awaitSingle()
        val changeCount = keAccountShopService.changeShopItemPriceOptions(
            id,
            shopItemId,
            request.step,
            request.minimumThreshold,
            request.maximumThreshold
        )
        if (changeCount <= 0) {
            return@runBlocking ResponseEntity.notFound().build<KeAccountShopItem>()
        }
        ResponseEntity.ok().build()
    }.toMono()
}
