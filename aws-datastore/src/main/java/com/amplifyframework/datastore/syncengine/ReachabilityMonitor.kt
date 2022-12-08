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
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.DataStoreChannelEventName
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.events.NetworkStatusEvent
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
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

    companion object {
        fun create() : ReachabilityMonitor {
            return ReachabilityMonitorImpl(ProdSchedulerProvider())
        }

        fun createForTesting(baseSchedulerProvider: SchedulerProvider): ReachabilityMonitor {
            return ReachabilityMonitorImpl(baseSchedulerProvider)
        }
    }
    fun getObservable(): Observable<Boolean>
    @VisibleForTesting
    fun getObservable(emitter: ObservableOnSubscribe<Boolean>): Observable<Boolean>
}

private class ReachabilityMonitorImpl constructor(val schedulerProvider: SchedulerProvider)
    : ReachabilityMonitor {

    var emitter: ObservableOnSubscribe<Boolean>? = null

    override fun configure(context: Context) {
        emitter = ObservableOnSubscribe { emitter ->
            val callback = getCallback(emitter)
            context.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(callback)
        }
    }

    override fun getObservable(): Observable<Boolean> {
        emitter?.let { emitter ->
            return getObservable(emitter)
        } ?: run { throw DataStoreException(
            "ReachabilityMonitor has not been configured.",
            "Call ReachabilityMonitor.configure() before calling ReachabilityMonitor.getObservable()") }
    }

    override fun getObservable(emitter: ObservableOnSubscribe<Boolean>): Observable<Boolean> {
        return Observable.create(emitter)
            .subscribeOn(schedulerProvider.computation())
            .debounce(250, TimeUnit.MILLISECONDS, schedulerProvider.computation())
            .doOnNext { println("value: $it") }
    }

    private fun getCallback(emitter: ObservableEmitter<Boolean>): NetworkCallback {
        return object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                emitter.onNext(true)
            }
            override fun onLost(network: Network) {
                emitter.onNext(false)
            }
        }
    }

//    private fun publishNetworkStatusEvent(active: Boolean) {
//        Amplify.Hub.publish(
//            HubChannel.DATASTORE,
//            HubEvent.create(DataStoreChannelEventName.NETWORK_STATUS, NetworkStatusEvent(active))
//        )
//    }
}
