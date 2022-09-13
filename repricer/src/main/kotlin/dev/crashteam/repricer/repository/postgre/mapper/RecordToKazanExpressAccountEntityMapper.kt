package dev.crashteam.repricer.repository.postgre.mapper

import dev.crashteam.repricer.db.model.tables.KeAccount.KE_ACCOUNT
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountEntity
import org.jooq.Record
import org.springframework.stereotype.Component

@Component
class RecordToKazanExpressAccountEntityMapper : RecordMapper<KazanExpressAccountEntity> {

    override fun convert(record: Record): KazanExpressAccountEntity {
        return KazanExpressAccountEntity(
            id = record.getValue(KE_ACCOUNT.ID),
            accountId = record.getValue(KE_ACCOUNT.ACCOUNT_ID),
            externalAccountId = record.getValue(KE_ACCOUNT.EXTERNAL_ACCOUNT_ID),
            name = record.getValue(KE_ACCOUNT.NAME),
            login = record.getValue(KE_ACCOUNT.LOGIN),
            password = record.getValue(KE_ACCOUNT.PASSWORD),
            lastUpdate = record.getValue(KE_ACCOUNT.LAST_UPDATE),
            monitorState = record.getValue(KE_ACCOUNT.MONITOR_STATE),
            updateState = record.getValue(KE_ACCOUNT.UPDATE_STATE),
        )
    }
}
