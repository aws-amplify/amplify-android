package com.amplifyframework.recordcache

import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.exceptionOrNull
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.foundation.result.isSuccess
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
                when (val result = client.flush()) {
                    is Result.Success -> logger.debug {
                        "Auto-flush completed: ${result.data.recordsFlushed} records flushed"
                    }
                    is Result.Failure -> logger.warn(result.error) { "Auto-flush failed" }
                }
            } catch (e: Exception) {
                // Defensive catch for unexpected exceptions to prevent scheduler from crashing
                logger.error(e) { "Unexpected error during auto-flush" }
            }
        }
    }
}
