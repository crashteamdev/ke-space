package dev.crashteam.repricer.config

import dev.crashteam.repricer.proxy.interceptor.CookieHeaderRequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(simpleHttpRequestFactory: ClientHttpRequestFactory): RestTemplate {
        val restTemplate = RestTemplate(simpleHttpRequestFactory)
        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean {
                return false
            }
        }
        return restTemplate
    }

    @Bean
    fun lkRestTemplate(
        proxyHttpRequestFactory: HttpComponentsClientHttpRequestFactory,
        cookieHeaderRequestInterceptor: CookieHeaderRequestInterceptor
    ): RestTemplate {
        val restTemplate = RestTemplate(proxyHttpRequestFactory)
        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean {
                return false
            }
        }
        //restTemplate.interceptors.add(cookieHeaderRequestInterceptor)
        return restTemplate
    }

}
