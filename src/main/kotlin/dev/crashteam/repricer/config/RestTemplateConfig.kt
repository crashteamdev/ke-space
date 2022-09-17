package dev.crashteam.repricer.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(requestFactory: ClientHttpRequestFactory): RestTemplate {
        val restTemplate = RestTemplate(requestFactory)
        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean {
                return false
            }
        }
        return restTemplate
    }

    @Bean
    fun lkRestTemplate(
        requestFactory: ClientHttpRequestFactory,
    ): RestTemplate {
        val restTemplate = RestTemplate(requestFactory)
        restTemplate.errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean {
                return false
            }
        }
        return restTemplate
    }

}
