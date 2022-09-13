package dev.crashteam.repricer.controller

import org.jooq.Record
import org.jooq.TableField
import java.util.*

interface ViewFieldToTableFieldMapper<R : Record, T> {
    fun tableField(): TableField<R, T>
    fun map(value: String): T
}

class StringTableFieldMapper<R : Record>(
    val tableField: TableField<R, String>
) : ViewFieldToTableFieldMapper<R, String> {
    override fun tableField(): TableField<R, String> {
        return tableField
    }

    override fun map(value: String): String {
        return value
    }
}

class UUIDTableFieldMapper<R : Record>(
    val tableField: TableField<R, UUID>
) : ViewFieldToTableFieldMapper<R, UUID> {
    override fun tableField(): TableField<R, UUID> {
        return tableField
    }

    override fun map(value: String): UUID {
        return UUID.fromString(value)
    }
}

class LongTableFieldMapper<R : Record>(
    val tableField: TableField<R, Long>
) : ViewFieldToTableFieldMapper<R, Long> {
    override fun tableField(): TableField<R, Long> {
        return tableField
    }

    override fun map(value: String): Long {
        return value.toLong()
    }
}

class IntegerTableFieldMapper<R : Record>(
    val tableField: TableField<R, Int>
) : ViewFieldToTableFieldMapper<R, Int> {
    override fun tableField(): TableField<R, Int> {
        return tableField
    }

    override fun map(value: String): Int {
        return value.toInt()
    }
}

