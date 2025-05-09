package com.amplifyframework.testutils.coroutines

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs a blocking coroutine with a timeout.
 */
fun <T> runBlockingWithTimeout(
    timeout: Duration = 5.seconds,
    block: suspend () -> T
): T = runBlocking {
    withTimeout(timeout) {
        block()
    }
}
