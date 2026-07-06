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
package com.amplifyframework.eventenrichment.clientid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesClientIdProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun preferences() =
        context.getSharedPreferences(SharedPreferencesClientIdProvider.PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Test
    fun `uses the shared cross-package storage key`() {
        SharedPreferencesClientIdProvider.CLIENT_ID_STORAGE_KEY shouldBe "com.amplifyframework.device_id"
    }

    @Test
    fun `creates and persists a new client id when none exists`() {
        val provider = SharedPreferencesClientIdProvider(context)

        val id = provider.getClientId()
        id.shouldNotBeEmpty()

        // Persisted under the shared key so other Amplify clients read the same id.
        preferences().getString(SharedPreferencesClientIdProvider.CLIENT_ID_STORAGE_KEY, null) shouldBe id
    }

    @Test
    fun `returns the existing client id on subsequent reads`() {
        val provider = SharedPreferencesClientIdProvider(context)
        val first = provider.getClientId()
        val second = provider.getClientId()
        second shouldBe first
    }

    @Test
    fun `reads an id written under the shared key by another client`() {
        preferences().edit()
            .putString(SharedPreferencesClientIdProvider.CLIENT_ID_STORAGE_KEY, "existing-shared-id")
            .apply()

        SharedPreferencesClientIdProvider(context).getClientId() shouldBe "existing-shared-id"
    }
}
