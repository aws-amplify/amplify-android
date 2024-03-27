package com.amplifyframework.storage

import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.InternalAmplifyApi

/**
 * Exception thrown when the StoragePath is not valid.
 */
class StoragePathValidationException @InternalAmplifyApi constructor(
    message: String,
    recoverySuggestion: String
) : AmplifyException(message, recoverySuggestion) {
    @InternalAmplifyApi companion object
}
