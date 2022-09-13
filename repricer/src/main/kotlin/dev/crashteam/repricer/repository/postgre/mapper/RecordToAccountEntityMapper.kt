package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.Account.ACCOUNT
import dev.crashteam.repricer.db.model.tables.Subscription.SUBSCRIPTION
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import dev.crashteam.repricer.repository.postgre.entity.SubscriptionEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToAccountEntityMapper : RecordMapper<AccountEntity> {

    override fun convert(record: Record): AccountEntity {
        return AccountEntity(
            id = record.getValue(ACCOUNT.ID),
            userId = record.getValue(ACCOUNT.USER_ID),
            subscription = record.getValue(SUBSCRIPTION.ID)?.let {
                SubscriptionEntity(
                    id = record.getValue(SUBSCRIPTION.ID),
                    name = record.getValue(SUBSCRIPTION.NAME),
                    plan = record.getValue(SUBSCRIPTION.PLAN),
                    price = record.getValue(SUBSCRIPTION.PRICE)
                )
            },
            subscriptionValidUntil = record.getValue(ACCOUNT.SUBSCRIPTION_VALID_UNTIL),
        )
    }
}
