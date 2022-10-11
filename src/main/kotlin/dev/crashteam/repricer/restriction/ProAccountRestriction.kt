package dev.crashteam.repricer.restriction

import org.springframework.stereotype.Component

@Component
class ProAccountRestriction : AccountRestriction {
    override fun keAccountLimit(): Int = 10

    override fun itemPoolLimit(): Int = 100

    override fun itemCompetitorLimit(): Int = 30
}
