package dev.crashteam.repricer.client.youkassa

import com.fasterxml.jackson.databind.ObjectMapper
import dev.crashteam.repricer.client.youkassa.model.PaymentRequest
import dev.crashteam.repricer.client.youkassa.model.PaymentResponse
import dev.crashteam.repricer.client.youkassa.model.PaymentStatusResponse
import dev.crashteam.repricer.config.properties.YouKassaProperties
import mu.KotlinLogging
import org.springframework.http.*
import org.springframework.http.HttpStatus.Series
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URI
import java.nio.charset.Charset
import java.util.*

private val log = KotlinLogging.logger {}

@Component
class YooKassaClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val youKassaProperties: YouKassaProperties
) {

    fun createPayment(idempotencyKey: String, paymentRequest: PaymentRequest): PaymentResponse {
        val json = objectMapper.writeValueAsString(paymentRequest)
        val httpHeaders = HttpHeaders().apply {
            val auth = "${youKassaProperties.shopId}:${youKassaProperties.key}"
            val encodedAuth = Base64.getEncoder().encodeToString(
                auth.toByteArray(Charset.forName("US-ASCII"))
            )
            val authHeader = "Basic $encodedAuth"
            set("Authorization", authHeader)
            set("Idempotence-Key", idempotencyKey)
            set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        }
        val requestEntity = RequestEntity(json, httpHeaders, HttpMethod.POST, URI.create(PAYMENTS_URL))
        val responseEntity = restTemplate.exchange<PaymentResponse>(requestEntity)
        checkOnError(responseEntity)

        return responseEntity.body!!
    }

    fun checkStatus(paymentId: String): PaymentStatusResponse {
        val headers = HttpHeaders().apply {
            val auth = "${youKassaProperties.shopId}:${youKassaProperties.key}"
            val encodedAuth = Base64.getEncoder().encodeToString(
                auth.toByteArray(Charset.forName("US-ASCII"))
            )
            val authHeader = "Basic $encodedAuth"
            set("Authorization", authHeader)
            set("Accept", MediaType.APPLICATION_JSON_VALUE)
        }

        val url = PAYMENT_STATUS_URL + paymentId
        val entity = HttpEntity<Any>(headers)
        val responseEntity = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            PaymentStatusResponse::class.java
        )
        checkOnError(responseEntity)

        return responseEntity.body!!
    }

    private fun checkOnError(responseEntity: ResponseEntity<*>) {
        val series = responseEntity.statusCode.series()
        if (series == Series.CLIENT_ERROR || series == Series.SERVER_ERROR) {
            throw YooKassaClientException("Unknown error during request")
        }
    }

    companion object {
        const val PAYMENTS_URL = "https://api.yookassa.ru/v3/payments"
        const val PAYMENT_STATUS_URL = "https://api.yookassa.ru/v3/payments/"
    }
}
