package dev.crashteam.repricer.extensions

import dev.crashteam.repricer.repository.postgre.SortType
import org.jooq.*
import org.jooq.impl.DSL

fun DSLContext.paginate(
    originalSelect: Select<*>,
    sort: List<Pair<Field<*>, SortType>>,
    limit: Long,
    offset: Long
): Select<*> {
    val u: Table<*> = originalSelect.asTable("u")
    val totalRows: Field<Int> = DSL.count().over().`as`("total_rows")
    val sortFields = u.fields(*sort.map { it.first }.toTypedArray())
    val groupBy: Map<String, List<SortType>> = sort.groupBy({ it.first.name }, { it.second })
    val sortFieldOrderBy = sortFields.map {
        when (groupBy[it.name]!!.first()) {
            SortType.ASC -> it.asc()
            SortType.DESC -> it.desc()
        }
    }
    val row: Field<Int> = DSL.rowNumber().over().orderBy(sortFieldOrderBy).`as`("row")
    val t: Table<*> = this
        .select(u.asterisk())
        .select(totalRows, row)
        .from(u)
        .orderBy(sortFieldOrderBy)
        .limit(limit)
        .offset(offset)
        .asTable("t")
    val resultSelect: Select<*> = this
        .select(*t.fields(*originalSelect.select.toTypedArray()) as Array<SelectField<*>>)
        .select(
            t.field(totalRows),
            t.field(row),
        )
        .from(t)
        .orderBy(t.fields(*sort.map { it.first }.toTypedArray()).map {
            when (groupBy[it.name]!!.first()) {
                SortType.ASC -> it.asc()
                SortType.DESC -> it.desc()
            }
        })
    return resultSelect
}
