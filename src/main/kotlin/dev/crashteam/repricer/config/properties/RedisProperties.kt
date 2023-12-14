package dev.crashteam.repricer.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "redis")
data class RedisProperties(
    val stream: RedisStreamProperty
)

data class RedisStreamProperty(
    val maxBatchSize: Int,
    val batchBufferDurationMs: Long,
    val batchParallelCount: Int,
    val payment: RedisStreamPropertyDetail,
)

data class RedisStreamPropertyDetail(
    val streamName: String,
    val consumerGroup: String,
    val consumerName: String,
)
