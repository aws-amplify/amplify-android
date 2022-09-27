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

package com.amplifyframework.auth.cognito.testutils

import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import java.util.Date

object CredentialStoreUtil {
    private val credential = AmplifyCredential.UserAndIdentityPool(
        SignedInData(
            "",
            "testuser",
            Date(0),
            SignInMethod.SRP,
            DeviceMetadata.Metadata("someDeviceKey", "someDeviceGroupKey", "someSecret"),
            CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", 1212),
        ),
        "identityId",
        AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", 1212)
    )

    fun getDefaultCredential(): AmplifyCredential {
        return credential
    }

    fun setupLegacyStore(context: Context, appClientId: String, userPoolId: String, identityPoolId: String) {
        AWSKeyValueStore(context, "CognitoIdentityProviderCache", true).apply {
            put("CognitoIdentityProvider.$appClientId.testuser.idToken", "idToken")
            put("CognitoIdentityProvider.$appClientId.testuser.accessToken", "accessToken")
            put("CognitoIdentityProvider.$appClientId.testuser.refreshToken", "refreshToken")
            put("CognitoIdentityProvider.$appClientId.testuser.tokenExpiration", "1212")
            put("CognitoIdentityProvider.$appClientId.LastAuthUser", "testuser")
        }

        AWSKeyValueStore(context, "CognitoIdentityProviderDeviceCache.$userPoolId.testuser", true).apply {
            put("DeviceKey", "someDeviceKey")
            put("DeviceGroupKey", "someDeviceGroupKey")
            put("DeviceSecret", "someSecret")
        }

        AWSKeyValueStore(context, "com.amazonaws.android.auth", true).apply {
            put("$identityPoolId.accessKey", "accessKeyId")
            put("$identityPoolId.secretKey", "secretAccessKey")
            put("$identityPoolId.sessionToken", "sessionToken")
            put("$identityPoolId.expirationDate", "1212")
            put("$identityPoolId.identityId", "identityId")
        }
    }
}
