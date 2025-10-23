package com.amplifyframework.testutils.coroutines

import io.kotest.assertions.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Runs a blocking coroutine with a timeout.
 */
fun <T> runBlockingWithTimeout(timeout: Duration = 10.seconds, block: suspend () -> T): T = runBlocking {
    withTimeout(timeout) {
        block()
    }
}

fun <T> runBlockingWithTimeout(timeout: Duration = 10.seconds, message: String, block: suspend () -> T): T =
    runBlocking {
        try {
            withTimeout(timeout) { block() }
        } catch (_: TimeoutCancellationException) {
            fail(message)
        }
    }
