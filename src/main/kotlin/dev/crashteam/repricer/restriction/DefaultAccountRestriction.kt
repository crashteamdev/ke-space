package dev.crashteam.repricer.restriction

import org.springframework.stereotype.Component

@Component
class DefaultAccountRestriction : AccountRestriction {
    override fun keAccountLimit(): Int = 2

    override fun itemPoolLimit(): Int = 20
}
