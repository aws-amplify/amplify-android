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
package com.amplifyframework.storage.s3.options

import com.amplifyframework.storage.ProgressStallTimeout
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class AWSS3StorageUploadFileOptionsTest {

    /**
     * The default instance must leave `progressStallTimeout` as `null` so that the plugin-level
     * default from `AWSS3StoragePluginConfiguration` is used unless a caller explicitly overrides it.
     *
     * - Given: the default instance of [AWSS3StorageUploadFileOptions]
     * - When: `progressStallTimeout` is read
     * - Then: the value is `null`, meaning "defer to plugin default"
     */
    @Test
    fun `default progressStallTimeout is null so plugin default applies`() {
        val options = AWSS3StorageUploadFileOptions.defaultInstance()
        options.progressStallTimeout shouldBe null
    }

    /**
     * Supplying a per-upload [ProgressStallTimeout] via the builder must make that value
     * reachable from the built options object so that downstream code can prefer the override
     * over the plugin default.
     *
     * - Given: a builder with a non-null [ProgressStallTimeout.Interval]
     * - When: the options are built
     * - Then: `progressStallTimeout` equals the configured override
     */
    @Test
    fun `builder propagates per-upload progressStallTimeout override`() {
        val override = ProgressStallTimeout.Interval(seconds = 20L)
        val options = AWSS3StorageUploadFileOptions.builder()
            .progressStallTimeout(override)
            .build()
        options.progressStallTimeout shouldBe override
    }

    /**
     * [ProgressStallTimeout.Disabled] is a legitimate per-upload override that opts the upload
     * out of stall detection even if the plugin default enables it. It must round-trip through
     * the builder without being treated as "defer to plugin default".
     *
     * - Given: a builder with `progressStallTimeout = ProgressStallTimeout.Disabled`
     * - When: the options are built
     * - Then: `progressStallTimeout` is `Disabled` (not `null`)
     */
    @Test
    fun `builder preserves Disabled as explicit per-upload opt out`() {
        val options = AWSS3StorageUploadFileOptions.builder()
            .progressStallTimeout(ProgressStallTimeout.Disabled)
            .build()
        options.progressStallTimeout shouldBe ProgressStallTimeout.Disabled
        options.progressStallTimeout shouldNotBe null
    }

    /**
     * `from(options)` is used to clone or customize an existing options object. The resulting
     * builder must carry over the previously configured [ProgressStallTimeout] so that callers
     * do not accidentally lose the override when creating derived options.
     *
     * - Given: an options object with a non-null [ProgressStallTimeout.Interval] override
     * - When: `from(options)` is used to produce a new options object
     * - Then: the clone exposes the same `progressStallTimeout`
     */
    @Test
    fun `from copies progressStallTimeout`() {
        val original = AWSS3StorageUploadFileOptions.builder()
            .progressStallTimeout(ProgressStallTimeout.Interval(seconds = 5L))
            .build()
        val clone = AWSS3StorageUploadFileOptions.from(original).build()
        clone.progressStallTimeout shouldBe original.progressStallTimeout
    }

    /**
     * Equality and hashing must include `progressStallTimeout` so that two options objects
     * that only differ on the override are not considered equal.
     *
     * - Given: two options objects with different [ProgressStallTimeout] overrides
     * - When: `equals`/`hashCode` are evaluated
     * - Then: the objects are not equal
     */
    @Test
    fun `equals and hashCode account for progressStallTimeout`() {
        val a = AWSS3StorageUploadFileOptions.builder()
            .progressStallTimeout(ProgressStallTimeout.Interval(seconds = 10L))
            .build()
        val b = AWSS3StorageUploadFileOptions.builder()
            .progressStallTimeout(ProgressStallTimeout.Disabled)
            .build()
        (a == b) shouldBe false
        (a.hashCode() == b.hashCode()) shouldBe false
    }
}
