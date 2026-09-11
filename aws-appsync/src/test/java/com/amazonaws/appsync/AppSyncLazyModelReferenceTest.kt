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

import com.amplifyframework.AmplifyException
import com.amplifyframework.testmodels.lazy.Blog
import com.amplifyframework.testmodels.lazy.Post
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests a related model that is fetched on first access.
 */
@RunWith(RobolectricTestRunner::class)
class AppSyncLazyModelReferenceTest {

    private val keyMap = mapOf("id" to "b1")
    private val blog = Blog.builder().name("My Blog").build()

    @Test
    fun `the identifier is the key the parent's response carried`() {
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, RecordingModelLoader())

        reference.getIdentifier() shouldContainExactly keyMap
    }

    @Test
    fun `fetching builds a get query for the key`() = runTest {
        val loader = RecordingModelLoader { blog }
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, loader)

        reference.fetchModel() shouldBe blog

        val request = loader.requests.single()
        request.query shouldContain "getBlog"
        request.variables shouldContainExactly mapOf("id" to "b1")
    }

    @Test
    fun `an empty key map resolves to null without issuing a query`() = runTest {
        // An empty key is how the response says the parent has no related model. There is nothing to
        // identify, so a query could only fail.
        val loader = RecordingModelLoader { blog }
        val reference = AppSyncLazyModelReference(Blog::class.java, emptyMap(), loader)

        reference.fetchModel().shouldBeNull()

        loader.calls shouldBe 0
    }

    @Test
    fun `a loaded value is cached, so a second fetch does not query again`() = runTest {
        val loader = RecordingModelLoader { blog }
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, loader)

        reference.fetchModel() shouldBe blog
        reference.fetchModel() shouldBe blog

        loader.calls shouldBe 1
    }

    @Test
    fun `a loaded null is cached, so a second fetch does not query again`() = runTest {
        // The case a bare nullable cache cannot express: the model genuinely does not exist, which is a
        // loaded answer rather than an absent one.
        val loader = RecordingModelLoader { null }
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, loader)

        reference.fetchModel().shouldBeNull()
        reference.fetchModel().shouldBeNull()

        loader.calls shouldBe 1
    }

    @Test
    fun `concurrent fetches issue one query between them`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val loader = RecordingModelLoader {
            gate.await()
            blog
        }
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, loader)

        val first = async { reference.fetchModel() }
        val second = async { reference.fetchModel() }
        // Both are now in flight: one is inside the loader, the other is waiting on the lock.
        runCurrent()
        gate.complete(Unit)

        first.await() shouldBe blog
        second.await() shouldBe blog
        loader.calls shouldBe 1
    }

    @Test
    fun `a failure is reported as an AmplifyException carrying the client's own exception`() = runTest {
        // The lazy interfaces declare com.amplifyframework.AmplifyException, which the client's own
        // hierarchy does not extend, so the typed error has to travel as the cause.
        val cause = AppSyncNetworkException(message = "offline", recoverySuggestion = "reconnect")
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, failingLoader(cause))

        val error = shouldThrow<AmplifyException> { reference.fetchModel() }

        error.cause shouldBe cause
        error.recoverySuggestion shouldBe "reconnect"
        error.message shouldContain "Blog"
    }

    @Test
    fun `an untyped failure is still reported with a typed cause`() = runTest {
        val reference = AppSyncLazyModelReference(
            Blog::class.java,
            keyMap,
            failingLoader(IllegalStateException("something odd"))
        )

        val error = shouldThrow<AmplifyException> { reference.fetchModel() }

        error.cause.shouldBeInstanceOf<AppSyncUnknownException>()
    }

    @Test
    fun `a failure is not cached, so a later fetch tries again`() = runTest {
        var attempts = 0
        val loader = RecordingModelLoader {
            attempts++
            if (attempts == 1) throw AppSyncNetworkException("offline") else blog
        }
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, loader)

        shouldThrow<AmplifyException> { reference.fetchModel() }
        reference.fetchModel() shouldBe blog

        loader.calls shouldBe 2
    }

    // ── Callback overload ───────────────────────────────────────────────

    @Test
    fun `the callback overload delivers a cached value exactly once`() = runTest {
        val loader = RecordingModelLoader { blog }
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, loader, this)
        reference.fetchModel()

        var deliveries = 0
        reference.fetchModel({ deliveries++ }, { })
        // Anything the cache-hit path launched would deliver a second time here.
        advanceUntilIdle()

        deliveries shouldBe 1
        loader.calls shouldBe 1
    }

    @Test
    fun `the callback overload delivers a fetched value`() = runTest {
        val loader = RecordingModelLoader { blog }
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, loader, this)

        var delivered: Blog? = null
        reference.fetchModel({ delivered = it }, { })
        advanceUntilIdle()

        delivered shouldBe blog
    }

    @Test
    fun `the callback overload reports a failure on onError`() = runTest {
        val cause = AppSyncNetworkException(message = "offline", recoverySuggestion = "reconnect")
        val reference = AppSyncLazyModelReference(Blog::class.java, keyMap, failingLoader(cause), this)

        var reported: AmplifyException? = null
        var delivered = 0
        reference.fetchModel({ delivered++ }, { reported = it })
        advanceUntilIdle()

        delivered shouldBe 0
        reported?.cause shouldBe cause
    }

    @Test
    fun `the callback overload delivers null for an empty key map without querying`() = runTest {
        val loader = RecordingModelLoader { blog }
        val reference = AppSyncLazyModelReference(Post::class.java, emptyMap(), loader, this)

        var deliveries = 0
        var delivered: Post? = null
        reference.fetchModel(
            {
                delivered = it
                deliveries++
            },
            { }
        )
        advanceUntilIdle()

        deliveries shouldBe 1
        delivered.shouldBeNull()
        loader.calls shouldBe 0
    }
}
