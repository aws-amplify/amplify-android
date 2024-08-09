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

fun ConnectivityManager.isNetworkAvailable() =
        networkCapabilitiesOrNull()?.isInternetReachable()
            ?: activeNetworkInfo?.isConnected
            ?: false

fun NetworkCapabilities.isInternetReachable() = when {
    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
    else -> false
}

private fun ConnectivityManager.networkCapabilitiesOrNull(): NetworkCapabilities? = try {
    getNetworkCapabilities(activeNetwork)
} catch (ignored: SecurityException) {
    // Android 11 may throw a 'package does not belong' security exception here.
    // Google fixed Android 14, 13 and 12 with the issue where Chaland Jean patched those versions.
    // Android 11 is too old, so that's why we have to catch this exception here to be safe.
    // https://android.googlesource.com/platform/frameworks/base/+/249be21013e389837f5b2beb7d36890b25ecfaaf%5E%21/
    // We need to catch this to prevent app crash.
    null
}