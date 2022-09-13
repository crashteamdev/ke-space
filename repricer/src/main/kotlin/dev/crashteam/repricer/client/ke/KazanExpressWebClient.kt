package dev.crashteam.repricer.client.ke

import dev.crashteam.repricer.client.ke.model.ProxyRequestBody
import dev.crashteam.repricer.client.ke.model.ProxyRequestContext
import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.web.ProductResponse
import dev.crashteam.repricer.client.ke.model.web.RootCategoriesResponse
import dev.crashteam.repricer.config.RedisConfig
import dev.crashteam.repricer.config.properties.ServiceProperties
import dev.crashteam.repricerjober.client.model.CategoryResponse
import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

private val log = KotlinLogging.logger {}

@Service
class KazanExpressWebClient(
    private val restTemplate: RestTemplate,
    private val serviceProperties: ServiceProperties
) {

    fun getCategory(categoryId: String, size: Int = 24, page: Int = 0): CategoryResponse? {
        log.debug { "Request category by id. categoryId=$categoryId; size=$size; page=$page" }
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.kazanexpress.ru/api/v2/main/search/product?size=$size&page=$page&categoryId=$categoryId",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                    )
                )
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<CategoryResponse>> =
            object : ParameterizedTypeReference<StyxResponse<CategoryResponse>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!
    }

    fun getRootCategories(): RootCategoriesResponse? {
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.kazanexpress.ru/api/main/root-categories",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Basic $AUTH_TOKEN"
                    )
                )
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<RootCategoriesResponse>> =
            object : ParameterizedTypeReference<StyxResponse<RootCategoriesResponse>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!
    }

    @Cacheable(value = [RedisConfig.KE_CLIENT_CACHE_NAME], key = "#productId", unless="#result == null")
    fun getProductInfo(productId: String): ProductResponse? {
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.kazanexpress.ru/api/v2/product/$productId",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Basic $AUTH_TOKEN"
                    )
                )
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<ProductResponse>> =
            object : ParameterizedTypeReference<StyxResponse<ProductResponse>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!
    }

    private fun <T> handleProxyResponse(styxResponse: StyxResponse<T>): T? {
        val originalStatus = styxResponse.originalStatus
        val statusCode = HttpStatus.resolve(originalStatus)
            ?: throw IllegalStateException("Unknown http status: $originalStatus")
        val isError = statusCode.series() == HttpStatus.Series.CLIENT_ERROR
                || statusCode.series() == HttpStatus.Series.SERVER_ERROR
        if (isError) {
            throw KazanExpressClientException(
                originalStatus,
                styxResponse.body.toString(),
                "Bad response from KazanExpress. Status=$originalStatus; Body=${styxResponse.body.toString()}"
            )
        }
        if (styxResponse.code != 0) {
            log.warn { "Bad proxy status - ${styxResponse.code}" }
        }
        return styxResponse.body
    }

    companion object {
        private const val AUTH_TOKEN = "a2F6YW5leHByZXNzLWN1c3RvbWVyOmN1c3RvbWVyU2VjcmV0S2V5"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.115 Mobile Safari/537.36"
    }

}
