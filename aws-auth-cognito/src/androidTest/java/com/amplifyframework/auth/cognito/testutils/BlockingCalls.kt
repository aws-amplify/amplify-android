/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.auth.cognito.testutils

import com.amplifyframework.auth.AuthException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

typealias ResultFunction<T> = (onSuccess: (T) -> Unit, onError: (AuthException) -> Unit) -> Unit
typealias ResultFunctionWithArg<T, R> = (arg: T, onSuccess: (R) -> Unit, onError: (AuthException) -> Unit) -> Unit

// Run a function that invokes a callback-based API and returns the result or throws an exception on error.
// The exception is thrown on the calling thread, allowing it to be caught by the JUnit runner
fun <T> blockForResult(timeout: Duration = 10.seconds, func: ResultFunction<T>): T = runBlocking {
    withTimeout(timeout) {
        suspendCoroutine { continuation ->
            func(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }
}

// Run a function that invokes a callback-based API and blocks until it completes, throwing an exception on error.
// The exception is thrown on the calling thread, allowing it to be caught by the JUnit runner
fun blockForCompletion(
    timeout: Duration = 10.seconds,
    func: (onSuccess: () -> Unit, onError: (AuthException) -> Unit) -> Unit
): Unit = blockForResult(timeout) { onSuccess, onError ->
    func({ onSuccess(Unit) }, onError)
}

// Run a function that invokes a callback-based API and get the result as a Future
// This allows invoking the API many times concurrently for stress-testing purposes
fun <T> deferredResult(func: ResultFunction<T>): Deferred<T> {
    val deferred = CompletableDeferred<T>()
    func(
        { deferred.complete(it) },
        { deferred.completeExceptionally(it) }
    )
    return deferred
}

fun <T, R> deferredResult(arg: T, func: ResultFunctionWithArg<T, R>): Deferred<R> {
    val deferred = CompletableDeferred<R>()
    func(
        arg,
        { deferred.complete(it) },
        { deferred.completeExceptionally(it) }
    )
    return deferred
}
