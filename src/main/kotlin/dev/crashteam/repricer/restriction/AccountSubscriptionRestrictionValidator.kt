package dev.crashteam.repricer.restriction

import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemPoolRepository
import org.springframework.stereotype.Component

@Component
class AccountSubscriptionRestrictionValidator(
    private val accountRepository: AccountRepository,
    private val keAccountShopItemPoolRepository: KeAccountShopItemPoolRepository,
    private val keAccountRepository: KeAccountRepository,
    private val subscriptionPlanResolver: SubscriptionPlanResolver
) {

    fun validateItemInPoolCount(userId: String): Boolean {
        val accountEntity = accountRepository.getAccount(userId)!!

        if (accountEntity.subscription == null) return false

        val itemsInPoolCount = keAccountShopItemPoolRepository.findCountShopItemsInPoolForUser(userId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(accountEntity.subscription.plan)
        val itemPoolLimit = accountRestriction.itemPoolLimit()
        val poolLimitExceeded = itemsInPoolCount >= itemPoolLimit

        return !poolLimitExceeded
    }

    fun validateKeAccountCount(userId: String): Boolean {
        val accountEntity = accountRepository.getAccount(userId)!!

        if (accountEntity.subscription == null) return false

        val keAccountCount = keAccountRepository.getKazanExpressAccountsCount(userId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(accountEntity.subscription.plan)
        val keAccountLimit = accountRestriction.keAccountLimit()
        val isKeAccountLimitExceeded = keAccountCount >= keAccountLimit

        return !isKeAccountLimitExceeded
    }

    fun validateChangeSubscriptionLevel(userId: String, targetSubscriptionPlan: SubscriptionPlan): Boolean {
        val keAccountCount = keAccountRepository.getKazanExpressAccountsCount(userId)
        val itemsInPoolCount = keAccountShopItemPoolRepository.findCountShopItemsInPoolForUser(userId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(targetSubscriptionPlan)
        val keAccountLimit = accountRestriction.keAccountLimit()
        val itemPoolLimit = accountRestriction.itemPoolLimit()
        if (keAccountCount > keAccountLimit) {
            return false
        }
        if (itemsInPoolCount > itemPoolLimit) {
            return false
        }
        return true
    }

}
