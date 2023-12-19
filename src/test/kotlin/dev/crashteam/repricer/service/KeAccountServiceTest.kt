package dev.crashteam.repricer.service

import dev.crashteam.repricer.ContainerConfiguration
import dev.crashteam.repricer.client.ke.KazanExpressLkClient
import dev.crashteam.repricer.client.ke.model.StyxResponse
import dev.crashteam.repricer.client.ke.model.lk.AuthResponse
import dev.crashteam.repricer.client.ke.model.lk.CheckTokenResponse
import dev.crashteam.repricer.db.model.enums.MonitorState
import dev.crashteam.repricer.db.model.enums.SubscriptionPlan
import dev.crashteam.repricer.repository.postgre.AccountRepository
import dev.crashteam.repricer.repository.postgre.SubscriptionRepository
import dev.crashteam.repricer.repository.postgre.entity.AccountEntity
import dev.crashteam.repricer.service.encryption.PasswordEncryptor
import dev.crashteam.repricer.service.error.AccountItemPoolLimitExceededException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@Testcontainers
@SpringBootTest
class KeAccountServiceTest : ContainerConfiguration() {

    @MockBean
    lateinit var kazanExpressLkClient: KazanExpressLkClient

    @Autowired
    lateinit var keAccountService: KeAccountService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var accountSubscriptionRepository: SubscriptionRepository

    @Autowired
    lateinit var passwordEncryptor: PasswordEncryptor

    val userId = UUID.randomUUID().toString()

    @BeforeEach
    internal fun setUp() {
        whenever(kazanExpressLkClient.auth(anyString(),  anyString(), anyString())).then {
            AuthResponse(
                accessToken = "testAccessToken",
                expiresIn = 99999,
                refreshToken = "testRefreshToken",
                scope = "testScope",
                tokenType = "JWT"
            )
        }
        whenever(kazanExpressLkClient.checkToken(anyString(), anyString())).then {
            StyxResponse(
                code = 200,
                originalStatus = 200,
                url = "testUrl",
                body = CheckTokenResponse(
                    accountId = 1234567 + Random().nextLong(1, 100),
                    active = true,
                    firstName = "testFirstName",
                    email = "testEmail",
                    sellerId = 123456789
                )
            )
        }
        accountRepository.deleteByUserId(userId)
        accountRepository.save(AccountEntity(userId = userId))
    }

    @Test
    fun `add ke account fail case of none subscription`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        assertThrows(AccountItemPoolLimitExceededException::class.java) {
            keAccountService.addKeAccount(userId, login, password)
        }
    }

    @Test
    fun `add ke account`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        val kazanExpressAccountEntity = keAccountService.addKeAccount(userId, login, password)
        val kePassword = Base64.getDecoder().decode(kazanExpressAccountEntity.password)
        val decryptPassword = passwordEncryptor.decryptPassword(kePassword)

        // Then
        assertEquals(login, kazanExpressAccountEntity.login)
        assertEquals(password, decryptPassword)
    }

    @Test
    fun `add ke account limit exceeded for subscription`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        keAccountService.addKeAccount(userId, login, password)
        keAccountService.addKeAccount(userId, "$login-2", password)
        assertThrows(AccountItemPoolLimitExceededException::class.java) {
            keAccountService.addKeAccount(userId, login, password)
        }
    }

    @Test
    fun `edit ke account login password`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"
        val newLogin = "newTestLogin"
        val newPassword = "newTestPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        val kazanExpressAccountEntity = keAccountService.addKeAccount(userId, login, password)
        keAccountService.editKeAccount(userId, kazanExpressAccountEntity.id!!, newLogin, newPassword)
        val keAccount = keAccountService.getKeAccount(userId, kazanExpressAccountEntity.id!!)

        // Then
        assertEquals(newLogin, keAccount?.login)
        val decryptPassword = passwordEncryptor.decryptPassword(Base64.getDecoder().decode(keAccount?.password))
        assertEquals(newPassword, decryptPassword)
    }

    @Test
    fun `change account monitor state`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"
        val nextMonitorState = MonitorState.active

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        val kazanExpressAccountEntity = keAccountService.addKeAccount(userId, login, password)
        keAccountService.changeKeAccountMonitoringState(userId, kazanExpressAccountEntity.id!!, nextMonitorState)
        val keAccount = keAccountService.getKeAccount(userId, kazanExpressAccountEntity.id!!)

        // Then
        assertEquals(nextMonitorState, keAccount?.monitorState)
    }

    @Test
    fun `remove one ke account from user`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        keAccountService.addKeAccount(userId, login, password)
        val keAccount = keAccountService.addKeAccount(userId, "$login-2", password)
        keAccountService.removeKeAccount(userId, keAccount.id!!)
        val keAccountCount = keAccountService.getKeAccounts(userId).size

        // Then
        assertEquals(1, keAccountCount)
    }

    @Test
    fun `purge all user ke accounts`() {
        // Given
        val login = "testLogin"
        val password = "testPassword"

        // When
        val accountEntity = accountRepository.getAccount(userId)!!
        val subscriptionEntity = accountSubscriptionRepository.findSubscriptionByPlan(SubscriptionPlan.default_)
        accountRepository.save(accountEntity.copy(subscription = subscriptionEntity))
        keAccountService.addKeAccount(userId, login, password)
        accountRepository.deleteByUserId(userId)
        val keAccountCount = keAccountService.getKeAccounts(userId).size

        // Then
        assertEquals(0, keAccountCount)
    }

}
