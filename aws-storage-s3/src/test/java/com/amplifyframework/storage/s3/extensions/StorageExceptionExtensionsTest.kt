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
package com.amplifyframework.storage.s3.extensions

import com.amplifyframework.storage.ProgressStallTimeoutException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

internal class StorageExceptionExtensionsTest {

    // / Wrapping a direct ProgressStallTimeoutException preserves the typed cause.
    // /
    // / - Given: A ProgressStallTimeoutException thrown from the worker layer.
    // / - When: toStorageUploadException is invoked with a fallback message.
    // / - Then: The returned StorageException's cause is the original typed exception, and the
    // /   stall-specific message is used instead of the fallback.
    @Test
    fun progressStallTimeoutExceptionPreservesTypedCause() {
        val fallbackMessage = "generic upload failure"
        val stall = ProgressStallTimeoutException("stall", "retry")

        val wrapped = stall.toStorageUploadException(fallbackMessage)

        wrapped.cause.shouldBeInstanceOf<ProgressStallTimeoutException>()
        wrapped.cause shouldBe stall
        wrapped.message shouldNotBe fallbackMessage
        wrapped.message!!.shouldContain("progress stall")
    }

    // / A ProgressStallTimeoutException nested inside another throwable is still surfaced.
    // /
    // / - Given: A generic Exception whose cause is a ProgressStallTimeoutException.
    // / - When: toStorageUploadException is invoked.
    // / - Then: The returned StorageException surfaces the typed cause, not the outer wrapper.
    @Test
    fun nestedProgressStallTimeoutExceptionIsDetected() {
        val stall = ProgressStallTimeoutException("stall", "retry")
        val outer = RuntimeException("wrapper", stall)

        val wrapped = outer.toStorageUploadException("generic upload failure")

        wrapped.cause shouldBe stall
    }

    // / Non-stall throwables are wrapped with the fallback message and preserve the original cause.
    // /
    // / - Given: A generic RuntimeException unrelated to stall detection.
    // / - When: toStorageUploadException is invoked.
    // / - Then: The returned StorageException uses the fallback message and keeps the original as cause.
    @Test
    fun genericThrowableUsesFallbackMessage() {
        val fallbackMessage = "something went wrong"
        val original = RuntimeException("boom")

        val wrapped = original.toStorageUploadException(fallbackMessage)

        wrapped.message shouldBe fallbackMessage
        wrapped.cause shouldBe original
    }
}
