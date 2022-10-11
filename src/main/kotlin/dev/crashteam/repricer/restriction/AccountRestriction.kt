package dev.crashteam.repricer.restriction

interface AccountRestriction {

    fun keAccountLimit(): Int

    fun itemPoolLimit(): Int

    fun itemCompetitorLimit(): Int
}
