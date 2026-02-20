package com.amplifyframework.recordcache

import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AutoFlushScheduler(
    val interval: FlushStrategy.Interval,
    val client: RecordClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger: Logger = AmplifyLogging.logger<AutoFlushScheduler>()
    private val scope = CoroutineScope(dispatcher + CoroutineName("AutoFlushScheduler"))
    private var flushJob: Job? = null

    fun start() {
        flushJob?.cancel()
        flushJob = scope.launch {
            run()
        }
    }

    fun disable() {
        flushJob?.cancel()
        flushJob = null
    }

    private suspend fun run() {
        while (true) {
            delay(interval.interval)
            try {
                val result = client.flush()
                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    logger.debug { "Auto-flush completed: ${data.recordsFlushed} records flushed" }
                } else {
                    // Expected failures (network, throttling, etc.) - will retry on next cycle
                    logger.warn(result.exceptionOrNull()) { "Auto-flush failed" }
                }
            } catch (e: Exception) {
                // Defensive catch for unexpected exceptions to prevent scheduler from crashing
                logger.error(e) { "Unexpected error during auto-flush" }
            }
        }
    }
}
