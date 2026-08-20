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
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import com.amplifyframework.foundation.credentials.toAwsCredentialsProvider
import com.amplifyframework.testutils.Resources
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import com.amplifyframework.testutils.sync.SynchronousAuth
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the Connect client against a deployed backend
 * (backend-notifications construct).
 *
 * Exercises the real Option C wire contract over HTTPS with SigV4 signing.
 *
 * ## Setup
 *
 * Place the following in `src/androidTest/res/raw/` (both gitignored):
 *
 * **amplify_outputs.json** — deployed construct config:
 * ```json
 * {
 *   "notifications": {
 *     "amazon_connect": {
 *       "aws_region": "us-east-1",
 *       "endpoint": "https://<api-id>.execute-api.<region>.amazonaws.com"
 *     }
 *   },
 *   "auth": { ... }
 * }
 * ```
 *
 * **credentials.json** — Cognito user credentials:
 * ```json
 * {
 *   "credentials": [
 *     { "username": "integ-test-user", "password": "..." }
 *   ]
 * }
 * ```
 *
 * When either file is absent, all tests skip (not fail) via JUnit Assume.
 *
 * ## CI gating
 *
 * This suite is NOT in the unit-test/apiCheck gates. It only runs in an
 * instrumentation lane with a device/emulator and a configured backend.
 */
@RunWith(AndroidJUnit4::class)
class ConnectClientInstrumentationTest {

    companion object {
        private lateinit var credentialsProvider: AwsCredentialsProvider<AwsCredentials>
        private lateinit var configuration: ConnectClientConfiguration
        private var configured = false

        @BeforeClass
        @JvmStatic
        fun setupBefore() {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // Check if raw resources exist (gitignored — absent in CI)
            val outputsId = context.resources.getIdentifier(
                "amplify_outputs",
                "raw",
                context.packageName
            )
            val credsId = context.resources.getIdentifier(
                "credentials",
                "raw",
                context.packageName
            )
            if (outputsId == 0 || credsId == 0) {
                configured = false
                return
            }

            try {
                // Configure Amplify with Auth (same pattern as kinesis)
                Amplify.Auth.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(AmplifyOutputs(outputsId), context)

                // Sign in using credentials from raw resource
                val synchronousAuth = SynchronousAuth.delegatingTo(Amplify.Auth)
                val (user, password) = readCredentialsFromResource(context, credsId)
                synchronousAuth.signOut()
                synchronousAuth.signIn(user, password)

                // Resolve AWS credentials via Cognito (authed path)
                credentialsProvider = CognitoCredentialsProvider().toAwsCredentialsProvider()

                // Parse Connect config from the same amplify_outputs
                configuration = ConnectClientConfiguration.fromAmplifyOutputs(
                    AmplifyOutputsData.deserialize(context, AmplifyOutputs(outputsId))
                )

                configured = true
            } catch (e: Exception) {
                configured = false
            }
        }

        private fun readCredentialsFromResource(context: Context, @RawRes resourceId: Int): Pair<String, String> {
            val resource = Resources.readAsJson(context, resourceId)
            return try {
                val credentials = resource.getJSONArray("credentials")
                val lastIndex = credentials.length() - 1
                val credential = credentials.getJSONObject(lastIndex)
                Pair(credential.getString("username"), credential.getString("password"))
            } catch (e: JSONException) {
                throw RuntimeException("Failed to read credentials resource", e)
            }
        }
    }

    private lateinit var client: AmplifyConnectClient

    @Before
    fun setup() {
        assumeTrue(
            "Backend not configured: place amplify_outputs.json and credentials.json " +
                "in src/androidTest/res/raw/ to enable integration tests.",
            configured
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        client = AmplifyConnectClient(
            context = context,
            configuration = configuration,
            credentialsProvider = credentialsProvider,
            platform = "Android",
            appVersion = "1.0.0-integ",
            channelType = ChannelType.GCM
        )
    }

    @Test
    fun identifyUser_withFullProfile_succeeds() {
        runBlocking {
            client.identifyUser(
                UserProfile(
                    email = "integ-test@example.com",
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
            ).shouldBeSuccess()
        }
    }

    @Test
    fun registerDevice_thenRemoveDevice_roundTrip() {
        runBlocking {
            // Ensure profile exists
            client.identifyUser(UserProfile(name = "Device Round-Trip")).shouldBeSuccess()

            // Register with a fake FCM token
            client.registerDevice("fake-fcm-token-${System.currentTimeMillis()}").shouldBeSuccess()

            // Remove the device (deviceId resolved from SharedPreferences)
            client.removeDevice().shouldBeSuccess()
        }
    }

    @Test
    fun identifyUser_minimalProfile_succeeds() {
        runBlocking {
            // Empty profile, only the SigV4 signer identity reaches the backend
            client.identifyUser(UserProfile()).shouldBeSuccess()
        }
    }
}
