package com.amplifyframework.core.async;

import android.util.Log;

import java.util.concurrent.CountDownLatch;

/**
 * Internal use. Refrain from using. Subject to breaking API changes at any time.
 *
 * Acts as a utility to de-reference library user's callback once it has been notified.
 * This class currently allows one callback before de-referencing.
 *
 * @param <R> result type of {@link InternalCallback#onResult(Object)}
 */
public class InternalCallback<R> implements Callback<R> {
    private static final String TAG = InternalCallback.class.getSimpleName();
    private enum Mode {
        Callback,
        Async,
        Sync,
        Done
    }

    private Callback<R> userCallback;
    private Mode mode;
    private CountDownLatch lock;
    private Runnable runnable;
    private R result;
    private Exception e;

    public InternalCallback() {
        this(null);
    }

    public InternalCallback(final Callback<R> userCallback) {
        this.userCallback = userCallback;
        mode = Mode.Callback;
        lock = new CountDownLatch(1);
    }

    @Override
    public void onResult(R result) {
        call(result, null);
    }

    @Override
    public void onError(Exception e) {
        call(null, e);
    }

    private void call(R result, Exception e) {
        switch (mode) {
            case Callback:
            case Async:
                if (result != null)
                    userCallback.onResult(result);
                else
                    userCallback.onError(e);
            case Sync:
                this.result = result;
                this.e = e;
                lock.countDown();
            case Done:
                Log.w(TAG, "Library attempted to call user callback twice, expected only once");
        }
        mode = Mode.Done;
        userCallback = null;
    }

    public void async(final Runnable runnable) {
        if (mode == Mode.Done) {
            Log.e(TAG, "Duplicate call to execute code.", new RuntimeException("Internal error, duplicate call"));
        }
        mode = Mode.Async;
        lock = null;
        new Thread(runnable).start();
    }

    public R await(final Runnable runnable) throws Exception {
        if (mode == Mode.Done) {
            Log.e(TAG, "Duplicate call to execute code.", new RuntimeException("Internal error, duplicate call"));
        }
        mode = Mode.Sync;
        try {
            runnable.run();
            lock.await();
        } catch (Exception e) {
            this.e = e;
        }

        final Exception localE = this.e;
        final R localResult = this.result;
        this.e = null;
        this.result = null;

        if (localE != null) {
            throw localE;
        }
        return localResult;
    }
}
