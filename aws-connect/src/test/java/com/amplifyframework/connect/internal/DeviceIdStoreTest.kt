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
package com.amplifyframework.connect.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceIdStoreTest {

    private lateinit var context: Context
    private lateinit var store: DeviceIdStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any leftover state from previous tests
        context.getSharedPreferences(
            DeviceIdStore.PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        store = DeviceIdStore(context)
    }

    @Test
    fun `getOrCreate generates a UUID when no value exists`() {
        val id = store.getOrCreate()
        id shouldNotBe null
        id shouldMatch "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    }

    @Test
    fun `getOrCreate returns the same value on subsequent calls`() {
        val first = store.getOrCreate()
        val second = store.getOrCreate()
        second shouldBe first
    }

    @Test
    fun `get returns null when no value exists`() {
        store.get() shouldBe null
    }

    @Test
    fun `get returns the value after getOrCreate`() {
        val id = store.getOrCreate()
        store.get() shouldBe id
    }

    @Test
    fun `clear removes the stored value`() {
        store.getOrCreate()
        store.clear()
        store.get() shouldBe null
    }

    @Test
    fun `getOrCreate generates a new value after clear`() {
        val first = store.getOrCreate()
        store.clear()
        val second = store.getOrCreate()
        second shouldNotBe first
    }

    @Test
    fun `shared-key contract - reads value written by enrichment under same key`() {
        // Simulate what enrichment's SharedPreferencesClientIdProvider does:
        // writes a UUID to SharedPreferences file "com.amplifyframework.device_id"
        // under key "com.amplifyframework.device_id"
        val enrichmentWrittenId = "enrichment-generated-uuid-1234"
        context.getSharedPreferences(
            "com.amplifyframework.device_id", // enrichment's PREFERENCES_NAME
            Context.MODE_PRIVATE
        ).edit().putString(
            "com.amplifyframework.device_id", // enrichment's CLIENT_ID_STORAGE_KEY
            enrichmentWrittenId
        ).commit()

        // Connect's DeviceIdStore should read the same value
        val connectStore = DeviceIdStore(context)
        connectStore.getOrCreate() shouldBe enrichmentWrittenId
        connectStore.get() shouldBe enrichmentWrittenId
    }

    @Test
    fun `shared-key contract - value written by Connect is readable by enrichment`() {
        // Connect generates the id first
        val connectId = store.getOrCreate()

        // Enrichment reads from the same SharedPreferences file and key
        val enrichmentValue = context.getSharedPreferences(
            "com.amplifyframework.device_id",
            Context.MODE_PRIVATE
        ).getString("com.amplifyframework.device_id", null)

        enrichmentValue shouldBe connectId
    }

    @Test
    fun `uses correct SharedPreferences file and key constants`() {
        // Verify the constants match the cross-package contract
        DeviceIdStore.PREFERENCES_NAME shouldBe "com.amplifyframework.device_id"
        DeviceIdStore.DEVICE_ID_KEY shouldBe "com.amplifyframework.device_id"
    }
}
