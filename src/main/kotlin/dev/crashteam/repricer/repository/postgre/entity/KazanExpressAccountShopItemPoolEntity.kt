package dev.crashteam.repricer.repository.postgre.entity

import java.time.LocalDateTime
import java.util.*

data class KazanExpressAccountShopItemPoolEntity(
    val keAccountShopItemId: UUID,
    val lastCheck: LocalDateTime? = null
)
