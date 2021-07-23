package com.amplifyframework.datastore.syncengine

import com.amplifyframework.datastore.utils.ErrorInspector
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import java.util.concurrent.TimeUnit
import kotlin.math.pow


class RetryHandler(
    private val maxExponent: Int = 8,
    private val jitterFactor: Int = 100,
    private val maxAttempts: Int = 3
) {

    private var numberOfAttempts = 0


    fun <T> retry(single: Single<T>, skipExceptions: List<Class<out Throwable>?>): Single<T> {
        return Single.create { emitter -> call(single, emitter, 0, skipExceptions) }
    }

    private fun <T> call(
        single: Single<T>,
        emitter: SingleEmitter<T>,
        delayInSeconds: Long,
        skipExceptions: List<Class<out Throwable>?>
    ) {
        single.delaySubscription(delayInSeconds, TimeUnit.SECONDS)
            .subscribe({ t: T ->
                emitter.onSuccess(t)
            }) { error ->
                numberOfAttempts++
                if (numberOfAttempts > maxAttempts || ErrorInspector.contains(error, skipExceptions)) {
                    emitter.onError(error)
                } else {
                    call(single, emitter, jitteredDelay(), skipExceptions)
                }
            }
    }


    fun jitteredDelay(): Long {
        val waitTimeSeconds: Long =
            2.0.pow((numberOfAttempts % maxExponent)).toLong()
        +jitterFactor * Math.random()

        return TimeUnit.MILLISECONDS.toMillis(waitTimeSeconds)
    }

}