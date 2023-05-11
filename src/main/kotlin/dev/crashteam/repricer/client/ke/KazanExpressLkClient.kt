package dev.crashteam.repricer.client.ke

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.repricer.client.ke.model.ProxyRequestBody
import dev.crashteam.repricer.client.ke.model.ProxyRequestContext
import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.lk.*
import dev.crashteam.repricer.config.properties.ServiceProperties
import dev.crashteam.repricer.service.util.RandomUserAgent
import mu.KotlinLogging
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URLEncoder
import java.util.*
import kotlin.collections.HashMap


private val log = KotlinLogging.logger {}

@Component
class KazanExpressLkClient(
    private val lkRestTemplate: RestTemplate,
    private val restTemplate: RestTemplate,
    private val serviceProperties: ServiceProperties,
) : KazanExpressClient {

    override fun getAccountShops(userId: String, userToken: String): List<AccountShop> {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<List<AccountShop>>(
                "https://api.business.kazanexpress.ru/api/seller/shop/",
                HttpMethod.GET,
                HttpEntity<Void>(headers)
            )

        return handleResponse(responseEntity)
    }

    override fun getAccountShopItems(
        userId: String,
        userToken: String,
        shopId: Long,
        page: Int
    ): List<AccountShopItem> {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<AccountShopItemWrapper>(
                "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product/getProducts?" +
                        "searchQuery=&filter=active&sortBy=id&order=descending&size=99&page=$page",
                HttpMethod.GET,
                HttpEntity<Void>(headers)
            )

        return handleResponse(responseEntity).productList
    }

    override fun changeAccountShopItemPrice(
        userId: String,
        userToken: String,
        shopId: Long,
        payload: ShopItemPriceChangePayload
    ): Boolean {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<Any>(
                "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product/sendSkuData",
                HttpMethod.POST,
                HttpEntity<ShopItemPriceChangePayload>(payload, headers)
            )
        if (!responseEntity.statusCode.is2xxSuccessful) {
            log.warn {
                "Bad response while trying to change item price." +
                        " statusCode=${responseEntity.statusCode};responseBody=${responseEntity.body};" +
                        "requestBody=${jacksonObjectMapper().writeValueAsString(payload)}"
            }
        }

        return responseEntity.statusCode.is2xxSuccessful
    }

    override fun getProductInfo(userId: String, userToken: String, shopId: Long, productId: Long): AccountProductInfo {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<AccountProductInfo>(
                "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product?productId=$productId",
                HttpMethod.GET,
                HttpEntity<Void>(headers)
            )

        return handleResponse(responseEntity)
    }

    override fun getProductDescription(
        userId: String,
        userToken: String,
        shopId: Long,
        productId: Long
    ): AccountProductDescription {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<AccountProductDescription>(
                "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product/$productId/description-response",
                HttpMethod.GET,
                HttpEntity<Void>(headers)
            )

        return handleResponse(responseEntity)
    }

    override fun auth(userId: String, username: String, password: String): AuthResponse {
        val map = HashMap<String, String>().apply {
            set("grant_type", "password")
            set("username", username)
            set("password", password)
        }

        val urlEncodedString = getUrlEncodedString(map)
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/oauth/token",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to RandomUserAgent.getRandomUserAgent(),
                        "Authorization" to "Basic $basicAuthToken",
                        "Content-Type" to MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                        USER_ID_HEADER to userId
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(urlEncodedString.encodeToByteArray()))
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<AuthResponse>> =
            object : ParameterizedTypeReference<StyxResponse<AuthResponse>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return handleProxyResponse(styxResponse!!)!!
    }

    override fun refreshAuth(userId: String, refreshToken: String): ResponseEntity<AuthResponse> {
        val map = LinkedMultiValueMap<Any, Any>().apply {
            set("grant_type", "refresh_token")
            set("refresh_token", refreshToken)
        }
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $basicAuthToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<AuthResponse>(
                "https://api.business.kazanexpress.ru/api/oauth/token",
                HttpMethod.POST,
                HttpEntity(map, headers)
            )

        return responseEntity
    }

    override fun checkToken(userId: String, token: String): ResponseEntity<CheckTokenResponse> {
        val map = LinkedMultiValueMap<Any, Any>().apply {
            set("token", token)
        }
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $basicAuthToken")
            set("User-Agent", RandomUserAgent.getRandomUserAgent())
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.exchange<CheckTokenResponse>(
                "https://api.business.kazanexpress.ru/api/auth/seller/check_token",
                HttpMethod.POST,
                HttpEntity(map, headers)
            )

        return responseEntity
    }

    private fun <T> handleResponse(responseEntity: ResponseEntity<T>): T {
        val statusCode = responseEntity.statusCode
        val isError = statusCode.series() == HttpStatus.Series.CLIENT_ERROR
                || statusCode.series() == HttpStatus.Series.SERVER_ERROR
        if (isError) {
            throw KazanExpressClientException(statusCode.value())
        }
        return responseEntity.body!!
    }

    private fun <T> handleProxyResponse(styxResponse: StyxResponse<T>): T? {
        val originalStatus = styxResponse.originalStatus
        val statusCode = HttpStatus.resolve(originalStatus)
        val isError = statusCode == null
                || statusCode.series() == HttpStatus.Series.CLIENT_ERROR
                || statusCode.series() == HttpStatus.Series.SERVER_ERROR
        if (isError) {
            throw KazanExpressProxyClientException(
                originalStatus,
                styxResponse.body.toString(),
                "Bad response. StyxStatus=${styxResponse.code}; Status=$originalStatus; Body=${styxResponse.body.toString()}"
            )
        }
        if (styxResponse.code != 0) {
            log.warn { "Bad proxy status - ${styxResponse.code}" }
        }
        return styxResponse.body
    }

    private fun getUrlEncodedString(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true
        for ((key, value) in params) {
            if (first) first = false else result.append("&")
            result.append(URLEncoder.encode(key, "UTF-8"))
            result.append("=")
            result.append(URLEncoder.encode(value, "UTF-8"))
        }
        return result.toString()
    }

    companion object {
        const val basicAuthToken = "a2F6YW5leHByZXNzOnNlY3JldEtleQ=="
        const val USER_ID_HEADER = "X-USER-ID"
        const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36"
    }
}
