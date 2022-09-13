package dev.crashteam.repricer.repository.postgre.mapper

interface RecordMapper<T> {
    fun convert(record: org.jooq.Record): T
}
