/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.datastore.extensions

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.VisibleForTesting

// Return network capabilities based on Connectivity Manager
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun ConnectivityManager.networkCapabilitiesOrNull() = try {
    getNetworkCapabilities(activeNetwork)
} catch (ignored: SecurityException) {
    // Android 11 may throw a 'package does not belong' security exception here.
    // Google fixed Android 14, 13 and 12 with the issue where Chaland Jean patched those versions.
    // Android 11 is too old, so that's why we have to catch this exception here to be safe.
    // https://android.googlesource.com/platform/frameworks/base/+/249be21013e389837f5b2beb7d36890b25ecfaaf%5E%21/
    // We need to catch this to prevent app crash.
    null
}

// Return whether internet is reachable based on network capabilities.
fun NetworkCapabilities?.isInternetReachable() = when {
    this == null -> false
    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
    else -> false
}

// Check whether network is available based on connectivity manager
// the same logic as https://github.com/aws-amplify/amplify-android/blob/main/aws-storage-s3/src/main/java/com/amplifyframework/storage/s3/transfer/worker/BaseTransferWorker.kt#L176
fun ConnectivityManager.isNetworkAvailable(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        networkCapabilitiesOrNull()?.isInternetReachable() ?: false
    } else {
        activeNetworkInfo?.isConnected ?: false
    }
