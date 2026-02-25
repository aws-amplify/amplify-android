package com.amplifyframework.recordcache

import com.amplifyframework.foundation.result.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoFlushSchedulerTest {

    @Test
    fun `start should begin periodic flushing`() = runTest {
        // Given
        val mockClient = mockk<RecordClient>()
        coEvery { mockClient.flush() } returns Result.Success(FlushData(0))

        val interval = FlushStrategy.Interval(1.seconds)
        val scheduler = AutoFlushScheduler(
            interval,
            mockClient,
            StandardTestDispatcher(testScheduler)
        )

        // When
        scheduler.start()
        advanceTimeBy(2.5.seconds) // Advance 2.5 seconds
        scheduler.disable()

        // Then
        coVerify(exactly = 2) { mockClient.flush() }
    }

    @Test
    fun `disable should stop periodic flushing`() = runTest {
        // Given
        val mockClient = mockk<RecordClient>()
        coEvery { mockClient.flush() } returns Result.Success(FlushData(0))

        val interval = FlushStrategy.Interval(1.seconds)
        val scheduler = AutoFlushScheduler(
            interval,
            mockClient,
            StandardTestDispatcher(testScheduler)
        )

        // When
        scheduler.start()
        advanceTimeBy(1500L) // 1.5 seconds - should trigger 1 flush
        scheduler.disable()
        advanceTimeBy(2000L) // Advance more time after disable

        // Then
        coVerify(exactly = 1) { mockClient.flush() }
    }

    @Test
    fun `start should cancel previous job and restart`() = runTest {
        // Given
        val mockClient = mockk<RecordClient>()
        coEvery { mockClient.flush() } returns Result.Success(FlushData(0))

        val interval = FlushStrategy.Interval(1.seconds)
        val scheduler = AutoFlushScheduler(
            interval,
            mockClient,
            StandardTestDispatcher(testScheduler)
        )

        // When
        scheduler.start()
        advanceTimeBy(500L) // Wait 0.5 seconds
        scheduler.start() // Restart - should cancel previous job
        advanceTimeBy(1500L) // Wait 1.5 more seconds
        scheduler.disable()

        // Then
        coVerify(exactly = 1) { mockClient.flush() }
    }
}
