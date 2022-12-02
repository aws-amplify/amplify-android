package com.amplifyframework.datastore.syncengine

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.DataStoreChannelEventName
import com.amplifyframework.datastore.events.NetworkStatusEvent
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import java.util.concurrent.TimeUnit


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

/**
 * The ReachabilityMonitor is responsible for watching the network status as provided by the OS,
 * and publishing the {@link DataStoreChannelEventName.NETWORK_STATUS} event on the {@link Hub}. NETWORK_STATUS=true
 * indicates the network has come online, and NETWORK_STATUS=false indicates the network has gone offline. The
 * ReachabilityMonitor does not try to monitor the DataStore websockets or the status of the AppSync service.
 *
 * The network changes are debounced with a 250 ms delay to allow some time for one network to connect after another
 * network has disconnected.
 */
class ReachabilityMonitor {
    private val LOG = Amplify.Logging.forNamespace("amplify:datastore")

    fun configure(context: Context) {
        val emitter = ObservableOnSubscribe { emitter ->
            val callback = getCallback(emitter)
            context.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(callback)
        }
        getObservable(emitter)
            .subscribe()
    }

    internal fun getObservable(observable: ObservableOnSubscribe<Boolean>): Observable<Boolean> {
        return Observable.create (observable)
            .debounce(250, TimeUnit.MILLISECONDS)
            .doOnEach {
                publishNetworkStatusEvent(it.value!!)
            }
    }

    internal fun getCallback(emitter: ObservableEmitter<Boolean>): NetworkCallback {
        return object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                LOG.info("Network available: $network")
                emitter.onNext(true)
            }
            override fun onLost(network: Network) {
                LOG.info("Network lost: $network")
                emitter.onNext(false)
            }
        }
    }

    private fun publishNetworkStatusEvent(active: Boolean) {
        Amplify.Hub.publish(
            HubChannel.DATASTORE,
            HubEvent.create(DataStoreChannelEventName.NETWORK_STATUS, NetworkStatusEvent(active))
        )
    }
}