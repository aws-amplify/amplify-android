/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.data

import android.content.Context
import androidx.core.content.edit
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.auth.cognito.helpers.identityProviderName
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthCredentialStore
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.FederatedToken
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

internal class AWSCognitoLegacyCredentialStore(
    val context: Context,
    private val authConfiguration: AuthConfiguration,
    private val keyValueRepoFactory: KeyValueRepositoryFactory = KeyValueRepositoryFactory()
) : AuthCredentialStore {

    companion object {
        const val AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER: String = "com.amazonaws.android.auth"
        const val AWS_MOBILE_CLIENT_PROVIDER = "com.amazonaws.mobile.client"
        const val APP_TOKENS_INFO_CACHE = "CognitoIdentityProviderCache"
        const val APP_DEVICE_INFO_CACHE = "CognitoIdentityProviderDeviceCache"
        const val LOCAL_STORAGE_PATH = "AWS.Cognito.ContextData"

        private const val DEVICE_KEY = "DeviceKey"
        private const val DEVICE_GROUP_KEY = "DeviceGroupKey"
        private const val DEVICE_SECRET_KEY = "DeviceSecret"
        private const val LOCAL_STORAGE_DEVICE_ID_KEY = "CognitoDeviceId"

        private const val ID_KEY: String = "identityId"
        private const val AK_KEY: String = "accessKey"
        private const val SK_KEY: String = "secretKey"
        private const val ST_KEY: String = "sessionToken"
        private const val EXP_KEY: String = "expirationDate"

        private const val APP_LAST_AUTH_USER = "LastAuthUser"
        private const val APP_LOCAL_CACHE_KEY_PREFIX = "CognitoIdentityProvider"

        private const val TOKEN_TYPE_ID = "idToken"
        private const val TOKEN_TYPE_ACCESS = "accessToken"
        private const val TOKEN_TYPE_REFRESH = "refreshToken"

        // TODO check if below exists
        private const val TOKEN_EXPIRATION = "tokenExpiration"

        // Mobile Client Keys
        const val PROVIDER_KEY = "provider"
        const val SIGN_IN_MODE_KEY = "signInMode"
        const val TOKEN_KEY = "token"
    }

    private val userDeviceDetailsCacheKeyPrefix = "$APP_DEVICE_INFO_CACHE.${authConfiguration.userPool?.poolId}."
    private val userDeviceDetailsCacheKey = "$APP_DEVICE_INFO_CACHE.${authConfiguration.userPool?.poolId}.%s"

    private val idAndCredentialsKeyValue: KeyValueRepository by lazy {
        keyValueRepoFactory.create(context, AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER)
    }

    private val mobileClientKeyValue: KeyValueRepository by lazy {
        keyValueRepoFactory.create(context, AWS_MOBILE_CLIENT_PROVIDER)
    }

    private val tokensKeyValue: KeyValueRepository by lazy {
        keyValueRepoFactory.create(context, APP_TOKENS_INFO_CACHE)
    }

    private lateinit var deviceKeyValue: KeyValueRepository

    override fun saveCredential(credential: AmplifyCredential) = Unit
    override fun saveDeviceMetadata(username: String, deviceMetadata: DeviceMetadata) = Unit
    override fun saveASFDevice(device: AmplifyCredential.ASFDevice) = Unit

    @Synchronized
    fun retrieveLastAuthUserId(): String? {
        val keys = getTokenKeys()
        return keys[APP_LAST_AUTH_USER]?.let { tokensKeyValue.get(it) }
    }

    @Synchronized
    override fun retrieveCredential(): AmplifyCredential {
        val signedInData = retrieveSignedInData()
        val awsCredentials = retrieveAWSCredentials()
        val identityId = retrieveIdentityId()

        return when {
            awsCredentials != null && identityId != null -> {
                val federateToIdentityPoolToken = retrieveFederateToIdentityPoolToken()
                when {
                    signedInData != null -> {
                        AmplifyCredential.UserAndIdentityPool(signedInData, identityId, awsCredentials)
                    }
                    federateToIdentityPoolToken != null -> {
                        AmplifyCredential.IdentityPoolFederated(
                            federateToIdentityPoolToken,
                            identityId,
                            awsCredentials
                        )
                    }
                    else -> {
                        AmplifyCredential.IdentityPool(identityId, awsCredentials)
                    }
                }
            }
            signedInData != null -> {
                AmplifyCredential.UserPool(signedInData)
            }
            else -> AmplifyCredential.Empty
        }
    }

    @Synchronized
    override fun retrieveASFDevice(): AmplifyCredential.ASFDevice {
        val sharedPreferences = context.getSharedPreferences(LOCAL_STORAGE_PATH, Context.MODE_PRIVATE)
        val deviceId = sharedPreferences?.getString(LOCAL_STORAGE_DEVICE_ID_KEY, null)
        return AmplifyCredential.ASFDevice(deviceId)
    }

    override fun deleteCredential() {
        deleteAWSCredentials()
        deleteIdentityId()
        deleteCognitoUserPoolTokens()
    }

    override fun deleteDeviceKeyCredential(username: String) {
        val keys = getTokenKeys()
        keys[APP_LAST_AUTH_USER]?.let { tokensKeyValue.remove(it) }

        deviceKeyValue.apply {
            remove(DEVICE_KEY)
            remove(DEVICE_GROUP_KEY)
            remove(DEVICE_SECRET_KEY)
        }
    }

    override fun deleteASFDevice() {
        val sharedPreferences = context.getSharedPreferences(LOCAL_STORAGE_PATH, Context.MODE_PRIVATE)
        sharedPreferences.edit { remove(LOCAL_STORAGE_DEVICE_ID_KEY) }
    }

    private fun deleteCognitoUserPoolTokens() {
        val keys = getTokenKeys()
        keys[TOKEN_TYPE_ID]?.let { tokensKeyValue.remove(it) }
        keys[TOKEN_TYPE_ACCESS]?.let { tokensKeyValue.remove(it) }
        keys[TOKEN_TYPE_REFRESH]?.let { tokensKeyValue.remove(it) }
        keys[TOKEN_EXPIRATION]?.let { tokensKeyValue.remove(it) }
    }

    private fun deleteIdentityId() {
        idAndCredentialsKeyValue.remove(namespace(ID_KEY))
    }

    private fun deleteAWSCredentials() {
        idAndCredentialsKeyValue.apply {
            remove(namespace(AK_KEY))
            remove(namespace(SK_KEY))
            remove(namespace(ST_KEY))
            remove(namespace(EXP_KEY))
        }
    }

    private fun retrieveAWSCredentials(): AWSCredentials? {
        val accessKey = idAndCredentialsKeyValue.get(namespace(AK_KEY))
        val secretKey = idAndCredentialsKeyValue.get(namespace(SK_KEY))
        val sessionToken = idAndCredentialsKeyValue.get(namespace(ST_KEY))

        // V2 AWSCredential expiration is in epoch seconds whereas legacy expiration may be in epoch milliseconds
        // Session expiration should be within 24 hours so if we see a date in the far future, we can assume the
        // timestamp is encoded in milliseconds.
        val expiration = idAndCredentialsKeyValue.get(namespace(EXP_KEY))?.toLongOrNull()?.let {
            if (Instant.ofEpochSecond(it).isAfter(Instant.now().plus(365, ChronoUnit.DAYS))) {
                it / 1000
            } else {
                it
            }
        }

        return if (accessKey == null && secretKey == null && sessionToken == null) {
            null
        } else {
            AWSCredentials(accessKey, secretKey, sessionToken, expiration)
        }
    }

    private fun retrieveIdentityId() = idAndCredentialsKeyValue.get(namespace(ID_KEY))

    private fun retrieveSignedInData(): SignedInData? {
        val keys = getTokenKeys()
        val cognitoUserPoolTokens = retrieveCognitoUserPoolTokens(keys) ?: return null
        val signInMethod = retrieveUserPoolSignInMethod() ?: return null
        val tokenUserId =
            try {
                cognitoUserPoolTokens.accessToken?.let { SessionHelper.getUserSub(it) } ?: ""
            } catch (e: Exception) {
                ""
            }
        val tokenUsername =
            try {
                cognitoUserPoolTokens.accessToken?.let { SessionHelper.getUsername(it) } ?: ""
            } catch (e: Exception) {
                ""
            }

        return SignedInData(
            tokenUserId,
            tokenUsername,
            Date(0),
            signInMethod,
            cognitoUserPoolTokens
        )
    }

    /*
    During migration away from the legacy credential store, we need to find all shared preference files that store
    device metadata. These filenames contain the real username (not aliased) for the tracked device metadata.
     */
    fun retrieveDeviceMetadataUsernameList(): List<String> {
        return try {
            val sharedPrefsSuffix = ".xml"
            File(context.dataDir, "shared_prefs").listFiles { _, filename ->
                filename.startsWith(userDeviceDetailsCacheKeyPrefix) && filename.endsWith(sharedPrefsSuffix)
            }?.map { it.name.substringAfter(userDeviceDetailsCacheKeyPrefix).substringBefore(sharedPrefsSuffix) }
                ?.filter { it.isNotBlank() } ?: emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    @Synchronized
    override fun retrieveDeviceMetadata(username: String): DeviceMetadata {
        val deviceDetailsCacheKey = String.format(userDeviceDetailsCacheKey, username)
        deviceKeyValue = keyValueRepoFactory.create(context, deviceDetailsCacheKey)

        val deviceKey = deviceKeyValue.get(DEVICE_KEY)
        val deviceGroupKey = deviceKeyValue.get(DEVICE_GROUP_KEY)
        val deviceSecretKey = deviceKeyValue.get(DEVICE_SECRET_KEY)

        return if (deviceKey.isNullOrEmpty() && deviceGroupKey.isNullOrEmpty()) {
            DeviceMetadata.Empty
        } else {
            DeviceMetadata.Metadata(deviceKey ?: "", deviceGroupKey ?: "", deviceSecretKey)
        }
    }

    private fun retrieveCognitoUserPoolTokens(keys: Map<String, String>): CognitoUserPoolTokens? {
        val idToken = keys[TOKEN_TYPE_ID]?.let { tokensKeyValue.get(it) }
        val accessToken = keys[TOKEN_TYPE_ACCESS]?.let { tokensKeyValue.get(it) }
        val refreshToken = keys[TOKEN_TYPE_REFRESH]?.let { tokensKeyValue.get(it) }
        val expiration = keys[TOKEN_EXPIRATION]?.let { tokensKeyValue.get(it) }?.toLongOrNull()

        return if (idToken == null && accessToken == null && refreshToken == null) {
            null
        } else {
            CognitoUserPoolTokens(idToken, accessToken, refreshToken, expiration)
        }
    }

    private fun getTokenKeys(): Map<String, String> {
        val appClient = authConfiguration.userPool?.appClient

        val userIdTokenKey = String.format(
            Locale.US,
            "%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            APP_LAST_AUTH_USER
        )

        val userId = tokensKeyValue.get(userIdTokenKey)

        val cachedIdTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            userId,
            TOKEN_TYPE_ID
        )
        val cachedAccessTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            userId,
            TOKEN_TYPE_ACCESS
        )
        val cachedRefreshTokenKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            userId,
            TOKEN_TYPE_REFRESH
        )

        val cachedTokenExpirationKey = String.format(
            Locale.US,
            "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX,
            appClient,
            userId,
            TOKEN_EXPIRATION
        )

        return mapOf(
            APP_LAST_AUTH_USER to userIdTokenKey,
            TOKEN_TYPE_ID to cachedIdTokenKey,
            TOKEN_TYPE_ACCESS to cachedAccessTokenKey,
            TOKEN_TYPE_REFRESH to cachedRefreshTokenKey,
            TOKEN_EXPIRATION to cachedTokenExpirationKey
        )
    }

    // prefix the key with identity pool id
    private fun namespace(key: String): String = getIdentityPoolId() + "." + key

    private fun getIdentityPoolId(): String? = authConfiguration.identityPool?.poolId

    private fun retrieveUserPoolSignInMethod() = when (mobileClientKeyValue.get(SIGN_IN_MODE_KEY)) {
        /*
        In almost all cases, federation will be enabled making "1" the most common value. Due to how mobile client
        stores credentials, it is difficult to determine the sign in method. If the stored provider matches a
        federateToIdentityPool provider, we return null so we can store the Federated Token. Otherwise, we will return
        ApiBased with an UNKNOWN method. We will use additional logic in the sign out check to determine if we should
        attempt hosted ui based sign out.
         */
        "1" -> {
            if (retrieveFederateToIdentityPoolToken() != null) {
                null
            } else {
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.UNKNOWN)
            }
        }
        // This is an unlikely as hosted ui with federation will automatically change to "1".
        // Disabling federation was only possible if the escape hatch was used.
        "2" -> SignInMethod.HostedUI()
        "3" -> null
        else -> SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
    }

    // only retrieve federate to identity pool token if stored provider is in list of known AuthProvider values
    private fun retrieveFederateToIdentityPoolToken(): FederatedToken? {
        val provider = mobileClientKeyValue.get(PROVIDER_KEY) ?: return null
        val token = mobileClientKeyValue.get(TOKEN_KEY) ?: return null

        val federatedIdentityPoolProviders = listOf(
            AuthProvider.amazon().identityProviderName,
            AuthProvider.facebook().identityProviderName,
            AuthProvider.apple().identityProviderName,
            AuthProvider.google().identityProviderName
        )

        return if (federatedIdentityPoolProviders.contains(provider)) {
            FederatedToken(provider, token)
        } else {
            null
        }
    }
}
