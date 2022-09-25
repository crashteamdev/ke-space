package dev.crashteam.repricer.client.ke

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.repricer.client.ke.model.ProxyRequestBody
import dev.crashteam.repricer.client.ke.model.ProxyRequestContext
import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.web.*
import dev.crashteam.repricer.config.RedisConfig
import dev.crashteam.repricer.config.properties.ServiceProperties
import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class KazanExpressWebClient(
    private val restTemplate: RestTemplate,
    private val serviceProperties: ServiceProperties
) {

    val categoryGraphQlQuery = lazy {
        val resource = ClassPathResource(
            "graphql/category-graphql-query.txt"
        ).file
        String(Files.readAllBytes(resource.toPath()))
    }

    fun getCategoryGraphQL(categoryId: String, limit: Int = 48, offset: Long = 0): CategoryGQLSearchResponse? {
        val categoryGQLQuery = KazanExpressGQLQuery(
            operationName = "getMakeSearch",
            query = categoryGraphQlQuery.value,
            variables = CategoryGQLQueryVariables(
                queryInput = CategoryGQLQueryInput(
                    categoryId = categoryId,
                    pagination = CategoryGQLQueryInputPagination(
                        offset = offset,
                        limit = limit
                    ),
                    showAdultContent = "TRUE",
                    sort = "BY_RELEVANCE_DESC"
                )
            )
        )
        val query = jacksonObjectMapper().writeValueAsBytes(categoryGQLQuery)
        val proxyRequestBody = ProxyRequestBody(
            url = "https://dshop.kznexpress.ru/",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Basic $AUTH_TOKEN",
                        "Content-Type" to MediaType.APPLICATION_JSON_VALUE,
                        "x-request-id" to UUID.randomUUID().toString()
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(query))
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<CategoryGQLResponseWrapper>> =
            object : ParameterizedTypeReference<StyxResponse<CategoryGQLResponseWrapper>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!.data?.makeSearch
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

    @Cacheable(value = [RedisConfig.KE_CLIENT_CACHE_NAME], key = "#productId", unless = "#result == null")
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
            throw KazanExpressProxyClientException(
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
