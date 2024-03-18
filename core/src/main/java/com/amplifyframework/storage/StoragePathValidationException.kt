package com.amplifyframework.storage

import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.InternalAmplifyApi

class StoragePathValidationException @InternalAmplifyApi constructor(
    message: String,
    recoverySuggestion: String
) : AmplifyException(message, recoverySuggestion) {
    @InternalAmplifyApi companion object
}
