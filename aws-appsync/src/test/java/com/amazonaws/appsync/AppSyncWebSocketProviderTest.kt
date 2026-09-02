/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests [AppSyncWebSocketProvider] — the lazy shared connection.
 *
 * The connection count is what these assert. AppSync multiplexes every subscription over one socket,
 * so opening a second one is a real defect: it wastes a connection and reaches the API's connection
 * limit sooner. The concurrent case is the one worth having, because a naive
 * check-then-open has a window where two subscribers both see "no connection" and both open.
 */
class AppSyncWebSocketProviderTest {

    private val connectionsOpened = AtomicInteger(0)

    private fun liveSocket(): AppSyncWebSocket = mockk(relaxed = true) {
        every { isClosed } returns false
        coEvery { connect() } returns Unit
    }

    private fun provider(factory: () -> AppSyncWebSocket = { liveSocket() }) = AppSyncWebSocketProvider {
        connectionsOpened.incrementAndGet()
        factory()
    }

    // ── Laziness and sharing ────────────────────────────────────────────

    @Test
    fun `no connection is opened until one is asked for`() {
        provider()

        connectionsOpened.get() shouldBe 0
    }

    @Test
    fun `existing does not open a connection`() {
        provider().existing.shouldBeNull()

        connectionsOpened.get() shouldBe 0
    }

    @Test
    fun `the first request opens a connection`() = runTest {
        val provider = provider()

        provider.connection()

        connectionsOpened.get() shouldBe 1
    }

    @Test
    fun `a second request reuses the same connection`() = runTest {
        val provider = provider()

        val first = provider.connection()
        val second = provider.connection()

        first shouldBe second
        connectionsOpened.get() shouldBe 1
    }

    @Test
    fun `concurrent requests share one connection`() = runTest {
        // The case a naive check-then-open gets wrong: several subscribers arriving before the first
        // attempt completes must join it rather than each starting their own. The delay is inside
        // connect() so the attempt is genuinely in flight while the others arrive.
        val provider = provider {
            mockk(relaxed = true) {
                every { isClosed } returns false
                coEvery { connect() } coAnswers { delay(50) }
            }
        }

        val sockets = (1..8).map { async { provider.connection() } }.awaitAll()

        connectionsOpened.get() shouldBe 1
        sockets.distinct().size shouldBe 1
    }

    // ── Failure is not cached ───────────────────────────────────────────

    @Test
    fun `a failed attempt is not cached, so the next caller retries`() = runTest {
        var attempts = 0
        val provider = AppSyncWebSocketProvider {
            attempts++
            if (attempts == 1) throw AppSyncConnectionException("refused") else liveSocket()
        }

        shouldThrow<AppSyncConnectionException> { provider.connection() }
        provider.connection()

        attempts shouldBe 2
    }

    @Test
    fun `a connection failure surfaces as a typed AppSyncException`() = runTest {
        val provider = AppSyncWebSocketProvider { throw IllegalStateException("boom") }

        shouldThrow<AppSyncException> { provider.connection() }
            .shouldBeInstanceOf<AppSyncUnknownException>()
    }

    @Test
    fun `every caller joined to a failed attempt sees the failure`() = runTest {
        val provider = AppSyncWebSocketProvider {
            throw AppSyncConnectionException("refused")
        }

        val results = (1..4).map {
            async { runCatching { provider.connection() } }
        }.awaitAll()

        results.forEach { it.isFailure shouldBe true }
    }

    // ── Replacing a dead connection ─────────────────────────────────────

    @Test
    fun `a closed connection is replaced rather than handed out`() = runTest {
        val closed: AppSyncWebSocket = mockk(relaxed = true) { every { isClosed } returns true }
        var handedOut = 0
        val provider = AppSyncWebSocketProvider {
            handedOut++
            if (handedOut == 1) closed else liveSocket()
        }

        val first = provider.connection()
        val second = provider.connection()

        // This is what lets a client recover from an idle timeout without managing state itself.
        first shouldBe closed
        (second === closed) shouldBe false
        handedOut shouldBe 2
    }

    @Test
    fun `existing reports null once the connection has closed`() = runTest {
        val socket: AppSyncWebSocket = mockk(relaxed = true) { every { isClosed } returns true }
        val provider = AppSyncWebSocketProvider { socket }

        provider.connection()

        provider.existing.shouldBeNull()
    }

    // ── close() ─────────────────────────────────────────────────────────

    @Test
    fun `close disconnects the shared socket`() = runTest {
        val socket = liveSocket()
        val provider = AppSyncWebSocketProvider { socket }
        provider.connection()

        provider.close()

        coVerify(exactly = 1) { socket.disconnect(null) }
    }

    @Test
    fun `close passes the cause through`() = runTest {
        val socket = liveSocket()
        val cause = AppSyncTimeoutException("idle")
        val provider = AppSyncWebSocketProvider { socket }
        provider.connection()

        provider.close(cause)

        coVerify(exactly = 1) { socket.disconnect(cause) }
    }

    @Test
    fun `close on a provider that never connected does nothing`() = runTest {
        provider().close()

        connectionsOpened.get() shouldBe 0
    }

    @Test
    fun `a request after close opens a fresh connection`() = runTest {
        val provider = provider()
        provider.connection()

        provider.close()
        provider.connection()

        connectionsOpened.get() shouldBe 2
    }
}
