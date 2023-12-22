package dev.crashteam.repricer.service

import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopRepository
import dev.crashteam.repricer.repository.postgre.entity.RestrictionEntity
import dev.crashteam.repricer.restriction.SubscriptionPlanResolver
import org.springframework.stereotype.Service

@Service
class UserService(
    private val keAccountShopRepository: KeAccountShopRepository,
    private val accountRepository: AccountRepository,
    private val subscriptionPlanResolver: SubscriptionPlanResolver
) {
    fun getUserSubscriptionRestrictions(userId: String): RestrictionEntity? {
        val plan = accountRepository.getAccount(userId)?.subscription?.plan
        if (plan != null) {
            val accountRestriction = subscriptionPlanResolver.toAccountRestriction(plan)
            return RestrictionEntity(
                userId = userId,
                keAccountLimit = accountRestriction.keAccountLimit(),
                keAccountLimitCurrent = keAccountShopRepository.countAccounts(userId),
                itemPoolLimit = accountRestriction.itemPoolLimit(),
                itemPoolLimitCurrent = keAccountShopRepository.countKeAccountShopItemsInPool(userId)
            )
        }
        return null
    }

    fun countKeAccountShopItemsInPool(userId: String): Int {
        return keAccountShopRepository.countKeAccountShopItemsInPool(userId)
    }
}