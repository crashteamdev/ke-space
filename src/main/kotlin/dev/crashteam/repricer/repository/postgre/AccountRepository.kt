package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.tables.Account.ACCOUNT
import dev.crashteam.repricer.db.model.tables.Subscription.SUBSCRIPTION
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToAccountEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AccountRepository(
    private val dsl: DSLContext,
    private val recordToAccountEntityMapper: RecordToAccountEntityMapper
) {

    fun save(accountEntity: AccountEntity) {
        val a = ACCOUNT
        dsl.insertInto(a, a.USER_ID, a.SUBSCRIPTION_ID, a.SUBSCRIPTION_VALID_UNTIL)
            .values(accountEntity.userId, accountEntity.subscription?.id, accountEntity.subscriptionValidUntil)
            .onConflict(a.USER_ID)
            .doUpdate().set(
                mapOf(
                    a.SUBSCRIPTION_ID to accountEntity.subscription?.id,
                    a.SUBSCRIPTION_VALID_UNTIL to accountEntity.subscriptionValidUntil
                )
            ).execute()
    }

    fun getAccount(userId: String): AccountEntity? {
        val a = ACCOUNT
        val s = SUBSCRIPTION
        val record = dsl.selectFrom(a.leftJoin(s).on(s.ID.eq(a.SUBSCRIPTION_ID)))
            .where(a.USER_ID.eq(userId))
            .fetchOne() ?: return null
        return recordToAccountEntityMapper.convert(record)
    }

    fun deleteByUserId(userId: String): Int {
        val a = ACCOUNT
        return dsl.deleteFrom(a).where(a.USER_ID.eq(userId)).execute()
    }

}
