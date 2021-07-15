package com.amplifyframework.datastore.syncengine

import com.amplifyframework.core.Amplify
import com.amplifyframework.core.model.Model
import java.util.*
import java.util.concurrent.TimeUnit

class RequestRetry {

    private var numberOfAttempts = 0
    private val maxExponent = 8
    private val jitterFactor = 100
    private val LOG = Amplify.Logging.forNamespace("amplify:aws-datastore")

    fun <T : Model> retry(retryCallback: RetryCallbackInterface<T>) {
        numberOfAttempts++
        val jitteredDelay = jitteredDelay()
        Timer().schedule(object : TimerTask() {
            override fun run() {
                LOG.verbose("Retrying attempt number:$numberOfAttempts jittered delay:$jitteredDelay")
                retryCallback.execute()
                cancel()
            }
        }, jitteredDelay)
    }


    fun jitteredDelay(): Long {
        val waitTimeSeconds: Long = java.lang.Double.valueOf(
            Math.pow(2.0, (numberOfAttempts % maxExponent).toDouble())
                    + jitterFactor * Math.random()
        ).toLong()
        return TimeUnit.MILLISECONDS.toMillis(waitTimeSeconds)
    }

}