package dev.crashteam.repricer.repository.postgre.entity

data class RestrictionEntity(
        val userId: String,
        val accountLimit: Int,
        val accountLimitCurrent: Int,
        val itemPoolLimit: Int,
        val itemPoolLimitCurrent: Int
)
