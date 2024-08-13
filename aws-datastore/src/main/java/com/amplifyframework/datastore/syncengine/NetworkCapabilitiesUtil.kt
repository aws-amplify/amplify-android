package com.amplifyframework.datastore.syncengine

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * The NetworkCapabilitiesUtil provides convenient methods to check network capabilities and connection status
 */
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

fun NetworkCapabilities?.isInternetReachable() = when {
    this == null -> false
    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
    hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> linkDownstreamBandwidthKbps != 0
    else -> false
}