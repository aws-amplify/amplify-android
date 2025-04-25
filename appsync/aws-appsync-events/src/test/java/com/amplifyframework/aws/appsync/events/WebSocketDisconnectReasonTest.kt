package com.amplifyframework.aws.appsync.events

import com.amplifyframework.aws.appsync.events.data.ConnectionClosedException
import com.amplifyframework.aws.appsync.events.data.EventsException
import com.amplifyframework.aws.appsync.events.data.UserClosedConnectionException
import io.kotest.matchers.shouldBe
import org.junit.Test

class WebSocketDisconnectReasonTest {

    @Test
    fun `test UserInitiated disconnect reason has null throwable`() {
        val reason = WebSocketDisconnectReason.UserInitiated

        reason.throwable shouldBe null
    }

    @Test
    fun `test Timeout disconnect reason has correct exception message`() {
        val reason = WebSocketDisconnectReason.Timeout
        reason.throwable shouldBe EventsException("Connection timed out.")
    }

    @Test
    fun `test Service disconnect reason with null throwable`() {
        val reason = WebSocketDisconnectReason.Service()

        reason.throwable shouldBe null
    }

    @Test
    fun `test Service disconnect reason with custom throwable`() {
        val customException = RuntimeException("Custom service error")
        val reason = WebSocketDisconnectReason.Service(customException)
        reason.throwable shouldBe customException
    }

    @Test
    fun `test toCloseException converts UserInitiated to UserClosedConnectionException`() {
        val reason = WebSocketDisconnectReason.UserInitiated
        val exception = reason.toCloseException()

        exception shouldBe UserClosedConnectionException()
    }

    @Test
    fun `test toCloseException converts Timeout to ConnectionClosedException`() {
        val reason = WebSocketDisconnectReason.Timeout
        val exception = reason.toCloseException()

        exception shouldBe ConnectionClosedException(EventsException("Connection timed out."))
    }

    @Test
    fun `test toCloseException converts Service to ConnectionClosedException`() {
        val customException = RuntimeException("Custom service error")
        val reason = WebSocketDisconnectReason.Service(customException)
        val exception = reason.toCloseException()

        exception shouldBe ConnectionClosedException(customException)
    }
}
