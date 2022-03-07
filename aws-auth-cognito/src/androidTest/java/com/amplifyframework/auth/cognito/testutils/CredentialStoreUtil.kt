package com.amplifyframework.auth.cognito.testutils

import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import com.amplifyframework.auth.cognito.data.AWSCredentials
import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.auth.cognito.data.CognitoUserPoolTokens

object CredentialStoreUtil {
    private val credential = AmplifyCredential(
        CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", 12111),
        "identityId",
        AWSCredentials("accessKeyId", "secretAccessKey", "sessionToken", 12111)
    )

    fun getDefaultCredential() : AmplifyCredential {
        return credential
    }

    fun setupLegacyStore(context: Context, appClientId: String, identityPoolId: String) {
        AWSKeyValueStore(context, "CognitoIdentityProviderCache", true).apply {
            put("CognitoIdentityProvider.$appClientId.testuser.idToken", "idToken")
            put("CognitoIdentityProvider.$appClientId.testuser.accessToken", "accessToken")
            put("CognitoIdentityProvider.$appClientId.testuser.refreshToken", "refreshToken")
            put("CognitoIdentityProvider.$appClientId.testuser.tokenExpiration", "tokenExpiration")
            put("CognitoIdentityProvider.$appClientId.LastAuthUser", "testuser")
        }

        AWSKeyValueStore(context, "com.amazonaws.android.auth", true).apply {
            put("$identityPoolId.accessKey", "accessKeyId")
            put("$identityPoolId.secretKey", "secretAccessKey")
            put("$identityPoolId.sessionToken", "sessionToken")
            put("$identityPoolId.expirationDate", "expiration")
            put("$identityPoolId.identityId", "identityId")
        }
    }
}