package com.amplifyframework.datastore.syncengine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.amplifyframework.datastore.DataStoreException
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subscribers.TestSubscriber
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        testSubscriber.assertValues(true, false, true)
    }

    /**
     * Test that ensures the reachabilityMonitor observer does not block while waiting on the debouncer,
     * but provides the last stable value (value that lasted > 250ms).
     */
    @Test
    fun testReachabilityDebounceCache() {
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

        // Assert that the first value is returned
        callback!!.onAvailable(network)
        testScheduler.advanceTimeBy(251, TimeUnit.MILLISECONDS)
        var result1: Boolean? = null
        val disposable1 = reachabilityMonitor.getObservable().subscribeOn(testScheduler).subscribe { result1 = it }
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertTrue(result1 == true)

        // Assert that the cached value is still returned if a status has changed but debouncer hasn't completed
        callback!!.onLost(network)
        var result2: Boolean? = null
        val disposable2 = reachabilityMonitor.getObservable().subscribeOn(testScheduler).subscribe { result2 = it }
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertTrue(result2 == true)

        // Assert that once the debouncer has completed, the returned value is changed
        var result3: Boolean? = null
        val disposable3 = reachabilityMonitor.getObservable().subscribeOn(testScheduler).subscribe { result3 = it }
        testScheduler.advanceTimeBy(251, TimeUnit.MILLISECONDS)
        assertTrue(result3 == false)

        // Assert that if debouncer keeps getting restarted, value doesn't change
        callback!!.onAvailable(network)
        testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        callback!!.onLost(network)
        testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        callback!!.onAvailable(network)
        testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        var result4: Boolean? = null
        val disposable4 = reachabilityMonitor.getObservable().subscribeOn(testScheduler).subscribe { result4 = it }
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertTrue(result4 == false)

        // Assert that once the debouncer has completed, the returned value is changed
        testScheduler.advanceTimeBy(151, TimeUnit.MILLISECONDS)
        var result5: Boolean? = null
        val disposable5 = reachabilityMonitor.getObservable().subscribeOn(testScheduler).subscribe { result5 = it }
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        assertTrue(result5 == true)

        disposable1.dispose()
        disposable2.dispose()
        disposable3.dispose()
        disposable4.dispose()
        disposable5.dispose()
    }

    /**
     * Test that calling getObservable() multiple times only results in the network
     * callback being registered once.
     */
    @Test
    fun testNetworkCallbackRegisteredOnce() {
        var networkCallback: ConnectivityManager.NetworkCallback? = null
        var numCallbacksRegistered = 0

        val connectivityProvider = object : ConnectivityProvider {
            override val hasActiveNetwork: Boolean
                get() = run {
                    return true
                }
            override fun registerDefaultNetworkCallback(
                context: Context,
                callback: ConnectivityManager.NetworkCallback
            ) {
                networkCallback = callback
                numCallbacksRegistered += 1
            }
        }

        // TestScheduler allows the virtual time to be advanced by exact amounts, to allow for repeatable tests
        val testScheduler = TestScheduler()
        val reachabilityMonitor = ReachabilityMonitor.createForTesting(TestSchedulerProvider(testScheduler))
        val mockContext = mock(Context::class.java)
        reachabilityMonitor.configure(mockContext, connectivityProvider)

        reachabilityMonitor.getObservable().subscribe()
        val network = mock(Network::class.java)
        // Should provide initial network state (true) upon subscription (after debounce)
        testScheduler.advanceTimeBy(251, TimeUnit.MILLISECONDS)
        networkCallback!!.onAvailable(network)

        reachabilityMonitor.getObservable().subscribe()
        testScheduler.advanceTimeBy(251, TimeUnit.MILLISECONDS)
        networkCallback!!.onAvailable(network)

        // Only 1 network callback should be registered
        assertEquals(1, numCallbacksRegistered)
    }
}
