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

import com.amplifyframework.core.model.temporal.Temporal
import io.kotest.matchers.shouldBe
import org.junit.Test

/**
 * Tests the client's Gson instance, which is configured to match the plugin's factories rather than
 * shared with them.
 */
class AppSyncGsonTest {

    @Test
    fun `nulls are serialized rather than omitted`() {
        val json = AppSyncGson.instance.toJson(mapOf("name" to null, "id" to "1"))

        // A mutation that clears a field needs the explicit null: an absent field means "leave
        // unchanged" to AppSync, which is a different request.
        json shouldBe """{"name":null,"id":"1"}"""
    }

    @Test
    fun `temporal adapters are registered`() {
        val date = Temporal.Date("2026-01-02")

        AppSyncGson.instance.toJson(date) shouldBe """"2026-01-02""""
    }
}
