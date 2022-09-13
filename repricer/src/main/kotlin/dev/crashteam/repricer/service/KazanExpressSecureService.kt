package dev.crashteam.repricer.service

import dev.crashteam.repricer.client.ke.KazanExpressLkClient
import dev.crashteam.repricer.client.ke.model.lk.AccountProductInfo
import dev.crashteam.repricer.client.ke.model.lk.AccountShop
import dev.crashteam.repricer.client.ke.model.lk.AccountShopItem
import dev.crashteam.repricer.client.ke.model.lk.ShopItemPriceChangePayload
import dev.crashteam.repricer.repository.postgre.KeAccountRepository
import dev.crashteam.repricer.repository.redis.UserTokenRepository
import dev.crashteam.repricer.service.encryption.PasswordEncryptor
import dev.crashteam.repricer.service.error.KazanExpressUserAuthException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.*

@Component
class KazanExpressSecureService(
    private val kazanExpressClient: KazanExpressLkClient,
    private val accountRepository: KeAccountRepository,
    private val userTokenRepository: UserTokenRepository,
    private val passwordEncryptor: PasswordEncryptor,
) {

    fun getAccountShops(userId: String, keAccountId: UUID): List<AccountShop> {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.getAccountShops(userId, userToken)
    }

    fun getAccountShopItems(userId: String, keAccountId: UUID, shopId: Long, page: Int): List<AccountShopItem> {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.getAccountShopItems(userId, userToken, shopId, page)
    }

    fun getProductInfo(userId: String, keAccountId: UUID, shopId: Long, productId: Long): AccountProductInfo {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.getProductInfo(userId, userToken, shopId, productId)
    }

    fun changeAccountShopItemPrice(
        userId: String,
        keAccountId: UUID,
        shopId: Long,
        payload: ShopItemPriceChangePayload
    ): Boolean {
        val userToken = authUser(userId, keAccountId)
        return kazanExpressClient.changeAccountShopItemPrice(userId, userToken, shopId, payload)
    }

    fun authUser(
        userId: String,
        keAccountId: UUID,
    ): String {
        val userTokenEntity = userTokenRepository.getToken(userId, keAccountId.toString())
        val accessToken = userTokenEntity?.accessToken
        val refreshToken = userTokenEntity?.refreshToken
        val recentUserToken = if (accessToken != null && refreshToken != null) {
            val checkTokenResponseStyxResponse = kazanExpressClient.checkToken(userId, accessToken)!!
            val httpSeries = HttpStatus.Series.resolve(checkTokenResponseStyxResponse.code)
            if (httpSeries == HttpStatus.Series.CLIENT_ERROR) {
                val refreshAuthResponse = kazanExpressClient.refreshAuth(userId, refreshToken)!!
                val refreshAuthHttpSeries = HttpStatus.Series.resolve(refreshAuthResponse.code)
                if (refreshAuthHttpSeries == HttpStatus.Series.CLIENT_ERROR) {
                    val kazanExpressAccount = accountRepository.getKazanExpressAccount(userId, keAccountId)
                        ?: throw KazanExpressUserAuthException("Not found user by userId=$userId; keAccountId=$keAccountId")
                    val authResponse = kazanExpressClient.auth(userId, kazanExpressAccount.name!!, kazanExpressAccount.name)
                    RecentAuthUserToken(authResponse.accessToken, authResponse.refreshToken)
                } else {
                    RecentAuthUserToken(refreshAuthResponse.body!!.accessToken, refreshAuthResponse.body.refreshToken)
                }
            } else {
                RecentAuthUserToken(accessToken, refreshToken)
            }
        } else {
            val kazanExpressAccount = accountRepository.getKazanExpressAccount(userId, keAccountId)
                ?: throw KazanExpressUserAuthException("Not found user by userId=$userId; keAccountId=$keAccountId")
            val kePassword = Base64.getDecoder().decode(kazanExpressAccount.password)
            val password = passwordEncryptor.decryptPassword(kePassword)
            val authResponse = kazanExpressClient.auth(userId, kazanExpressAccount.login, password)
            RecentAuthUserToken(authResponse.accessToken, authResponse.refreshToken)
        }
        userTokenRepository.saveToken(
            userId,
            keAccountId.toString(),
            recentUserToken.accessToken,
            recentUserToken.refreshToken
        )

        return recentUserToken.accessToken
    }

    private data class RecentAuthUserToken(
        val accessToken: String,
        val refreshToken: String,
    )
}
