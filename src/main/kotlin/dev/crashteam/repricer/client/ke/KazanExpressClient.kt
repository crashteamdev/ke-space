package dev.crashteam.repricer.client.ke

import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.lk.*

interface KazanExpressClient {
    fun getAccountShops(userId: String, userToken: String): List<AccountShop>
    fun getAccountShopItems(userId: String, userToken: String, shopId: Long, page: Int = 0): List<AccountShopItem>
    fun changeAccountShopItemPrice(
        userId: String,
        userToken: String,
        shopId: Long,
        payload: ShopItemPriceChangePayload
    ): Boolean

    fun getProductInfo(userId: String, userToken: String, shopId: Long, productId: Long): AccountProductInfo
    fun auth(userId: String, username: String, password: String): AuthResponse
    fun refreshAuth(userId: String, refreshToken: String): StyxResponse<AuthResponse>?
    fun checkToken(userId: String, token: String): StyxResponse<CheckTokenResponse>?
}
