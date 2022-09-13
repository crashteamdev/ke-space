package dev.crashteam.repricer.repository.postgre.entity

data class PaginateEntity<T>(
    val item: T,
    val limit: Long,
    val offset: Long,
    val total: Long,
    val row: Long,
)
