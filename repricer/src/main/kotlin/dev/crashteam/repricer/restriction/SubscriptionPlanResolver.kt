package dev.crashteam.repricer.restriction

import dev.crashteam.repricer.db.model.enums.SubscriptionPlan

interface SubscriptionPlanResolver {
    fun toAccountRestriction(subscriptionPlan: SubscriptionPlan): AccountRestriction
}
