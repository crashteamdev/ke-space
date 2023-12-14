package dev.crashteam.repricer.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.crashteam.repricer.config.properties.RedisProperties
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.stream.StreamReceiver
import java.nio.ByteBuffer
import java.time.Duration

private val log = KotlinLogging.logger {}

@Configuration
class RedisConfig(
    private val objectMapper: ObjectMapper,
    private val redisProperties: RedisProperties,
) {

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val redisTemplate: RedisTemplate<String, Any> = RedisTemplate<String, Any>()
        redisTemplate.setConnectionFactory(redisConnectionFactory)
        return redisTemplate
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.redis", value = ["ssl"], havingValue = "true")
    fun builderCustomizer(): LettuceClientConfigurationBuilderCustomizer {
        return LettuceClientConfigurationBuilderCustomizer { builder: LettuceClientConfiguration.LettuceClientConfigurationBuilder ->
            builder.useSsl().disablePeerVerification()
        }
    }

    @Bean
    fun redisCacheManagerBuilderCustomizer(): RedisCacheManagerBuilderCustomizer {
        return RedisCacheManagerBuilderCustomizer { builder: RedisCacheManager.RedisCacheManagerBuilder ->
            val configurationMap: MutableMap<String, RedisCacheConfiguration> =
                HashMap()
            val redisSerializer = object : RedisSerializer<Any> {
                override fun serialize(t: Any?): ByteArray {
                    return objectMapper.writeValueAsBytes(t)
                }
                override fun deserialize(bytes: ByteArray?): Any? {
                    return if (bytes != null) {
                        objectMapper.readValue<Any>(bytes)
                    } else null
                }
            }
            configurationMap[KE_CLIENT_CACHE_NAME] = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .entryTtl(Duration.ofSeconds(120))
            configurationMap[PROXY_SOURCE_NAME] = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .entryTtl(Duration.ofMinutes(40))
            builder.withInitialCacheConfigurations(configurationMap)
        }
    }

    @Bean
    fun paymentSubscription(
        redisConnectionFactory: ReactiveRedisConnectionFactory
    ): StreamReceiver<String, ObjectRecord<String, ByteArray>> {
        val options = StreamReceiver.StreamReceiverOptions.builder().pollTimeout(Duration.ofMillis(100))
            .targetType(ByteArray::class.java).build()
        try {
//            redisConnectionFactory.reactiveConnection.streamCommands().xGroupDestroy(
//                ByteBuffer.wrap(redisProperties.stream.keCategoryInfo.streamName.toByteArray()),
//                redisProperties.stream.keCategoryInfo.consumerGroup
//            )?.subscribe()
            redisConnectionFactory.reactiveConnection.streamCommands().xGroupCreate(
                ByteBuffer.wrap(redisProperties.stream.payment.streamName.toByteArray()),
                redisProperties.stream.payment.consumerGroup,
                ReadOffset.from("0-0"),
                true
            ).subscribe()
        } catch (e: RedisSystemException) {
            log.warn(e) { "Failed to create consumer group: ${redisProperties.stream.payment.consumerGroup}" }
        }
        return StreamReceiver.create(redisConnectionFactory, options)
    }

    companion object {
        const val KE_CLIENT_CACHE_NAME = "ke-products-info"
        const val PROXY_SOURCE_NAME = "proxy-source"
    }

}
