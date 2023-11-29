package dev.crashteam.repricer.service.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.crashteam.repricer.client.ke.KazanExpressProxyClientException
import dev.crashteam.repricer.client.ke.model.StyxResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus

private val log = KotlinLogging.logger {}

object StyxUtils {
     fun <T> handleProxyResponse(styxResponse: StyxResponse<T>): T? {
        val originalStatus = styxResponse.originalStatus
        val statusCode = HttpStatus.resolve(originalStatus)
        val isError = statusCode == null
                || statusCode.series() == HttpStatus.Series.CLIENT_ERROR
                || statusCode.series() == HttpStatus.Series.SERVER_ERROR
        log.debug { "Styx response: $styxResponse" }
        if (isError) {
            throw KazanExpressProxyClientException(
                originalStatus,
                styxResponse.body.toString(),
                "Bad response. StyxStatus=${styxResponse.code}; Status=$originalStatus; Body=${styxResponse.body.toString()}"
            )
        }
        if (styxResponse.code != 0) {
            log.warn { "Bad proxy status - ${styxResponse.code}" }
        }
        return styxResponse.body
    }

    fun <T> isProxyRequestSuccessful(styxResponse: StyxResponse<T>, request: Any): Boolean? {
        val originalStatus = styxResponse.originalStatus
        val statusCode = HttpStatus.resolve(originalStatus)
        if (statusCode?.is2xxSuccessful == false) {
            log.warn {
                "Bad response while using proxy." +
                        " statusCode=${statusCode};responseBody=${styxResponse.body};" +
                        "requestBody=${jacksonObjectMapper().writeValueAsString(request)}"
            }
        }
        return statusCode?.is2xxSuccessful;
    }
}