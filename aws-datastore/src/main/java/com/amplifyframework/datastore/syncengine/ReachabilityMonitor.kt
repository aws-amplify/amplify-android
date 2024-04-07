/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.syncengine

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.amplifyframework.datastore.DataStoreException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit


/**
 * The ReachabilityMonitor is responsible for watching the network status as provided by the OS.
 * It returns an observable that publishes "true" when the network becomes available and "false" when
 * the network is lost.  It publishes the current status on subscription.
 *
 * ReachabilityMonitor does not try to monitor the DataStore websockets or the status of the AppSync service.
 *
 * The network changes are debounced with a 250 ms delay to allow some time for one network to connect after another
 * network has disconnected (for example, wifi is lost, then cellular connects) to reduce thrashing.
 */
interface ReachabilityMonitor {
    fun configure(context: Context)
    @VisibleForTesting
    fun configure(context: Context, connectivityProvider: ConnectivityProvider)

    companion object {
        fun create(): ReachabilityMonitor {
            return ReachabilityMonitorImpl(ProdSchedulerProvider())
        }

        fun createForTesting(baseSchedulerProvider: SchedulerProvider): ReachabilityMonitor {
            return ReachabilityMonitorImpl(baseSchedulerProvider)
        }
    }
    fun getObservable(): Observable<Boolean>
}

private class ReachabilityMonitorImpl constructor(val schedulerProvider: SchedulerProvider) : ReachabilityMonitor {
    private val subject = BehaviorSubject.create<Boolean>()
    private var connectivityProvider: ConnectivityProvider? = null

    override fun configure(context: Context) {
        return configure(context, DefaultConnectivityProvider())
    }

    override fun configure(context: Context, connectivityProvider: ConnectivityProvider) {
        this.connectivityProvider = connectivityProvider
        val observable = Observable.create { emitter ->
            connectivityProvider.registerDefaultNetworkCallback(
                context,
                ConnectivityNetworkCallback(emitter)
            )
            emitter.onNext(connectivityProvider.hasActiveNetwork)
        }
        observable.debounce(250, TimeUnit.MILLISECONDS, schedulerProvider.computation())
            .distinctUntilChanged()
            .subscribe(subject)
    }

    override fun getObservable(): Observable<Boolean> {
        if (connectivityProvider == null) {
            throw DataStoreException(
                "ReachabilityMonitor has not been configured.",
                "Call ReachabilityMonitor.configure() before calling ReachabilityMonitor.getObservable()"
            )
        }
        return subject.subscribeOn(schedulerProvider.io())
    }

    private inner class ConnectivityNetworkCallback(private val emitter: ObservableEmitter<Boolean>) : NetworkCallback() {
        private var currentNetwork: Network? = null
        private var currentCapabilities: NetworkCapabilities? = null
        private val DELAY_MS: Long = 250

        override fun onAvailable(network: Network) {
            currentNetwork = network
            asyncUpdateAndSend()
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            currentNetwork = network
            updateAndSend()
        }

        override fun onLost(network: Network) {
            currentNetwork = null
            currentCapabilities = null
            updateAndSend()
        }

        override fun onUnavailable() {
            currentNetwork = null
            currentCapabilities = null
            updateAndSend()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            currentNetwork = network
            currentCapabilities = networkCapabilities
            updateAndSend()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            if (currentNetwork != null) {
                currentNetwork = network
            }
            asyncUpdateAndSend()
        }

        private fun updateAndSend() {
            emitter.onNext(DefaultConnectivityProvider.isInternetReachable(currentCapabilities))
        }

        @SuppressLint("CheckResult")
        private fun asyncUpdateAndSend() {
            Completable.timer(DELAY_MS, TimeUnit.MILLISECONDS, schedulerProvider.computation()).subscribe {
                currentCapabilities = DefaultConnectivityProvider.getNetworkCapabilities(connectivityProvider?.connectivityNetworkManager)
                if (currentCapabilities != null) {
                    updateAndSend()
                }
            }
        }
    }
}

/**
 * This interface puts an abstraction layer over ConnectivityManager. Since ConnectivityManager
 * is a concrete class created within context.getSystemService() it can't be overridden with a test
 * implementation, so this interface works around that issue.
 */
interface ConnectivityProvider {
    val hasActiveNetwork: Boolean
    val connectivityNetworkManager: ConnectivityManager?
    fun registerDefaultNetworkCallback(context: Context, callback: NetworkCallback)
}

private class DefaultConnectivityProvider : ConnectivityProvider {
    private var connectivityManager: ConnectivityManager? = null

    override val connectivityNetworkManager: ConnectivityManager?
        get() = connectivityManager

    override val hasActiveNetwork: Boolean
        get() = connectivityManager?.let {
            val networkCapabilities: NetworkCapabilities? = getNetworkCapabilities(it)
            isInternetReachable(networkCapabilities)
        } ?: run {
            throw DataStoreException(
                "ReachabilityMonitor has not been configured.",
                "Call ReachabilityMonitor.configure() before calling ReachabilityMonitor.getObservable()"
            )
        }

    override fun registerDefaultNetworkCallback(context: Context, callback: NetworkCallback) {
        connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        connectivityManager?.registerDefaultNetworkCallback(callback)
            ?: run {
                throw DataStoreException(
                    "ConnectivityManager not available",
                    "No recovery suggestion is available"
                )
            }
    }

    companion object {
        fun getNetworkCapabilities(connectivityManager: ConnectivityManager?): NetworkCapabilities? {
            try {
                return connectivityManager?.let {
                    it.getNetworkCapabilities(it.activeNetwork)
                }
            } catch (ignored: SecurityException) {
                // Android 11 may throw a 'package does not belong' security exception here.
                // Google fixed Android 14, 13 and 12 with the issue where Chaland Jean patched those versions.
                // Android 11 is too old, so that's why we have to catch this exception here to be safe.
                //  https://android.googlesource.com/platform/frameworks/base/+/249be21013e389837f5b2beb7d36890b25ecfaaf%5E%21/
                // We need to catch this to prevent app crash.
            }
            return null
        }

        fun isInternetReachable(capabilities: NetworkCapabilities?): Boolean {
            var isInternetReachable = false
            if (capabilities != null) {
                // Check to see if the network is temporarily unavailable or if airplane mode is toggled on
                val isInternetSuspended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                } else {
                    // TODO: for SDK < 28, add extra check to evaluate airplane mode
                    false
                }
                isInternetReachable = (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    && !isInternetSuspended)
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    isInternetReachable = isInternetReachable && capabilities.linkDownstreamBandwidthKbps != 0
                }
            }
            return isInternetReachable
        }
    }
}
