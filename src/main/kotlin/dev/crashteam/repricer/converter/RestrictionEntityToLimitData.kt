package dev.crashteam.repricer.converter

import dev.crashteam.openapi.kerepricer.model.LimitData
import dev.crashteam.repricer.repository.postgre.entity.RestrictionEntity
import org.springframework.stereotype.Component

@Component
class RestrictionEntityToLimitData : DataConverter<RestrictionEntity, LimitData> {
    override fun convert(source: RestrictionEntity): LimitData {
        return LimitData().apply {
            userId = source.userId
            keAccountLimit = source.keAccountLimit.toLong()
            keAccountLimitCurrent = source.keAccountLimitCurrent.toLong()
            itemPoolLimit = source.itemPoolLimit.toLong()
            itemPoolLimitCurrent = source.itemPoolLimitCurrent.toLong()
        }
    }
}