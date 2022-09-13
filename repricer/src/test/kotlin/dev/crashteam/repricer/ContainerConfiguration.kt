package dev.crashteam.repricer

import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

@SpringBootTest
class ContainerConfiguration {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            redis.start()
            postgresql.start()
            kafka.start()
        }

        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7.0.4-alpine"))
                .withExposedPorts(6379)

        @JvmStatic
        val postgresql: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:14-alpine").apply {
            withDatabaseName("postgresql")
            withUsername("user")
            withPassword("password")
        }

        @JvmStatic
        val kafka: KafkaContainer = KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka").withTag("7.1.1")
        ).withEmbeddedZookeeper()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            registry.add("spring.datasource.url", postgresql::getJdbcUrl)
            registry.add("spring.datasource.password", postgresql::getPassword)
            registry.add("spring.datasource.username", postgresql::getUsername)
            registry.add("spring.redis.host", redis::getHost)
            registry.add("spring.redis.port", redis::getFirstMappedPort)
            registry.add("repricer.jobEnabled") { false }
            registry.add("repricer.cookieBotProtectionBypassEnabled") { false }
        }
    }
}
