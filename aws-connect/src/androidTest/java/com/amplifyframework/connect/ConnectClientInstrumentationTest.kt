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
package com.amplifyframework.connect

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplifyframework.foundation.credentials.AwsCredentials
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the Connect client against a deployed backend
 * (backend-notifications construct).
 *
 * These tests exercise the real Option C wire contract over HTTPS with
 * SigV4 signing — the unit tests use MockWebServer so real signing is
 * not covered there.
 *
 * ## Setup
 *
 * Place an `amplify_outputs.json` in `src/androidTest/res/raw/` with a
 * deployed construct endpoint:
 * ```json
 * {
 *   "notifications": {
 *     "amazon_connect_customer_profiles": {
 *       "aws_region": "us-east-1",
 *       "endpoint": "https://<api-id>.execute-api.<region>.amazonaws.com"
 *     }
 *   }
 * }
 * ```
 *
 * And a `credentials.json` in the same directory with temporary AWS
 * credentials (from a Cognito Identity Pool):
 * ```json
 * {
 *   "accessKeyId": "ASIA...",
 *   "secretAccessKey": "...",
 *   "sessionToken": "..."
 * }
 * ```
 *
 * Both files are gitignored. When absent, all tests in this class are
 * skipped (not failed) with "Backend not configured".
 *
 * ## CI gating
 *
 * This suite is NOT in the unit-test/apiCheck gates that run on every PR.
 * It only runs in an instrumentation lane with a device/emulator and a
 * configured backend.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class ConnectClientInstrumentationTest {

    private lateinit var client: AmplifyConnectClient
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        val config = loadConfig()
        assumeTrue(
            "Backend not configured: place amplify_outputs.json and credentials.json " +
                "in src/androidTest/res/raw/ to enable integration tests.",
            config != null
        )

        val credentials = loadCredentials()
        assumeTrue(
            "Credentials not configured: place credentials.json in " +
                "src/androidTest/res/raw/ to enable integration tests.",
            credentials != null
        )

        client = AmplifyConnectClient(
            configuration = config!!,
            credentialsProvider = { credentials!! },
            context = context,
            platform = "Android",
            appVersion = "1.0.0-test",
            channelType = ChannelType.GCM
        )
    }

    @Test
    fun identifyUser_withProfile_succeeds() = runBlocking {
        client.identifyUser(
            UserProfile(
                email = "integration-test@example.com",
                name = "Integration Test",
                phone = "+15555555555",
                customAttributes = mapOf("testRun" to System.currentTimeMillis().toString()),
                location = UserProfileLocation(
                    city = "Seattle",
                    country = "US",
                    postalCode = "98101",
                    region = "WA"
                )
            )
        )
        // If we reach here without throwing, the endpoint accepted the request.
    }

    @Test
    fun registerDevice_thenRemoveDevice_roundTrip() = runBlocking {
        // First identify (required for the profile to exist)
        client.identifyUser(UserProfile(name = "Device Round-Trip"))

        // Register a device with a fake FCM token
        client.registerDevice("fake-fcm-token-${System.currentTimeMillis()}")

        // Remove the same device (deviceId resolved from SharedPreferences)
        client.removeDevice()
        // If we reach here without throwing, the round-trip succeeded.
    }

    @Test
    fun identifyUser_guestPath_sigV4Signing() = runBlocking {
        // This test exercises the SigV4 signing path.
        // The credentials loaded are Identity Pool guest credentials.
        client.identifyUser(
            UserProfile(
                name = "Guest User",
                customAttributes = mapOf("guestTest" to "true")
            )
        )
    }

    // ------------------------------------------------------------------
    // Config loading helpers
    // ------------------------------------------------------------------

    private fun loadConfig(): ConnectClientConfiguration? {
        val rawId = context.resources.getIdentifier(
            "amplify_outputs",
            "raw",
            context.packageName
        )
        if (rawId == 0) return null
        return try {
            val json = context.resources.openRawResource(rawId)
                .bufferedReader().use { it.readText() }
            val map = jsonToMap(JSONObject(json))
            ConnectClientConfiguration.fromAmplifyOutputs(map)
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("kotlin:S6518")
    private fun loadCredentials(): AwsCredentials? {
        val rawId = context.resources.getIdentifier(
            "credentials",
            "raw",
            context.packageName
        )
        if (rawId == 0) return null
        return try {
            val json = JSONObject(
                context.resources.openRawResource(rawId)
                    .bufferedReader().use { it.readText() }
            )
            AwsCredentials.Temporary(
                accessKeyId = json.getString("accessKeyId"),
                secretAccessKey = json.getString("secretAccessKey"),
                sessionToken = json.getString("sessionToken"),
                expiration = kotlin.time.Instant.DISTANT_FUTURE
            )
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun jsonToMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        json.keys().forEach { key ->
            val value = json.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonToMap(value)
                else -> value
            }
        }
        return map
    }
}
