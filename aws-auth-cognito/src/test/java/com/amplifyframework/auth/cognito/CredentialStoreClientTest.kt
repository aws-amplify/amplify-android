package com.amplifyframework.auth.cognito

import com.amplifyframework.statemachine.codegen.states.CredentialStoreState
import io.mockk.mockk
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class CredentialStoreClientTest {

    /**
     * This test has been verified to regularly fail if the OneShotCredentialStoreStateListener isActive
     * field is non-atomic.
     */
    @Test
    fun one_shot_listener_fires_once() {
        val attempts = 10_000
        val timesFired = AtomicInteger(0)
        var timesFailed = 0
        val listener = CredentialStoreClient.OneShotCredentialStoreStateListener(
            {
                if (timesFired.incrementAndGet() != 1) {
                    timesFailed += 1
                }
            }, {
            if (timesFired.incrementAndGet() != 1) {
                timesFailed += 1
            }
        },
            mockk(relaxed = true)
        )

        for (i in 0..attempts) {
            for (x in 0..5) {
                if (Random.nextBoolean()) {
                    listener.listen(CredentialStoreState.Success(mockk()))
                } else {
                    listener.listen(CredentialStoreState.Error(mockk()))
                }
                CoroutineScope(Dispatchers.IO).launch {
                    for (y in 0..5) {
                        listener.listen(CredentialStoreState.Idle())
                    }
                }
            }
        }

        assertEquals(0, timesFailed)
    }
}
