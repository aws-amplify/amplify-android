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
import java.io.File
import java.util.Date

internal class CredentialStoreUtil {
    private val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwidXNlcm5hbWUiO" +
        "iJhbXBsaWZ5X3VzZXIiLCJpYXQiOjE1MTYyMzkwMjJ9.zBiQ0guLRX34pUEYLPyDxQAyDDlXmL0JY7kgPWAHZos"

    private val credential = AmplifyCredential.UserAndIdentityPool(
        SignedInData(
            "1234567890",
            "amplify_user",
            Date(0),
            SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            CognitoUserPoolTokens(
                "idToken",
                accessToken,
                "refreshToken",
                1212
            ),
        ),
        "identityId",
        AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", 1212)
    )

    fun getDefaultCredential(): AmplifyCredential {
        return credential
    }

    val user1Username = "2924030b-54c0-48bc-8bff-948418fba949"
    val user2Username = "7e001127-5f11-41fb-9d10-ab9d6cf41dba"

    fun getUser1DeviceMetadata(): DeviceMetadata.Metadata {
        return DeviceMetadata.Metadata(
            "DeviceKey1",
            "DeviceGroupKey1",
            "DeviceSecret1"
        )
    }

    fun getUser2DeviceMetadata(): DeviceMetadata.Metadata {
        return DeviceMetadata.Metadata(
            "DeviceKey2",
            "DeviceGroupKey2",
            "DeviceSecret2"
        )
    }

    fun setupLegacyStore(context: Context, appClientId: String, userPoolId: String, identityPoolId: String) {

        clearSharedPreferences(context)

        AWSKeyValueStore(context, "CognitoIdentityProviderCache", true).apply {
            put("CognitoIdentityProvider.$appClientId.testuser.idToken", "idToken")
            put("CognitoIdentityProvider.$appClientId.testuser.accessToken", accessToken)
            put("CognitoIdentityProvider.$appClientId.testuser.refreshToken", "refreshToken")
            put("CognitoIdentityProvider.$appClientId.testuser.tokenExpiration", "1212")
            put("CognitoIdentityProvider.$appClientId.LastAuthUser", "testuser")
        }

        AWSKeyValueStore(context, "CognitoIdentityProviderDeviceCache.$userPoolId.$user1Username", true).apply {
            put("DeviceKey", "DeviceKey1")
            put("DeviceGroupKey", "DeviceGroupKey1")
            put("DeviceSecret", "DeviceSecret1")
        }

        AWSKeyValueStore(context, "CognitoIdentityProviderDeviceCache.$userPoolId.$user2Username", true).apply {
            put("DeviceKey", "DeviceKey2")
            put("DeviceGroupKey", "DeviceGroupKey2")
            put("DeviceSecret", "DeviceSecret2")
        }

        AWSKeyValueStore(context, "com.amazonaws.android.auth", true).apply {
            put("$identityPoolId.accessKey", "accessKeyId")
            put("$identityPoolId.secretKey", "secretAccessKey")
            put("$identityPoolId.sessionToken", "sessionToken")
            put("$identityPoolId.expirationDate", "1212")
            put("$identityPoolId.identityId", "identityId")
        }

        // we need to wait for shared prefs to actually hit filesystem as we always use apply instead of commit
        val beginWait = System.currentTimeMillis()
        while (System.currentTimeMillis() - beginWait < 3000) {
            if ((File(context.dataDir, "shared_prefs").listFiles()?.size ?: 0) >= 4) {
                break
            } else {
                Thread.sleep(50)
            }
        }
    }

    fun saveLegacyDeviceMetadata(
        context: Context,
        userPoolId: String,
        username: String,
        deviceMetadata: DeviceMetadata.Metadata
    ) {
        val prefsName = "CognitoIdentityProviderDeviceCache.$userPoolId.$username"
        AWSKeyValueStore(
            context,
            "CognitoIdentityProviderDeviceCache.$userPoolId.$username", true
        ).apply {
            put("DeviceKey", deviceMetadata.deviceKey)
            put("DeviceGroupKey", deviceMetadata.deviceGroupKey)
            put("DeviceSecret", deviceMetadata.deviceSecret)
        }

        // we need to wait for shared prefs to actually hit filesystem as we always use apply instead of commit
        val beginWait = System.currentTimeMillis()
        while (System.currentTimeMillis() - beginWait < 3000) {
            if (File(context.dataDir, "shared_prefs/$prefsName.xml").exists()) {
                break
            } else {
                Thread.sleep(50)
            }
        }
    }

    fun clearSharedPreferences(context: Context) {
        File(context.dataDir, "shared_prefs").listFiles()?.forEach { it.delete() }
    }
}
