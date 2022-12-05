package com.amplifyframework.datastore.syncengine

import com.amplifyframework.datastore.events.NetworkStatusEvent
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testutils.HubAccumulator
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import org.junit.Assert
import org.junit.Test

class ReachabilityMonitorTest {

    // Test that the debounce and the event publishing in ReachabilityMonitor works as expected.
    // Events that occur within 250 ms of each other should be debounced so that only the last event
    // of the sequence is published.
    @Test
    fun testReachabilityDebounce() {
        val accumulator = HubAccumulator.create(HubChannel.DATASTORE, 3)
        accumulator.start()

        val reachabilityMonitor = ReachabilityMonitor()

        val emitter = ObservableOnSubscribe { emitter ->
            emitter.onNext(true)
            emitter.onNext(false)
            Thread.sleep(500)
            emitter.onNext(true)
            Thread.sleep(500)
            emitter.onNext(false)
            emitter.onNext(true)
        }

        val debounced = reachabilityMonitor.getObservable(emitter)
        debounced.subscribe()

        val events = accumulator.await()

        Assert.assertEquals(
            events.map { (it.data as NetworkStatusEvent).active },
            listOf<Boolean>(false, true, true)
        )
    }
}
