package dev.crashteam.repricer.config

import org.apache.http.HeaderElementIterator
import org.apache.http.client.config.RequestConfig
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeaderElementIterator
import org.apache.http.protocol.HTTP
import org.apache.http.ssl.SSLContexts
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext

@Configuration
class HttpClientConfig {

    @Bean
    fun connectionKeepAliveStrategy(): ConnectionKeepAliveStrategy {
        return ConnectionKeepAliveStrategy { response, context ->
            val it: HeaderElementIterator = BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE))
            while (it.hasNext()) {
                val he = it.nextElement()
                val param = he.name
                val value = he.value
                if (value != null && param.equals("timeout", ignoreCase = true)) {
                    return@ConnectionKeepAliveStrategy value.toLong() * 1000
                }
            }
            DEFAULT_KEEP_ALIVE_TIME_MILLIS.toLong()
        }
    }

    @Bean
    fun sslFactory(): SSLConnectionSocketFactory {
        val acceptingTrustStrategy =
            TrustStrategy { chain: Array<X509Certificate?>?, authType: String? -> true }

        val sslContext: SSLContext = SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy)
            .build()

        return SSLConnectionSocketFactory(sslContext)
    }

    @Bean
    fun httpClient(): CloseableHttpClient {
        val requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(REQUEST_TIMEOUT)
            .setConnectTimeout(CONNECT_TIMEOUT)
            .setSocketTimeout(SOCKET_TIMEOUT)
            .build()
        return HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setKeepAliveStrategy(connectionKeepAliveStrategy())
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .build()
    }

    @Bean
    fun simpleHttpRequestFactory(simpleHttpClient: CloseableHttpClient): HttpComponentsClientHttpRequestFactory {
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        requestFactory.httpClient = simpleHttpClient
        return requestFactory
    }

    companion object {
        private const val CONNECT_TIMEOUT = 30000
        private const val REQUEST_TIMEOUT = 30000
        private const val SOCKET_TIMEOUT = 30000
        private const val DEFAULT_KEEP_ALIVE_TIME_MILLIS = 20 * 1000
    }
}
