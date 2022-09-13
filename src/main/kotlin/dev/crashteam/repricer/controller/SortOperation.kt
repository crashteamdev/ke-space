package dev.crashteam.repricer.controller

import dev.crashteam.repricer.repository.postgre.SortType
import org.jooq.Record
import org.jooq.TableField

object SortOperation {

    fun parse(
        sort: List<String>,
        mapper: Map<String, ViewFieldToTableFieldMapper<*, *>>
    ): List<Pair<TableField<out Record, out Any?>, SortType>> {
        return sort.mapNotNull { sortField ->
            val tableFieldMapper = mapper[sortField] ?: return@mapNotNull null
            if (sortField.startsWith("-")) {
                tableFieldMapper.tableField() to SortType.DESC
            } else {
                tableFieldMapper.tableField() to SortType.ASC
            }
        }
    }
}
