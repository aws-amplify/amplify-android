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

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import androidx.annotation.VisibleForTesting
import com.amplifyframework.datastore.DataStoreException
import io.reactivex.rxjava3.core.Observable
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
public interface ReachabilityMonitor {
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
                object : NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        emitter.onNext(true)
                    }

                    override fun onLost(network: Network) {
                        emitter.onNext(false)
                    }
                }
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
}

/**
 * This interface puts an abstraction layer over ConnectivityManager. Since ConnectivityManager
 * is a concrete class created within context.getSystemService() it can't be overridden with a test
 * implementation, so this interface works around that issue.
 */
public interface ConnectivityProvider {
    val hasActiveNetwork: Boolean
    fun registerDefaultNetworkCallback(context: Context, callback: NetworkCallback)
}

private class DefaultConnectivityProvider : ConnectivityProvider {

    private var connectivityManager: ConnectivityManager? = null

    override val hasActiveNetwork: Boolean
        get() = connectivityManager?.let { it.activeNetwork != null }
            ?: run {
                throw DataStoreException(
                    "ReachabilityMonitor has not been configured.",
                    "Call ReachabilityMonitor.configure() before calling ReachabilityMonitor.getObservable()"
                )
            }

    override fun registerDefaultNetworkCallback(context: Context, callback: NetworkCallback) {
        connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        connectivityManager?.let { it.registerDefaultNetworkCallback(callback) }
            ?: run {
                throw DataStoreException(
                    "ConnectivityManager not available",
                    "No recovery suggestion is available"
                )
            }
    }
}
