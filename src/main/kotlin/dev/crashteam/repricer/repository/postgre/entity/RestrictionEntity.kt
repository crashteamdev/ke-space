package dev.crashteam.repricer.repository.postgre.entity

data class RestrictionEntity(
    val userId: String,
    val keAccountLimit: Int,
    val keAccountLimitCurrent: Int,
    val itemPoolLimit: Int,
    val itemPoolLimitCurrent: Int
)
