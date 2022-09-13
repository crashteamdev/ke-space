package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.db.model.tables.Subscription.SUBSCRIPTION
import dev.crashteam.repricer.repository.postgre.entity.SubscriptionEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToSubscriptionEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class SubscriptionRepository(
    private val dsl: DSLContext,
    private val recordToSubscriptionEntityMapper: RecordToSubscriptionEntityMapper
) {

    fun getAllSubscriptions(): List<SubscriptionEntity> {
        val s = SUBSCRIPTION
        val records = dsl.select().from(s).fetch()
        return records.map { recordToSubscriptionEntityMapper.convert(it) }
    }

    fun findSubscriptionByPlan(subscriptionPlan: SubscriptionPlan): SubscriptionEntity? {
        val s = SUBSCRIPTION
        return dsl.selectFrom(s)
            .where(s.PLAN.eq(subscriptionPlan))
            .fetchOne()?.map { recordToSubscriptionEntityMapper.convert(it) }
    }

}
