package dev.crashteam.repricer.client.ke

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.repricer.client.ke.model.ProxyRequestBody
import dev.crashteam.repricer.client.ke.model.ProxyRequestContext
import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.lk.*
import dev.crashteam.repricer.config.properties.ServiceProperties
import dev.crashteam.repricer.service.util.StyxUtils
import mu.KotlinLogging
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.util.*


private val log = KotlinLogging.logger {}

@Component
class KazanExpressLkClient(
    private val restTemplate: RestTemplate,
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
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Bearer $userToken",
                        USER_ID_HEADER to userId
                    )
                )
            )
        )

        val responseType: ParameterizedTypeReference<StyxResponse<List<AccountShop>>> =
            object : ParameterizedTypeReference<StyxResponse<List<AccountShop>>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return StyxUtils.handleProxyResponse(styxResponse!!)!!
    }

    override fun getAccountShopItems(
        userId: String,
        userToken: String,
        shopId: Long,
        page: Int
    ): List<AccountShopItem> {

        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product/getProducts?" +
                    "searchQuery=&filter=active&sortBy=id&order=descending&size=99&page=$page",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Bearer $userToken",
                        USER_ID_HEADER to userId
                    )
                )
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<AccountShopItemWrapper>> =
            object : ParameterizedTypeReference<StyxResponse<AccountShopItemWrapper>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return StyxUtils.handleProxyResponse(styxResponse!!)!!.productList
    }

    override fun changeAccountShopItemPrice(
        userId: String,
        userToken: String,
        shopId: Long,
        payload: ShopItemPriceChangePayload
    ): Boolean {
        val body = jacksonObjectMapper().writeValueAsBytes(payload)
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product/sendSkuData",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Bearer $userToken",
                        "Content-Type" to MediaType.APPLICATION_JSON_VALUE,
                        USER_ID_HEADER to userId
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(body))
            )
        )
        log.info { "ITEM PRICE CHANGE REQUEST BODY - ${jacksonObjectMapper().writeValueAsString(proxyRequestBody)}" }
        val responseType: ParameterizedTypeReference<StyxResponse<ShopItemPriceChangePayload>> =
            object : ParameterizedTypeReference<StyxResponse<ShopItemPriceChangePayload>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return StyxUtils.isProxyRequestSuccessful(styxResponse!!, payload)!!
    }

    override fun getProductInfo(userId: String, userToken: String, shopId: Long, productId: Long): AccountProductInfo {

        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product?productId=$productId",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Bearer $userToken",
                        USER_ID_HEADER to userId
                    )
                )
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<AccountProductInfo>> =
            object : ParameterizedTypeReference<StyxResponse<AccountProductInfo>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return StyxUtils.handleProxyResponse(styxResponse!!)!!
    }

    override fun getProductDescription(
        userId: String,
        userToken: String,
        shopId: Long,
        productId: Long
    ): AccountProductDescription {

        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/seller/shop/$shopId/product/$productId/description-response",
            httpMethod = "GET",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Bearer $userToken",
                        USER_ID_HEADER to userId
                    )
                )
            )
        )

        val responseType: ParameterizedTypeReference<StyxResponse<AccountProductDescription>> =
            object : ParameterizedTypeReference<StyxResponse<AccountProductDescription>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return StyxUtils.handleProxyResponse(styxResponse!!)!!
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
                        "User-Agent" to USER_AGENT,
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

        val authResponse = StyxUtils.handleProxyResponse(styxResponse!!)!!
        if (authResponse.error != null) throw KazanExpressAuthException(authResponse.error.description)
        return authResponse
    }

    override fun refreshAuth(userId: String, refreshToken: String): ResponseEntity<AuthResponse> {

        val map = HashMap<String, String>().apply {
            set("grant_type", "refresh_token")
            set("refresh_token", refreshToken)
        }
        val urlEncodedString = getUrlEncodedString(map)
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/oauth/token",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
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

        return ResponseEntity.ok(StyxUtils.handleProxyResponse(styxResponse!!)!!)
    }

    override fun checkToken(userId: String, token: String): ResponseEntity<CheckTokenResponse> {
        val map = HashMap<String, String>().apply {
            set("token", token)
        }
        val urlEncodedString = getUrlEncodedString(map)
        val proxyRequestBody = ProxyRequestBody(
            url = "https://api.business.kazanexpress.ru/api/auth/seller/check_token",
            httpMethod = "POST",
            context = listOf(
                ProxyRequestContext(
                    key = "headers",
                    value = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Authorization" to "Basic $basicAuthToken",
                        "Content-Type" to MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                        USER_ID_HEADER to userId
                    )
                ),
                ProxyRequestContext("content", Base64.getEncoder().encodeToString(urlEncodedString.encodeToByteArray()))
            )
        )
        val responseType: ParameterizedTypeReference<StyxResponse<CheckTokenResponse>> =
            object : ParameterizedTypeReference<StyxResponse<CheckTokenResponse>>() {}
        val styxResponse = restTemplate.exchange(
            "${serviceProperties.proxy!!.url}/v2/proxy",
            HttpMethod.POST,
            HttpEntity<ProxyRequestBody>(proxyRequestBody),
            responseType
        ).body

        return ResponseEntity.ok(StyxUtils.handleProxyResponse(styxResponse!!)!!)
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
