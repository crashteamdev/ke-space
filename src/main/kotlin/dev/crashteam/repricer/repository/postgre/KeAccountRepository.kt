package dev.crashteam.repricer.repository.postgre

import dev.crashteam.repricer.db.model.enums.InitializeState
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.db.model.tables.Account.ACCOUNT
import dev.crashteam.repricer.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountEntity
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountEntityJoinAccountEntity
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountEntityJoinAccountEntityMapper
import dev.crashteam.repricer.repository.postgre.mapper.RecordToKazanExpressAccountEntityMapper
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
class KeAccountRepository(
    private val dsl: DSLContext,
    private val recordToKazanExpressAccountEntityMapper: RecordToKazanExpressAccountEntityMapper,
    private val recordToKazanExpressAccountEntityJoinAccountEntityMapper: RecordToKazanExpressAccountEntityJoinAccountEntityMapper
) {

    fun save(keAccountEntity: KazanExpressAccountEntity): UUID? {
        val k = KE_ACCOUNT
        return dsl.insertInto(
            k,
            k.ID,
            k.ACCOUNT_ID,
            k.EXTERNAL_ACCOUNT_ID,
            k.NAME,
            k.EMAIL,
            k.LOGIN,
            k.PASSWORD,
            k.LAST_UPDATE
        )
            .values(
                keAccountEntity.id,
                keAccountEntity.accountId,
                keAccountEntity.externalAccountId,
                keAccountEntity.name,
                keAccountEntity.email,
                keAccountEntity.login,
                keAccountEntity.password,
                keAccountEntity.lastUpdate
            )
            .onDuplicateKeyUpdate()
            .set(
                mapOf(
                    k.ACCOUNT_ID to keAccountEntity.accountId,
                    k.EXTERNAL_ACCOUNT_ID to keAccountEntity.externalAccountId,
                    k.NAME to keAccountEntity.name,
                    k.EMAIL to keAccountEntity.email,
                    k.LOGIN to keAccountEntity.login,
                    k.PASSWORD to keAccountEntity.password,
                    k.LAST_UPDATE to keAccountEntity.lastUpdate,
                )
            )
            .returningResult(k.ID)
            .fetchOne()!!.getValue(k.ID)
    }

    fun getKazanExpressAccounts(userId: String): List<KazanExpressAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.select()
            .from(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(a.USER_ID.eq(userId))
            .fetch()
        return records.map { recordToKazanExpressAccountEntityMapper.convert(it) }.toList()
    }

    fun getKazanExpressAccountsCount(userId: String): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        return dsl.selectCount()
            .from(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(a.USER_ID.eq(userId))
            .fetchOne(0, Int::class.java) ?: 0
    }

    fun getKazanExpressAccount(userId: String, keAccountId: UUID): KazanExpressAccountEntity? {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val record = dsl.select()
            .from(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId)))
            .fetchOne() ?: return null
        return recordToKazanExpressAccountEntityMapper.convert(record)
    }

    fun getKazanExpressAccount(keAccountId: UUID): KazanExpressAccountEntityJoinAccountEntity? {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val record = dsl.select()
            .from(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(k.ID.eq(keAccountId))
            .fetchOne() ?: return null
        return recordToKazanExpressAccountEntityJoinAccountEntityMapper.convert(record)
    }

    fun changeUpdateState(
        userId: String,
        keAccountId: UUID,
        updateState: UpdateState,
        lastUpdate: LocalDateTime? = null
    ): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val updateStep = dsl.update(k)
            .set(
                mapOf(
                    k.UPDATE_STATE to updateState,
                    k.UPDATE_STATE_LAST_UPDATE to LocalDateTime.now()
                )
            )
        if (lastUpdate != null) {
            updateStep.set(k.LAST_UPDATE, lastUpdate)
        }
        return updateStep.from(a).where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId))).execute()
    }

    fun changeMonitorState(
        userId: String,
        keAccountId: UUID,
        monitorState: MonitorState,
    ): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        return dsl.update(k)
            .set(k.MONITOR_STATE, monitorState)
            .from(a)
            .where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId))).execute()
    }

    fun changeInitializeState(
        userId: String,
        keAccountId: UUID,
        initializeState: InitializeState
    ): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        return dsl.update(k)
            .set(
                mapOf(
                    k.INITIALIZE_STATE to initializeState,
                    k.INITIALIZE_STATE_LAST_UPDATE to LocalDateTime.now()
                )
            )
            .from(a)
            .where(a.USER_ID.eq(userId).and(k.ID.eq(keAccountId))).execute()
    }

    fun removeKazanExpressAccount(userId: String, keAccountId: UUID): Int {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        return dsl.deleteFrom(k)
            .using(a)
            .where(
                k.ID.eq(keAccountId).and(a.USER_ID.eq(userId))
            )
            .execute()
    }

    fun findAccountUpdateNotInProgress(
        lastUpdate: LocalDateTime
    ): MutableList<KazanExpressAccountEntityJoinAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(
                k.INITIALIZE_STATE.eq(InitializeState.finished)
                    .and(k.UPDATE_STATE.notEqual(UpdateState.in_progress))
                    .and(k.LAST_UPDATE.lessThan(lastUpdate))
            )
            .fetch()
        return records.map { recordToKazanExpressAccountEntityJoinAccountEntityMapper.convert(it) }
    }

    fun findAccountByUpdateStateInProgressAndLastUpdateLessThan(
        updateStateLastUpdate: LocalDateTime
    ): List<KazanExpressAccountEntityJoinAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(
                k.UPDATE_STATE.notEqual(UpdateState.in_progress)
                    .and(k.UPDATE_STATE_LAST_UPDATE.lessThan(updateStateLastUpdate))
            )
            .fetch()
        return records.map { recordToKazanExpressAccountEntityJoinAccountEntityMapper.convert(it) }
    }

    fun findAccountWhereMonitorActiveWithValidSubscription(): List<KazanExpressAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(
                k.MONITOR_STATE.eq(MonitorState.active).and(a.SUBSCRIPTION_VALID_UNTIL.lessThan(LocalDateTime.now()))
            )
            .fetch()

        return records.map { recordToKazanExpressAccountEntityMapper.convert(it) }
    }

    fun findNotInitializedAccount(): List<KazanExpressAccountEntityJoinAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(k.INITIALIZE_STATE.eq(InitializeState.not_started))
            .fetch()
        return records.map { recordToKazanExpressAccountEntityJoinAccountEntityMapper.convert(it) }
    }

    fun findAccountByInitializeStateInProgressAndLastUpdateLessThan(
        initializeStateLastUpdate: LocalDateTime
    ): List<KazanExpressAccountEntityJoinAccountEntity> {
        val a = ACCOUNT
        val k = KE_ACCOUNT
        val records = dsl.selectFrom(k.join(a).on(a.ID.eq(k.ACCOUNT_ID)))
            .where(
                k.INITIALIZE_STATE.notEqual(InitializeState.in_progress)
                    .and(k.INITIALIZE_STATE_LAST_UPDATE.lessThan(initializeStateLastUpdate))
            )
            .fetch()
        return records.map { recordToKazanExpressAccountEntityJoinAccountEntityMapper.convert(it) }
    }

}
