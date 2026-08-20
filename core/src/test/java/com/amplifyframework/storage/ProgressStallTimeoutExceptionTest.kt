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

package com.amplifyframework.storage

import com.amplifyframework.AmplifyException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class ProgressStallTimeoutExceptionTest {

    /**
     * The exception must surface its message and recovery suggestion verbatim so callers using
     * `storageException.cause as ProgressStallTimeoutException` can render both fields without
     * any unwrapping.
     *
     * - Given: a [ProgressStallTimeoutException] constructed with explicit text
     * - When: `message` and `recoverySuggestion` are read
     * - Then: both fields equal the constructor inputs
     */
    @Test
    fun `message and recovery suggestion round trip through constructor`() {
        val exception = ProgressStallTimeoutException(
            "Upload cancelled due to progress stall timeout.",
            "Increase the configured progress stall timeout or verify the network conditions, then retry the upload."
        )

        exception.message shouldBe "Upload cancelled due to progress stall timeout."
        exception.recoverySuggestion shouldBe
            "Increase the configured progress stall timeout or verify the network conditions, then retry the upload."
    }

    /**
     * The exception must inherit from [AmplifyException] so that callers handling generic Amplify
     * failures will also catch a stall failure when they walk the cause chain.
     *
     * - Given: a [ProgressStallTimeoutException]
     * - When: it is checked against [AmplifyException]
     * - Then: it is a subtype of [AmplifyException]
     */
    @Test
    fun `extends AmplifyException so generic catches still match`() {
        val exception = ProgressStallTimeoutException("stall", "retry")

        exception.shouldBeInstanceOf<AmplifyException>()
    }

    /**
     * The companion object is annotated `@InternalAmplifyApi` and is the anchor for plugin-side
     * factory extensions defined in `aws-storage-s3`. Confirming that it is accessible (and not
     * private) keeps the contract for those extension declarations.
     *
     * - Given: the [ProgressStallTimeoutException.Companion]
     * - When: it is referenced
     * - Then: the reference is non-null
     */
    @Test
    fun `companion object is exposed for plugin extensions`() {
        val companion = ProgressStallTimeoutException.Companion

        @Suppress("USELESS_IS_CHECK") // safety net if the companion type ever changes
        (companion is ProgressStallTimeoutException.Companion) shouldBe true
    }
}
