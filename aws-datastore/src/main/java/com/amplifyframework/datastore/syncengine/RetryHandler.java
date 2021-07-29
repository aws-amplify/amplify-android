package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.datastore.utils.ErrorInspector;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;

public class RetryHandler{
    private int maxExponent = 8;
    private int jitterFactor = 100;
    private int maxAttempts  = 3;
    public RetryHandler(  int maxExponent,
                          int jitterFactor,
                          int maxAttempts){

        this.maxExponent = maxExponent;
        this.jitterFactor = jitterFactor;
        this.maxAttempts = maxAttempts;
    }

    public RetryHandler(){
    }

    public <T> Single<T> retry(Single<T> single, List<Class<? extends Throwable>> skipExceptions ) {
            return Single.create (
                    emitter -> call(single, emitter, 0L, maxAttempts, skipExceptions));

    }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        private  <T> void  call(
                Single<T> single,
                SingleEmitter<T> emitter,
                Long delayInSeconds,
                int attemptsLeft,
                List<Class<? extends Throwable>> skipExceptions) {
            single.delaySubscription(delayInSeconds, TimeUnit.SECONDS)
                    .subscribe(emitter::onSuccess,
                            error ->{
                        if (attemptsLeft == 0 || ErrorInspector.contains(error, skipExceptions)) {
                            emitter.onError(error);
                        } else {
                            call(single, emitter, jitteredDelaySec(attemptsLeft),
                                    attemptsLeft -1, skipExceptions);
                        }
                    }

            );
        }


    long jitteredDelaySec(int attemptsLeft) {
            int numAttempt = maxAttempts - (maxAttempts - attemptsLeft);
            double waitTimeSeconds =
                     Math.pow(2,((numAttempt) % maxExponent))
                            +jitterFactor * Math.random();
            return (long) waitTimeSeconds ;
        }
}
