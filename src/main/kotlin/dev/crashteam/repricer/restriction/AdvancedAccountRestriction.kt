package dev.crashteam.repricer.restriction

import org.springframework.stereotype.Component

@Component
class AdvancedAccountRestriction : AccountRestriction {
    override fun keAccountLimit(): Int = 5

    override fun itemPoolLimit(): Int = 50

    override fun itemCompetitorLimit(): Int = 20
}
