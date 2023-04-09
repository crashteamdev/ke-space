package dev.crashteam.repricer.config.retry

import mu.KotlinLogging
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.listener.RetryListenerSupport

private val log = KotlinLogging.logger {}

class ErrorRetryListener : RetryListenerSupport() {

    override fun <T : Any?, E : Throwable?> open(context: RetryContext?, callback: RetryCallback<T, E>?): Boolean {
        return super.open(context, callback)
    }

    override fun <T : Any?, E : Throwable?> close(
        context: RetryContext?,
        callback: RetryCallback<T, E>?,
        throwable: Throwable?
    ) {
        super.close(context, callback, throwable)
    }

    override fun <T : Any?, E : Throwable?> onError(
        context: RetryContext,
        callback: RetryCallback<T, E>?,
        throwable: Throwable?
    ) {
        log.error(throwable) { "Exception Occurred. retryCount=${context.retryCount}" }
        super.onError(context, callback, throwable)
    }
}
