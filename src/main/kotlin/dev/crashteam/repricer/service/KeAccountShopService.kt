package dev.crashteam.repricer.service

import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.db.model.tables.Account
import dev.crashteam.repricer.db.model.tables.Subscription
import dev.crashteam.repricer.repository.postgre.*
import dev.crashteam.repricer.repository.postgre.entity.*
import dev.crashteam.repricer.restriction.AccountSubscriptionRestrictionValidator
import dev.crashteam.repricer.restriction.SubscriptionPlanResolver
import dev.crashteam.repricer.service.error.AccountItemCompetitorLimitExceededException
import dev.crashteam.repricer.service.error.AccountItemPoolLimitExceededException
import dev.crashteam.repricer.service.error.CompetitorItemAlreadyExistsException
import mu.KotlinLogging
import org.jooq.Condition
import org.jooq.Field
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class KeAccountShopService(
    private val keAccountShopRepository: KeAccountShopRepository,
    private val keAccountShopItemRepository: KeAccountShopItemRepository,
    private val keAccountShopItemPoolRepository: KeAccountShopItemPoolRepository,
    private val keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository,
    private val keShopItemRepository: KeShopItemRepository,
    private val kazanExpressWebClient: KazanExpressWebClient,
    private val keShopItemService: KeShopItemService,
    private val accountSubscriptionRestrictionValidator: AccountSubscriptionRestrictionValidator
) {

    fun getKeAccountShops(userId: String, keAccountId: UUID): List<KazanExpressAccountShopEntityWithData> {
        log.debug { "Get ke account shops. userId=$userId; keAccountId=${keAccountId}" }
        return keAccountShopRepository.getKeAccountShopsWithData(userId, keAccountId)
    }

    fun getKeAccountShopItem(
        userId: String,
        keAccountId: UUID,
        shopItemId: UUID,
    ): KazanExpressAccountShopItemEntity? {
        log.debug {
            "Get ke account shop item. userId=$userId; keAccountId=${keAccountId}; shopItemId=${shopItemId}"
        }
        return keAccountShopItemRepository.findShopItem(userId, keAccountId, shopItemId)
    }

    fun getKeAccountShopItems(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<KazanExpressAccountShopItemEntity>> {
        log.debug {
            "Get ke account shop items. userId=$userId; keAccountId=${keAccountId};" +
                    " keAccountShopId=$keAccountShopId; limit=$limit; offset=$offset"
        }
        return keAccountShopItemRepository.findShopItems(
            keAccountId,
            keAccountShopId,
            filter,
            sortFields,
            limit,
            offset
        )
    }

    @Transactional
    fun addShopItemIntoPool(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        keAccountShopItemId: UUID
    ) {
        log.debug {
            "Add shop item into pool. userId=$userId; keAccountId=${keAccountId}; keAccountShopId=$keAccountShopId"
        }
        val isValidPoolItemCount = accountSubscriptionRestrictionValidator.validateItemInPoolCount(userId)

        if (!isValidPoolItemCount)
            throw AccountItemPoolLimitExceededException("Pool limit exceeded for user. userId=$userId")

        val kazanExpressAccountShopItemPoolEntity = KazanExpressAccountShopItemPoolEntity(keAccountShopItemId)
        keAccountShopItemPoolRepository.save(kazanExpressAccountShopItemPoolEntity)
    }

    @Transactional
    fun addShopItemIntoPoolBulk(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        keAccountShopItemIds: List<UUID>,
    ) {
        log.debug {
            "Add shop items into pool. userId=$userId; keAccountId=${keAccountId};" +
                    " keAccountShopId=$keAccountShopId itemSize=${keAccountShopItemIds.stream()}"
        }
        val isValidPoolItemCount = accountSubscriptionRestrictionValidator.validateItemInPoolCount(userId)

        if (!isValidPoolItemCount)
            throw AccountItemPoolLimitExceededException("Pool limit exceeded for user. userId=$userId")

        val kazanExpressAccountShopItemPoolEntities =
            keAccountShopItemIds.map { KazanExpressAccountShopItemPoolEntity(it) }
        keAccountShopItemPoolRepository.saveBatch(kazanExpressAccountShopItemPoolEntities)
    }

    @Transactional
    fun addShopItemCompetitor(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        keAccountShopItemId: UUID,
        productId: Long,
        skuId: Long
    ) {
        log.debug {
            "Add shop item competitor. userId=$userId; keAccountId=$keAccountId;" +
                    " keAccountShopId=${keAccountShopId}; keAccountShopItemId=${keAccountShopItemId}"
        }
        val isValidCompetitorItemCount =
            accountSubscriptionRestrictionValidator.validateItemCompetitorCount(userId, keAccountShopItemId)
        if (!isValidCompetitorItemCount)
            throw AccountItemCompetitorLimitExceededException("Pool limit exceeded for user. userId=$userId")

        val shopItemCompetitor =
            keAccountShopItemCompetitorRepository.findShopItemCompetitorForUpdate(keAccountShopItemId, productId, skuId)
        if (shopItemCompetitor != null) {
            throw CompetitorItemAlreadyExistsException()
        }

        val kazanExpressAccountShopItemEntity =
            keAccountShopItemRepository.findShopItem(keAccountId, keAccountShopId, keAccountShopItemId)
                ?: throw IllegalArgumentException(
                    "Not found shop item." +
                            " keAccountId=${keAccountId};keAccountShopId=${keAccountShopId};keAccountShopItemId=${keAccountShopItemId}"
                )
        val shopItemEntity = keShopItemRepository.findByProductIdAndSkuId(productId, skuId)
        if (shopItemEntity == null) {
            val productInfo = kazanExpressWebClient.getProductInfo(productId.toString())
            if (productInfo?.payload == null) {
                throw IllegalArgumentException("Not found shop item by productId=$productId; skuId=$skuId")
            }
            val productData = productInfo.payload.data
            keShopItemService.addShopItemFromKeData(productData)
        }
        val kazanExpressAccountShopItemCompetitorEntity = KazanExpressAccountShopItemCompetitorEntity(
            id = UUID.randomUUID(),
            keAccountShopItemId = kazanExpressAccountShopItemEntity.id,
            productId = productId,
            skuId = skuId,
        )
        keAccountShopItemCompetitorRepository.save(kazanExpressAccountShopItemCompetitorEntity)
    }

    fun removeShopItemCompetitor(
        userId: String,
        keAccountId: UUID,
        keAccountShopItemId: UUID,
        competitorId: UUID,
    ): Int {
        log.debug {
            "Remove shop item competitor. userId=$userId; keAccountId=$keAccountId;" +
                    " keAccountShopItemId=$keAccountShopItemId; competitorId=$competitorId"
        }
        return keAccountShopItemCompetitorRepository.removeShopItemCompetitor(keAccountShopItemId, competitorId)
    }

    fun getShopItemCompetitors(
        userId: String,
        keAccountId: UUID,
        keShopId: UUID,
        keAccountShopItemId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<KazanExpressAccountShopItemCompetitorEntityJoinKeShopItemEntity>> {
        log.debug {
            "Get shop item competitors. userId=$userId; keAccountId=$keAccountId;" +
                    " keShopId=$keShopId; keAccountShopItemId=$keAccountShopItemId; limit=$limit; offset=$offset"
        }
        return keAccountShopItemCompetitorRepository.findShopItemCompetitors(
            keAccountShopItemId,
            filter,
            sortFields,
            limit,
            offset
        )
    }

    fun getShopItemPoolCount(userId: String): Int {
        log.debug { "Get user shop item pool count. userId=$userId" }
        return keAccountShopItemPoolRepository.findCountShopItemsInPoolForUser(userId)
    }

    fun getShopItemsInPool(
        userId: String,
        keAccountId: UUID,
        keShopId: UUID,
        filter: Condition? = null,
        sortFields: List<Pair<Field<*>, SortType>>? = null,
        limit: Long,
        offset: Long
    ): List<PaginateEntity<KazanExpressAccountShopItemEntity>> {
        log.debug { "Get user shop items in pool. userId=$userId; keAccountId=$keAccountId; keShopId=$keShopId" }
        return keAccountShopItemPoolRepository.findShopItemInPool(
            userId,
            keAccountId,
            keShopId,
            filter,
            sortFields,
            limit,
            offset
        )
    }

    fun removeShopItemFromPool(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        keShopItemId: UUID,
    ): Int {
        log.debug {
            "Remove shop item from pool. userId=$userId; keAccountId=$keAccountId;" +
                    " keAccountShopId=$keAccountShopId; keShopItemId=$keShopItemId;"
        }
        return keAccountShopItemPoolRepository.delete(keShopItemId)
    }

    fun removeShopItemsFromPool(
        userId: String,
        keAccountId: UUID,
        keAccountShopId: UUID,
        keShopItemIds: List<UUID>,
    ): Int {
        log.debug {
            "Remove shop items from pool. userId=$userId; keAccountId=$keAccountId;" +
                    " keAccountShopId=$keAccountShopId; keShopItemIdsCount=${keShopItemIds.size};"
        }
        return keAccountShopItemPoolRepository.delete(keShopItemIds)
    }

    fun changeShopItemPriceOptions(
        keAccountId: UUID,
        keAccountShopItemId: UUID,
        step: Int,
        minimumThreshold: Long,
        maximumThreshold: Long,
        discount: BigDecimal
    ): Int {
        log.debug { "Change shop item price options. keAccountId=$keAccountId; keAccountShopItemId=$keAccountShopItemId" }
        return keAccountShopItemRepository.updatePriceChangeOptions(
            keAccountId,
            keAccountShopItemId,
            step,
            minimumThreshold,
            maximumThreshold,
            discount
        )
    }

}
