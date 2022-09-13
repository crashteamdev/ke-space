package dev.crashteam.repricer.repository.redis

import dev.crashteam.repricer.repository.redis.entity.CookieEntity
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Repository
class UserCookieRepository(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    val hashOperations = redisTemplate.opsForHash<String, String>()

    fun saveCookie(userId: String, cookie: CookieEntity) {
        redisTemplate.execute {
            try {
                log.info { "Save user cookie. userId=$userId; cookie=$cookie" }
                it.multi()
                hashOperations.put("user:$userId", "lkCookieName", cookie.name)
                hashOperations.put("user:$userId", "lkCookieValue", cookie.value)
                hashOperations.put("user:$userId", "lkCookieExpiryAt", cookie.expiryAt.format(DateTimeFormatter.ISO_DATE_TIME))
                redisTemplate.expire("user:$userId", Duration.ofMinutes(30))
                return@execute it.exec()
            } catch (e: Exception) {
                it.discard()
            }
        }
    }

    fun getCookie(userId: String): CookieEntity? {
        val cookieName = hashOperations.get("user:$userId", "lkCookieName")
        val cookieValue = hashOperations.get("user:$userId", "lkCookieValue")
        val cookieExpiryAt = hashOperations.get("user:$userId", "lkCookieExpiryAt")
        if (cookieName == null || cookieValue == null || cookieExpiryAt == null) return null

        return CookieEntity(
            name = cookieName,
            value = cookieValue,
            expiryAt = LocalDateTime.parse(cookieExpiryAt, DateTimeFormatter.ISO_DATE_TIME)
        )
    }

}
