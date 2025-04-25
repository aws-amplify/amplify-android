package com.amplifyframework.aws.appsync.events

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import java.net.SocketTimeoutException
import junit.framework.TestCase.fail
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class EventsWebSocketProviderTest {
    private val provider = EventsWebSocketProvider(
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        null
    )

    @Before
    fun setup() {
        // Given
        mockkConstructor(EventsWebSocket::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Using runBlocking to test with IO launches to test introduced connect delay
     */
    @Test
    fun `multiple calls return same instance when not closed`() = runBlocking {
        every { anyConstructed<EventsWebSocket>().isClosed } returns false
        // introduce small random connect delay to mimic real scenario
        coEvery { anyConstructed<EventsWebSocket>().connect() } coAnswers {
            val randomDelay = Random.nextLong(0, 20)
            delay(randomDelay)
        }

        // launch multiple calls at once
        val calls = IntRange(1, 10).map {
            async(Dispatchers.IO) {
                provider.getConnectedWebSocket()
            }
        }

        // verify that all connections have the same instance and that we didn't end up with multiple websockets
        val results = calls.awaitAll()
        val firstConnection = results.first()
        results.forEach {
            firstConnection shouldBeSameInstanceAs it
        }
        coVerify(exactly = 1) {
            anyConstructed<EventsWebSocket>().connect()
        }
    }

    @Test
    fun `new connection created when previous connection is closed`() = runTest {
        // Given
        mockkConstructor(EventsWebSocket::class)
        coEvery { anyConstructed<EventsWebSocket>().connect() } answers {}

        // When
        // Create initial websocket connection
        every { anyConstructed<EventsWebSocket>().isClosed } answers { false }
        val firstConnection = provider.getConnectedWebSocket()

        // Before second attempt, set isClosed check to true so second attempt attempts another connection
        every { anyConstructed<EventsWebSocket>().isClosed } answers { true }
        val secondConnection = provider.getConnectedWebSocket()

        // Before third attempt, set isClosed check to false so third attempt reuses second connection
        every { anyConstructed<EventsWebSocket>().isClosed } answers { false }
        val thirdConnection = provider.getConnectedWebSocket()

        // Then
        coVerify(exactly = 2) {
            anyConstructed<EventsWebSocket>().connect()
        }
        firstConnection shouldNotBeSameInstanceAs secondConnection
        secondConnection shouldBeSameInstanceAs thirdConnection
    }

    @Test
    fun `getConnectedWebSocket failure will try new connection on second call`() = runTest {
        // Given
        mockkConstructor(EventsWebSocket::class)
        coEvery { anyConstructed<EventsWebSocket>().connect() } throws SocketTimeoutException()
        every { anyConstructed<EventsWebSocket>().isClosed } answers { false }

        // When
        // Force a connect exception, such as a network error
        val exception = try {
            provider.getConnectedWebSocket()
        } catch (e: Exception) {
            e
        }

        // Then
        // Ensure first attempt failed
        if (exception !is SocketTimeoutException) {
            fail("Expected exception calling getConnectedWebSocket")
        }

        // Given
        // Set up for second attempt to succeed
        coEvery { anyConstructed<EventsWebSocket>().connect() } answers {}

        // When
        // Attempt second connection
        val secondConnection = provider.getConnectedWebSocket()

        // Then
        // Verify second connection succeeded
        secondConnection.isClosed shouldBe false
        coVerify(exactly = 2) {
            anyConstructed<EventsWebSocket>().connect()
        }
    }
}
