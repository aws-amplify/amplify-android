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

    //private var numberOfAttempts = 0


    fun <T> retry(single: Single<T>, skipExceptions: List<Class<out Throwable>?>): Single<T> {
        return Single.create { emitter -> call(single, emitter, 0, maxAttempts, skipExceptions) }
    }

    private fun <T> call(
        single: Single<T>,
        emitter: SingleEmitter<T>,
        delayInSeconds: Long,
        attemptsLeft: Int,
        skipExceptions: List<Class<out Throwable>?>
    ) {
        single.delaySubscription(delayInSeconds, TimeUnit.SECONDS)
            .subscribe({
                emitter.onSuccess(it)
            }) { error ->
                if (attemptsLeft == 0 || ErrorInspector.contains(error, skipExceptions)) {
                    emitter.onError(error)
                } else {
                    call(single, emitter, jitteredDelayMilli(attemptsLeft), attemptsLeft -1, skipExceptions)
                }
            }
    }


    fun jitteredDelayMilli(attemptsLeft: Int): Long {
        val waitTimeSeconds: Long =
            2.0.pow(((maxAttempts - attemptsLeft) % maxExponent)).toLong()
        +jitterFactor * Math.random()
        return TimeUnit.SECONDS.toMillis(waitTimeSeconds)
    }

}