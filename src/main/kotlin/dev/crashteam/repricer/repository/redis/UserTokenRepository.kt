package dev.crashteam.repricer.repository.redis

import dev.crashteam.repricer.repository.redis.entity.UserTokenEntity
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

private val log = KotlinLogging.logger {}

@Repository
class UserTokenRepository(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    val hashOperations = redisTemplate.opsForHash<String, String>()

    fun saveToken(userId: String, keAccountId: String, accessToken: String, refreshToken: String) {
        redisTemplate.execute {
            try {
                log.info { "Save user token. userId=$userId; keAccountId=$keAccountId; accessToken=$accessToken; refreshToken=$refreshToken" }
                it.multi()
                hashOperations.put("user:$userId:$keAccountId", "accessToken", accessToken)
                hashOperations.put("user:$userId:$keAccountId", "refreshToken", refreshToken)
                redisTemplate.expire("user:$userId:$keAccountId", Duration.ofHours(3))
                return@execute it.exec()
            } catch (e: Exception) {
                it.discard()
            }
        }
    }

    fun getToken(userId: String, keAccountId: String): UserTokenEntity? {
        val accessToken = hashOperations.get("user:$userId:$keAccountId", "accessToken")
        val refreshToken = hashOperations.get("user:$userId:$keAccountId", "refreshToken")
        if (accessToken == null || refreshToken == null) return null

        return UserTokenEntity(accessToken, refreshToken)
    }

}
