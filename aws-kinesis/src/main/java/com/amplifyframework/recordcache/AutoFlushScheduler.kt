package com.amplifyframework.recordcache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AutoFlushScheduler(
    val interval: FlushStrategy.Interval,
    val client: RecordClient<*>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
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
                client.flush()
            } catch (e: Exception) {
                // TODO: Log
            }
        }
    }
}
