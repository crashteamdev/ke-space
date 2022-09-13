package dev.crashteam.repricer.repository.postgre.entity

import dev.crashteam.repricer.db.model.enums.InitializeState
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.enums.UpdateState
import java.time.LocalDateTime
import java.util.*

data class KazanExpressAccountEntity(
    val id: UUID? = null,
    val accountId: Long,
    val externalAccountId: Long? = null,
    val name: String? = null,
    val email: String? = null,
    val login: String,
    val password: String,
    val lastUpdate: LocalDateTime? = null,
    val monitorState: MonitorState? = null,
    val updateState: UpdateState? = null,
    val initializeState: InitializeState? = null
)
