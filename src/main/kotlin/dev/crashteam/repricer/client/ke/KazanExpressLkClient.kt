package dev.crashteam.repricer.client.ke

import dev.crashteam.repricer.client.ke.model.lk.*
import dev.crashteam.repricer.config.properties.ServiceProperties
import mu.KotlinLogging
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.postForEntity

private val log = KotlinLogging.logger {}

@Component
class KazanExpressLkClient(
    private val lkRestTemplate: RestTemplate,
    private val serviceProperties: ServiceProperties,
) : KazanExpressClient {

    override fun getAccountShops(userId: String, userToken: String): List<AccountShop> {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", USER_AGENT)
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
            set("User-Agent", USER_AGENT)
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
            set("User-Agent", USER_AGENT)
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
                        " statusCode=${responseEntity.statusCode};responseBody=${responseEntity.body}"
            }
        }

        return responseEntity.statusCode.is2xxSuccessful
    }

    override fun getProductInfo(userId: String, userToken: String, shopId: Long, productId: Long): AccountProductInfo {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $userToken")
            set("User-Agent", USER_AGENT)
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

    override fun auth(userId: String, username: String, password: String): AuthResponse {
        val map = LinkedMultiValueMap<Any, Any>().apply {
            set("grant_type", "password")
            set("username", username)
            set("password", password)
        }
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $basicAuthToken")
            set("User-Agent", USER_AGENT)
            set(USER_ID_HEADER, userId)
        }
        val responseEntity =
            lkRestTemplate.postForEntity<AuthResponse>(
                "https://api.business.kazanexpress.ru/api/oauth/token",
                HttpEntity(map, headers)
            )

        return handleResponse(responseEntity)
    }

    override fun refreshAuth(userId: String, refreshToken: String): ResponseEntity<AuthResponse> {
        val map = LinkedMultiValueMap<Any, Any>().apply {
            set("grant_type", "refresh_token")
            set("refresh_token", refreshToken)
        }
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            set("Authorization", "Basic $basicAuthToken")
            set("User-Agent", USER_AGENT)
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
            set("User-Agent", USER_AGENT)
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

    companion object {
        const val basicAuthToken = "a2F6YW5leHByZXNzOnNlY3JldEtleQ=="
        const val USER_ID_HEADER = "X-USER-ID"
        const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36"
    }

}
