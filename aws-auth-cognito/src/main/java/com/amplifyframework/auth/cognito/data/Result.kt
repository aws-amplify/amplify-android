package com.amplifyframework.auth.cognito.data

import java.lang.Exception

/**
 * Similar to {@see kotlin.Result} with Failure accepting message only.
 */
sealed class Result<out T > {
    data class Success<out T>(val value: T) : Result<T>()
    data class Failure(val message: String, val exception: Exception? = null) : Result<Nothing>()
}