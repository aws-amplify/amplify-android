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
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.google.gson.Gson
import junit.framework.Assert.assertTrue
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialStoreStateMachineInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val configuration = AuthConfigurationProvider.getAuthConfigurationObject()
    private val identityPoolId = configuration.credentials.cognitoIdentity.identityData.PoolId
    private val userPoolAppClientId = configuration.userPool.userPool.AppClientId

    private val credential = CredentialStoreUtil.getDefaultCredential()

    @Before
    fun setup() {
        CredentialStoreUtil.setupLegacyStore(context, userPoolAppClientId, identityPoolId)
    }

    private val authConfigJson = JSONObject(Gson().toJson(configuration))

    @Test
    fun test_CredentialStore_Migration_Succeeds_On_Plugin_Configuration() {
        AWSCognitoAuthPlugin().configure(authConfigJson, context)

        val credentialStore = AWSCognitoAuthCredentialStore(
            context,
            AuthConfiguration.fromJson(authConfigJson).build()
        )
        val creds = credentialStore.retrieveCredential()

        assertTrue(creds == credential)
    }
}
