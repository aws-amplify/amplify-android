package com.amplifyframework.datastore.syncengine

import com.amplifyframework.datastore.events.NetworkStatusEvent
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.testutils.HubAccumulator
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subscribers.TestSubscriber
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit

class ReachabilityMonitorTest {

    // Test that the debounce and the event publishing in ReachabilityMonitor works as expected.
    // Events that occur within 250 ms of each other should be debounced so that only the last event
    // of the sequence is published.
    @Test
    fun testReachabilityDebounce() {
//        val accumulator = HubAccumulator.create(HubChannel.DATASTORE, 3)
//        accumulator.start()

        val testScheduler = TestScheduler()

        val reachabilityMonitor = ReachabilityMonitor.createForTesting(TestSchedulerProvider(testScheduler))

        val emitter = ObservableOnSubscribe { emitter ->
            println("HELLO")
            emitter.onNext(true)
            println("HELLO")
            emitter.onNext(false)
            println("HELLO")
            testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS)
            println("HELLO")
            emitter.onNext(true)
            println("HELLO")
            testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS)
            println("HELLO")
            emitter.onNext(false)
            println("HELLO")
            emitter.onNext(true)
            println("HELLO")
            testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS)
        }

        val testSubscriber = TestSubscriber<Boolean>()

        reachabilityMonitor.getObservable(emitter)
            .toFlowable(BackpressureStrategy.BUFFER)
            .subscribe(testSubscriber)

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        testSubscriber.request(3)
        testSubscriber.awaitCount(3)
        testSubscriber.assertValues(false, true, true)
    }
}
