package dev.crashteam.repricer.restriction

import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemCompetitorRepository
import dev.crashteam.repricer.repository.postgre.KeAccountShopItemPoolRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AccountSubscriptionRestrictionValidator(
    private val accountRepository: AccountRepository,
    private val keAccountShopItemPoolRepository: KeAccountShopItemPoolRepository,
    private val keAccountRepository: KeAccountRepository,
    private val keAccountShopItemCompetitorRepository: KeAccountShopItemCompetitorRepository,
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

    fun validateItemCompetitorCount(userId: String, shopItemId: UUID): Boolean {
        val accountEntity = accountRepository.getAccount(userId)!!

        if (accountEntity.subscription == null) return false

        val itemCompetitorsCount =
            keAccountShopItemCompetitorRepository.findShopItemCompetitorsCount(shopItemId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(accountEntity.subscription.plan)
        val itemCompetitorLimit = accountRestriction.itemCompetitorLimit()
        val isItemCompetitorLimitExceeded = itemCompetitorsCount >= itemCompetitorLimit

        return !isItemCompetitorLimitExceeded
    }

    fun validateChangeSubscriptionLevel(userId: String, targetSubscriptionPlan: SubscriptionPlan): Boolean {
        val keAccountCount = keAccountRepository.getKazanExpressAccountsCount(userId)
        val itemsInPoolCount = keAccountShopItemPoolRepository.findCountShopItemsInPoolForUser(userId)
        val accountRestriction = subscriptionPlanResolver.toAccountRestriction(targetSubscriptionPlan)
        // TODO: add item competitor limit
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
