package dev.crashteam.repricer.filter

import dev.crashteam.repricer.controller.ViewFieldToTableFieldMapper
import dev.crashteam.repricer.db.model.tables.records.KeAccountShopItemRecord

interface FilterRecordMapper {

    fun recordMapper(): Map<String, ViewFieldToTableFieldMapper<KeAccountShopItemRecord, out Comparable<*>>>

}
