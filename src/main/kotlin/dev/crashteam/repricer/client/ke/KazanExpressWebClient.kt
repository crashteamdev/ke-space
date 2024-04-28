package dev.crashteam.repricer.client.ke

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.repricer.client.ke.model.ProxyRequestBody
import dev.crashteam.repricer.client.ke.model.ProxyRequestContext
import dev.crashteam.repricer.client.ke.model.ProxySource
import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.web.*
import dev.crashteam.repricer.config.RedisConfig
import dev.crashteam.repricer.config.properties.ServiceProperties
import dev.crashteam.repricer.service.util.StyxUtils
import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class KazanExpressWebClient(
    private val restTemplate: RestTemplate,
    private val serviceProperties: ServiceProperties
) {

    fun getCategoryGraphQL(categoryId: String, limit: Int = 48, offset: Long = 0): CategoryGQLSearchResponse? {
        val categoryGQLQuery = KazanExpressGQLQuery(
            operationName = "getMakeSearch",
            query = "query getMakeSearch(\$queryInput: MakeSearchQueryInput!) {\n  makeSearch(query: \$queryInput) {\n    id\n    queryId\n    queryText\n    category {\n      ...CategoryShortFragment\n      __typename\n    }\n    categoryTree {\n      category {\n        ...CategoryFragment\n        __typename\n      }\n      total\n      __typename\n    }\n    items {\n      catalogCard {\n        __typename\n        ...SkuGroupCardFragment\n      }\n      __typename\n    }\n    facets {\n      ...FacetFragment\n      __typename\n    }\n    total\n    mayHaveAdultContent\n    categoryFullMatch\n    __typename\n  }\n}\n\nfragment FacetFragment on Facet {\n  filter {\n    id\n    title\n    type\n    measurementUnit\n    description\n    __typename\n  }\n  buckets {\n    filterValue {\n      id\n      description\n      image\n      name\n      __typename\n    }\n    total\n    __typename\n  }\n  range {\n    min\n    max\n    __typename\n  }\n  __typename\n}\n\nfragment CategoryFragment on Category {\n  id\n  icon\n  parent {\n    id\n    __typename\n  }\n  seo {\n    header\n    metaTag\n    __typename\n  }\n  title\n  adult\n  __typename\n}\n\nfragment CategoryShortFragment on Category {\n  id\n  parent {\n    id\n    title\n    __typename\n  }\n  title\n  __typename\n}\n\nfragment SkuGroupCardFragment on SkuGroupCard {\n  ...DefaultCardFragment\n  photos {\n    key\n    link(trans: PRODUCT_540) {\n      high\n      low\n      __typename\n    }\n    previewLink: link(trans: PRODUCT_240) {\n      high\n      low\n      __typename\n    }\n    __typename\n  }\n  badges {\n    ... on BottomTextBadge {\n      backgroundColor\n      description\n      id\n      link\n      text\n      textColor\n      __typename\n    }\n    __typename\n  }\n  characteristicValues {\n    id\n    value\n    title\n    characteristic {\n      values {\n        id\n        title\n        value\n        __typename\n      }\n      title\n      id\n      __typename\n    }\n    __typename\n  }\n  __typename\n}\n\nfragment DefaultCardFragment on CatalogCard {\n  adult\n  favorite\n  feedbackQuantity\n  id\n  minFullPrice\n  minSellPrice\n  offer {\n    due\n    icon\n    text\n    textColor\n    __typename\n  }\n  badges {\n    backgroundColor\n    text\n    textColor\n    __typename\n  }\n  ordersQuantity\n  productId\n  rating\n  title\n  __typename\n}",
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
            proxySource = ProxySource.PROXYS_IO,
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Basic $AUTH_TOKEN",
                        "Content-Type" to MediaType.APPLICATION_JSON_VALUE,
                        "x-iid" to "random_uuid()",
                        "apollographql-client-name" to "web-customers",
                        "apollographql-client-version" to "1.37.0"
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

        return StyxUtils.handleProxyResponse(styxResponse!!)!!.data?.makeSearch
    }

    fun getRootCategories(): RootCategoriesResponse? {
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.kazanexpress.ru/api/main/root-categories",
            httpMethod = "GET",
            proxySource = ProxySource.PROXYS_IO,
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

        return StyxUtils.handleProxyResponse(styxResponse!!)!!
    }

    @Cacheable(value = [RedisConfig.KE_CLIENT_CACHE_NAME], key = "#productId", unless = "#result == null")
    fun getProductInfo(productId: String): ProductResponse? {
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.kazanexpress.ru/api/v2/product/$productId",
            proxySource = ProxySource.PROXYS_IO,
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Basic $AUTH_TOKEN",
                        "x-iid" to "random_uuid()"
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

        return StyxUtils.handleProxyResponse(styxResponse!!)!!
    }

    companion object {
        private const val AUTH_TOKEN = "a2F6YW5leHByZXNzLWN1c3RvbWVyOmN1c3RvbWVyU2VjcmV0S2V5"
        private const val USER_AGENT = "Opera/9.64 (X11; Linux x86_64; U; cs) Presto/2.1.1"
    }

}
