package dev.crashteam.repricer.controller

import org.jooq.Condition
import org.jooq.TableField

enum class FilterOperation(
    val operationName: String,
    val operation: FilterCondition
) {
    EQUALS(
        ":",
        { tableField, value -> if (value is String) tableField.likeIgnoreCase(value) else tableField.eq(value) }),
    NOT_EQUALS("!", { tableField, value -> tableField.notEqual(value) });

    companion object {
        fun parse(filter: String, mapper: Map<String, ViewFieldToTableFieldMapper<*, *>>): Condition? {
            val splitFilterStr = filter.split(";")
            var condition: Condition? = null
            for (partFilter in splitFilterStr) {
                for (filterOperation in values()) {
                    val index = partFilter.indexOf(filterOperation.operationName)
                    if (index > 0) {
                        val fieldName: String = partFilter.substring(0, index)
                        val value: String = partFilter.substring(index + filterOperation.operationName.length)
                        val filterTableMapper = mapper[fieldName]
                        if (filterTableMapper != null) {
                            // If multiple value (OR)
                            if (value.contains(",")) {
                                val values = value.split(",")
                                val tableValues = values.map { filterTableMapper.map(it)!! }
                                var orCondition: Condition? = null
                                for (tableValue in tableValues) {
                                    val tableField = filterTableMapper.tableField()
                                    val cond = filterOperation.operation.condition(
                                        tableField as TableField<*, Any>,
                                        tableValue
                                    )
                                    orCondition = orCondition?.or(cond) ?: cond
                                }
                                condition = orCondition
                            } else {
                                val tableValue = filterTableMapper.map(value)!!
                                val tableField = filterTableMapper.tableField()
                                if (condition != null) {
                                    condition = condition.and(
                                        filterOperation.operation.condition(
                                            tableField as TableField<*, Any>,
                                            tableValue
                                        )
                                    )
                                } else {
                                    condition =
                                        filterOperation.operation.condition(tableField as TableField<*, Any>, value)
                                }
                            }
                        }
                    }
                }
            }
            return condition
        }
    }
}

fun interface FilterCondition {
    fun condition(tableField: TableField<*, Any>, value: Any): Condition
}
