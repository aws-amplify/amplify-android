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
package com.amplifyframework.eventenrichment

import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class EventEnrichmentClientOptionsTest {

    @Test
    fun `defaults enable auto session tracking with a five second timeout`() {
        val options = EventEnrichmentClientOptions.defaults()
        options.autoSessionTracking shouldBe true
        options.sessionTimeout shouldBe 5.seconds
    }

    @Test
    fun `builder overrides are applied`() {
        val options = EventEnrichmentClientOptions.builder()
            .autoSessionTracking(false)
            .sessionTimeout(30.seconds)
            .build()

        options.autoSessionTracking shouldBe false
        options.sessionTimeout shouldBe 30.seconds
    }

    @Test
    fun `dsl invoke configures options`() {
        val options = EventEnrichmentClientOptions {
            autoSessionTracking = false
            sessionTimeout = 10.seconds
        }

        options.autoSessionTracking shouldBe false
        options.sessionTimeout shouldBe 10.seconds
    }
}
