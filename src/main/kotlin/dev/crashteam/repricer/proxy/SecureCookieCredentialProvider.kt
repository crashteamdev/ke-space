package dev.crashteam.repricer.proxy

import org.apache.http.auth.AuthScope
import org.apache.http.auth.Credentials
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.springframework.stereotype.Component

@Component
class SecureCookieCredentialProvider(
    private val proxyManager: ProxyManager
) : CredentialsProvider {
    override fun setCredentials(authscope: AuthScope, p1: Credentials) {
        TODO("Not yet implemented")
    }

    override fun getCredentials(authscope: AuthScope): Credentials? {
        val proxyAddress = proxyManager.getProxyByHost(authscope.host) ?: return null
        return UsernamePasswordCredentials(proxyAddress.login, proxyAddress.password)
    }

    override fun clear() {
        TODO("Not yet implemented")
    }
}
