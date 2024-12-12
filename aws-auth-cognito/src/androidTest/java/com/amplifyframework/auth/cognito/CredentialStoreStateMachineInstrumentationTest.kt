/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.testutils.AuthConfigurationProvider
import com.amplifyframework.auth.cognito.testutils.CredentialStoreUtil
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.google.gson.Gson
import junit.framework.TestCase.assertEquals
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialStoreStateMachineInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val credentialStoreUtil = CredentialStoreUtil()

    private val configuration = AuthConfigurationProvider.getAuthConfigurationObject()
    private val userPoolId = configuration.userPool.userPool.PoolId
    private val identityPoolId = configuration.credentials.cognitoIdentity.identityData.PoolId
    private val userPoolAppClientId = configuration.userPool.userPool.AppClientId

    @Before
    fun setup() {
        credentialStoreUtil.setupLegacyStore(context, userPoolAppClientId, userPoolId, identityPoolId)
    }

    @After
    fun tearDown() {
        credentialStoreUtil.clearSharedPreferences(context)
    }

    private val authConfigJson = JSONObject(Gson().toJson(configuration))

    @Test
    fun test_CredentialStore_Migration_Succeeds_On_Plugin_Configuration() {
        val plugin = AWSCognitoAuthPlugin()
        plugin.configure(authConfigJson, context)
        plugin.initialize(context)

        val credentialStore = AWSCognitoAuthCredentialStore(
            context,
            AuthConfiguration.fromJson(authConfigJson)
        )

        assertEquals(credentialStoreUtil.getDefaultCredential(), credentialStore.retrieveCredential())
        assertEquals(
            credentialStoreUtil.getUser1DeviceMetadata(),
            credentialStore.retrieveDeviceMetadata(credentialStoreUtil.user1UserId)
        )
        assertEquals(
            credentialStoreUtil.getUser2DeviceMetadata(),
            credentialStore.retrieveDeviceMetadata(credentialStoreUtil.user2UserId)
        )
    }

    @Test
    fun test_CredentialStore_Missing_DeviceMetadata_Migration_Succeeds_On_Plugin_Configuration() {
        // GIVEN
        val userAUsername = "userA"
        val expectedUserADeviceMetadata = DeviceMetadata.Metadata("A", "B", "C")
        val userBUsername = "userB"
        val expectedUserBDeviceMetadata = DeviceMetadata.Metadata("1", "2", "3")

        AWSCognitoAuthPlugin().apply {
            configure(authConfigJson, context)
            initialize(context)
        }

        AWSCognitoAuthCredentialStore(
            context,
            AuthConfiguration.fromJson(authConfigJson)
        ).apply {
            saveDeviceMetadata("userA", expectedUserADeviceMetadata)
        }

        // WHEN
        // Simulating missed device metadata migration from issue 2929
        // We expect this to not migrate as it will conflict with existing metadata already saved
        credentialStoreUtil.saveLegacyDeviceMetadata(
            context,
            userPoolId,
            userAUsername,
            DeviceMetadata.Metadata("X", "Y", "Z")
        )

        // We expect this to migrate as it does not conflict with any existing saved metadata
        credentialStoreUtil.saveLegacyDeviceMetadata(
            context,
            userPoolId,
            userBUsername,
            expectedUserBDeviceMetadata
        )

        // THEN

        // Initialize plugin again to complete migration of missing device metadata
        AWSCognitoAuthPlugin().apply {
            configure(authConfigJson, context)
            initialize(context)
        }

        // WHEN
        val credentialStore = AWSCognitoAuthCredentialStore(
            context,
            AuthConfiguration.fromJson(authConfigJson)
        )

        // Expect the device metadata for user A to have not changed from data that was already saved in v2 store
        assertEquals(
            expectedUserADeviceMetadata,
            credentialStore.retrieveDeviceMetadata(userAUsername)
        )
        // Expect the device metadata for user A to have not changed from data that was already saved in v2 store
        assertEquals(
            expectedUserBDeviceMetadata,
            credentialStore.retrieveDeviceMetadata(userBUsername)
        )
    }
}
