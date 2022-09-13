package dev.crashteam.repricer.client.ke

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.repricer.client.ke.model.ProxyRequestBody
import dev.crashteam.repricer.client.ke.model.ProxyRequestContext
import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.lk.*
import dev.crashteam.repricer.config.properties.ServiceProperties
import dev.crashteam.repricer.extensions.toUrlParams
import mu.KotlinLogging
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class KazanExpressLkClient(
    private val lkRestTemplate: RestTemplate,
    private val serviceProperties: ServiceProperties,
) : KazanExpressClient {

    override fun getAccountShops(userId: String, userToken: String): List<AccountShop> {
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/seller/shop/",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "Authorization" to "Basic $userToken",
                    )
                )
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<List<AccountShop>>> =
            object : ParameterizedTypeReference<StyxResponse<List<AccountShop>>>() {}
        val httpHeaders = HttpHeaders().apply {
            set(USER_ID_HEADER, userId)
        }
        val styxResponse = lkRestTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody, httpHeaders),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!
    }

    override fun getAccountShopItems(userId: String, userToken: String, shopId: Long, page: Int): List<AccountShopItem> {
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/seller/shop/7424/product/getProducts?" +
                    "searchQuery=&filter=active&sortBy=id&order=descending&size=99&page=$page",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "Authorization" to "Basic $userToken",
                    )
                )
            )
        )
        val httpHeaders = HttpHeaders().apply {
            set(USER_ID_HEADER, userId)
        }
        val responseType: ParameterizedTypeReference<StyxResponse<AccountShopItemResponse>> =
            object : ParameterizedTypeReference<StyxResponse<AccountShopItemResponse>>() {}
        val styxResponse = lkRestTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody, httpHeaders),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!.productList
    }

    override fun changeAccountShopItemPrice(
        userId: String,
        userToken: String,
        shopId: Long,
        payload: ShopItemPriceChangePayload
    ): Boolean {
        val requestBody = jacksonObjectMapper().writeValueAsBytes(payload)
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product/sendSkuData",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "Authorization" to "Basic $userToken",
                        "Content-Type" to MediaType.APPLICATION_JSON_VALUE
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(requestBody))
            )
        )
        val httpHeaders = HttpHeaders().apply {
            set(USER_ID_HEADER, userId)
        }
        val responseType: ParameterizedTypeReference<StyxResponse<Any>> =
            object : ParameterizedTypeReference<StyxResponse<Any>>() {}
        val styxResponse = lkRestTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody, httpHeaders),
            responseType
        ).body
        val statusCode = HttpStatus.resolve(styxResponse!!.originalStatus)!!

        return statusCode.is2xxSuccessful
    }

    override fun getProductInfo(userId: String, userToken: String, shopId: Long, productId: Long): AccountProductInfo {
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product?productId=$productId",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "Authorization" to "Basic $userToken",
                    )
                )
            )
        )
        val httpHeaders = HttpHeaders().apply {
            set(USER_ID_HEADER, userId)
        }
        val responseType: ParameterizedTypeReference<StyxResponse<AccountProductInfo>> =
            object : ParameterizedTypeReference<StyxResponse<AccountProductInfo>>() {}
        val styxResponse = lkRestTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody, httpHeaders),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!
    }

    override fun auth(userId: String, username: String, password: String): AuthResponse {
        val map = mapOf(
            "grant_type" to "password",
            "username" to username,
            "password" to password
        )
        val urlParams = map.toUrlParams()
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/oauth/token",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "Authorization" to "Basic $basicAuthToken",
                        "Content-Type" to MediaType.APPLICATION_FORM_URLENCODED
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(urlParams.encodeToByteArray()))
            )
        )
        val httpHeaders = HttpHeaders().apply {
            set(USER_ID_HEADER, userId)
        }
        val responseType: ParameterizedTypeReference<StyxResponse<AuthResponse>> =
            object : ParameterizedTypeReference<StyxResponse<AuthResponse>>() {}
        val styxResponse = lkRestTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody, httpHeaders),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!
    }

    override fun refreshAuth(userId: String, refreshToken: String): StyxResponse<AuthResponse>? {
        val map = mapOf(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken
        )
        val urlParams = map.toUrlParams()
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/oauth/token",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "Authorization" to "Basic $basicAuthToken",
                        "Content-Type" to MediaType.APPLICATION_FORM_URLENCODED
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(urlParams.encodeToByteArray()))
            )
        )
        val httpHeaders = HttpHeaders().apply {
            set(USER_ID_HEADER, userId)
        }
        val responseType: ParameterizedTypeReference<StyxResponse<AuthResponse>> =
            object : ParameterizedTypeReference<StyxResponse<AuthResponse>>() {}
        val styxResponse = lkRestTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody, httpHeaders),
            responseType
        ).body

        return styxResponse
    }

    override fun checkToken(userId: String, token: String): StyxResponse<CheckTokenResponse>? {
        val map = mapOf(
            "token" to token
        )
        val urlParams = map.toUrlParams()
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/oauth/token",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "Authorization" to "Basic $basicAuthToken",
                        "Content-Type" to MediaType.APPLICATION_FORM_URLENCODED
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(urlParams.encodeToByteArray()))
            )
        )
        val httpHeaders = HttpHeaders().apply {
            set(USER_ID_HEADER, userId)
        }
        val responseType: ParameterizedTypeReference<StyxResponse<CheckTokenResponse>> =
            object : ParameterizedTypeReference<StyxResponse<CheckTokenResponse>>() {}
        val styxResponse = lkRestTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody, httpHeaders),
            responseType
        ).body

        return styxResponse
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
        const val basicAuthToken = "Basic a2F6YW5leHByZXNzOnNlY3JldEtleQ=="
        const val USER_ID_HEADER = "X-USER-ID"
    }

}
