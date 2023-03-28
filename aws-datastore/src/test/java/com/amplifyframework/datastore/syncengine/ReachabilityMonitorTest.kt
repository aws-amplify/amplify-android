package com.amplifyframework.datastore.syncengine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.amplifyframework.datastore.DataStoreException
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subscribers.TestSubscriber
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.mockito.Mockito.mock

class ReachabilityMonitorTest {

    // Test that calling getObservable() without calling configure() first throws a DataStoreException
    @Test(expected = DataStoreException::class)
    fun testReachabilityConfigThrowsException() {
        ReachabilityMonitor.create().getObservable()
    }

    // Test that the debounce and the event publishing in ReachabilityMonitor works as expected.
    // Events that occur within 250 ms of each other should be debounced so that only the last event
    // of the sequence is published.
    @Test
    fun testReachabilityDebounce() {
        var callback: ConnectivityManager.NetworkCallback? = null

        val connectivityProvider = object : ConnectivityProvider {
            override val hasActiveNetwork: Boolean
                get() = run {
                    return true
                }
            override fun registerDefaultNetworkCallback(
                context: Context,
                callback2: ConnectivityManager.NetworkCallback
            ) {
                callback = callback2
            }
        }

        val mockContext = mock(Context::class.java)
        // TestScheduler allows the virtual time to be advanced by exact amounts, to allow for repeatable tests
        val testScheduler = TestScheduler()
        val reachabilityMonitor = ReachabilityMonitor.createForTesting(TestSchedulerProvider(testScheduler))
        reachabilityMonitor.configure(mockContext, connectivityProvider)

        // TestSubscriber allows for assertions and awaits on the items it observes
        val testSubscriber = TestSubscriber<Boolean>()
        reachabilityMonitor.getObservable()
            // TestSubscriber requires a Flowable
            .toFlowable(BackpressureStrategy.BUFFER)
            .subscribe(testSubscriber)

        val network = mock(Network::class.java)
        // Should provide initial network state (true) upon subscription (after debounce)
        testScheduler.advanceTimeBy(251, TimeUnit.MILLISECONDS)
        callback!!.onAvailable(network)
        callback!!.onAvailable(network)
        callback!!.onLost(network)
        // Should provide false after debounce
        testScheduler.advanceTimeBy(251, TimeUnit.MILLISECONDS)
        callback!!.onAvailable(network)
        // Should provide true after debounce
        testScheduler.advanceTimeBy(251, TimeUnit.MILLISECONDS)
        callback!!.onAvailable(network)
        // Should provide true after debounce
        testScheduler.advanceTimeBy(251, TimeUnit.MILLISECONDS)

        testSubscriber.assertValues(false, true, true)
    }
}
