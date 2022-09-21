package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.KeAccount
import dev.crashteam.repricer.db.model.enums.InitializeState
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.enums.UpdateState
import dev.crashteam.repricer.repository.postgre.entity.KazanExpressAccountEntity
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class KeAccountEntityToViewConverter : DataConverter<KazanExpressAccountEntity, KeAccount> {
    override fun convert(source: KazanExpressAccountEntity): KeAccount {
        return KeAccount().apply {
            this.id = source.id
            this.name = source.name
            this.email = source.email
            this.lastUpdate = source.lastUpdate?.atOffset(ZoneOffset.UTC)
            this.monitorState = when (source.monitorState) {
                MonitorState.active -> dev.crashteam.openapi.kerepricer.model.MonitorState.ACTIVE
                MonitorState.suspended -> dev.crashteam.openapi.kerepricer.model.MonitorState.SUSPENDED
                else -> dev.crashteam.openapi.kerepricer.model.MonitorState.SUSPENDED
            }
            this.updateState = when (source.updateState) {
                UpdateState.not_started -> dev.crashteam.openapi.kerepricer.model.UpdateState.NOT_STARTED
                UpdateState.in_progress -> dev.crashteam.openapi.kerepricer.model.UpdateState.IN_PROGRESS
                UpdateState.finished -> dev.crashteam.openapi.kerepricer.model.UpdateState.FINISHED
                UpdateState.error -> dev.crashteam.openapi.kerepricer.model.UpdateState.ERROR
                null -> dev.crashteam.openapi.kerepricer.model.UpdateState.NOT_STARTED
            }
            this.initializeState = when (source.initializeState) {
                InitializeState.not_started -> dev.crashteam.openapi.kerepricer.model.InitializeState.NOT_STARTED
                InitializeState.in_progress -> dev.crashteam.openapi.kerepricer.model.InitializeState.IN_PROGRESS
                InitializeState.finished -> dev.crashteam.openapi.kerepricer.model.InitializeState.FINISHED
                InitializeState.error -> dev.crashteam.openapi.kerepricer.model.InitializeState.ERROR
                null -> dev.crashteam.openapi.kerepricer.model.InitializeState.NOT_STARTED
            }
        }
    }
}
