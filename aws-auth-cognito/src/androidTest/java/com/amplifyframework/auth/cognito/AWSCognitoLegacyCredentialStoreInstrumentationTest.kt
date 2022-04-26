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
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.auth.cognito.testutils.AuthConfigurationProvider
import com.amplifyframework.auth.cognito.testutils.CredentialStoreUtil
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AWSCognitoLegacyCredentialStoreInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val configuration: AuthConfiguration = AuthConfigurationProvider.getAuthConfiguration()

    private val credential = CredentialStoreUtil.getDefaultCredential()

    private lateinit var store: AWSCognitoLegacyCredentialStore

    @Before
    fun setup() {
        store = AWSCognitoLegacyCredentialStore(context, configuration)
        //TODO: Pull the appClientID from the configuration instead of hardcoding
        CredentialStoreUtil.setupLegacyStore(context, "userPoolAppClientId", "identityPoolId")
    }

    @Test
    fun test_legacy_store_implementation_can_retrieve_credentials_stored_using_aws_sdk() {
        val creds = store.retrieveCredential()
        assertTrue(creds == credential)
    }
}
