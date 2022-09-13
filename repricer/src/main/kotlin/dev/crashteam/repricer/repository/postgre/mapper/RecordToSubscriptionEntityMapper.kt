package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.Subscription.SUBSCRIPTION
import dev.crashteam.repricer.repository.postgre.entity.SubscriptionEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToSubscriptionEntityMapper : RecordMapper<SubscriptionEntity> {

    override fun convert(record: Record): SubscriptionEntity {
        return SubscriptionEntity(
            id = record.getValue(SUBSCRIPTION.ID),
            name = record.getValue(SUBSCRIPTION.NAME),
            plan = record.getValue(SUBSCRIPTION.PLAN),
            price = record.getValue(SUBSCRIPTION.PRICE),
        )
    }
}
