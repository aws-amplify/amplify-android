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

package com.amplifyframework.auth.plugins.core

import android.content.Context
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.plugins.core.data.AWSCognitoIdentityPoolConfiguration
import com.amplifyframework.auth.plugins.core.data.AWSCredentialsInternal
import com.amplifyframework.auth.plugins.core.data.AuthCredentialStore
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.core.Amplify
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LoginProvider(val name: String, val userIdentifier: String)

/**
 * Provides access to Cognito Identity Client and API to to fetch identity Id and AWS Credentials
 * by exchanging OIDC tokens. AWS credentials are auto refreshes if expired.
 *
 * @param context android context
 * @param identityPool Cognito Identity Pool configuration
 * @param pluginKey unique plugin key
 * @param pluginVersion plugin version name, default is 1.0.0
 */
class AWSCognitoIdentityPoolOperations(
    context: Context,
    private val identityPool: AWSCognitoIdentityPoolConfiguration,
    pluginKey: String,
    pluginVersion: String = "1.0.0"
) {
    companion object {
        const val OIDC_PLUGIN_LOG_NAMESPACE = "amplify:oidc-plugin:%s"
    }

    private val logger = Amplify.Logging.forNamespace(OIDC_PLUGIN_LOG_NAMESPACE.format(this::class.java.simpleName))

    private val semVerRegex = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)?\$".toRegex()
    private val pluginKeySanitized = pluginKey.take(25).filter { it.isLetterOrDigit() }
    private val pluginVersionSanitized = pluginVersion.take(10).takeIf {
        logger.warn("Plugin version does not match semantic versioning rules, version set to 1.0.0")
        it.matches(semVerRegex)
    } ?: "1.0.0"

    private val KEY_LOGINS_PROVIDER = "amplify.${identityPool.poolId}.session.loginsProvider"
    private val KEY_IDENTITY_ID = "amplify.${identityPool.poolId}.session.identityId"
    private val KEY_AWS_CREDENTIALS = "amplify.${identityPool.poolId}.session.credential"
    private val awsAuthCredentialStore = AuthCredentialStore(context.applicationContext, pluginKeySanitized, true)

    val cognitoIdentityClient = CognitoClientFactory.createIdentityClient(
        identityPool,
        pluginKeySanitized,
        pluginVersionSanitized
    )

    private fun isValidSession(awsCredentials: AWSCredentialsInternal): Boolean {
        val currentTimeStamp = Instant.now()
        val isValid = currentTimeStamp < awsCredentials.expiration?.let { Instant.ofEpochSecond(it) }
        logger.verbose("fetchAWSCognitoIdentityPoolDetails: is AWS session valid? $isValid")
        return isValid
    }

    private suspend fun getIdentityId(logins: List<LoginProvider>): String {
        return try {
            val loginsMap = logins.associate { it.name to it.userIdentifier }
            val request = GetIdRequest {
                this.identityPoolId = this@AWSCognitoIdentityPoolOperations.identityPool.poolId
                this.logins = loginsMap
            }

            val response = cognitoIdentityClient.getId(request)
            logger.verbose("getIdentityId: fetched identity id")
            response.identityId?.let { return@let it } ?: throw Exception("Fetching identity id failed.")
        } catch (notAuthorized: aws.sdk.kotlin.services.cognitoidentity.model.NotAuthorizedException) {
            logger.verbose("getIdentityId: guest access disabled")
            throw NotAuthorizedException(
                recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED,
                cause = notAuthorized
            )
        } catch (e: Exception) {
            throw SignedOutException(
                recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_POSSIBLE,
                cause = e
            )
        }
    }

    private suspend fun getAWSCredentials(
        identityId: String?,
        logins: List<LoginProvider>
    ): Pair<String?, AWSCredentialsInternal> {
        return try {
            val loginsMap = logins.associate { it.name to it.userIdentifier }
            val request = GetCredentialsForIdentityRequest {
                this.identityId = identityId
                this.logins = loginsMap
            }

            val response = cognitoIdentityClient.getCredentialsForIdentity(request)
            logger.verbose("getAWSCredentials: fetched AWS credentials")
            response.credentials?.let {
                return@let (
                    response.identityId to AWSCredentialsInternal(
                        it.accessKeyId,
                        it.secretKey,
                        it.sessionToken,
                        it.expiration?.epochSeconds
                    )
                    )
            } ?: throw Exception("Fetching AWS credentials failed.")
        } catch (notAuthorized: aws.sdk.kotlin.services.cognitoidentity.model.NotAuthorizedException) {
            logger.verbose("getIdentityId: guest access disabled")
            throw NotAuthorizedException(
                recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED,
                cause = notAuthorized
            )
        } catch (e: Exception) {
            throw SignedOutException(
                recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_POSSIBLE,
                cause = e
            )
        }
    }

    private fun getCredentialsResult(awsCredentials: AWSCredentialsInternal): AuthSessionResult<AWSCredentials> =
        with(awsCredentials) {
            AWSCredentials.createAWSCredentials(accessKeyId, secretAccessKey, sessionToken, expiration)
        }?.let {
            AuthSessionResult.success(it)
        } ?: AuthSessionResult.failure(UnknownException("Failed to fetch AWS credentials."))

    /**
     * fetches these identityIdResult and awsCredentialsResult. The results are part an AuthSession returned
     * by `Amplify.Auth.fetchAuthSession` plugin API.
     *
     * Usage:
     * ```
     * // for authenticated user
     * val logins = listOf(LoginProvider(providerName, tokens.idToken))
     *
     * // for guest user
     * val logins = emptyList()
     *
     * val authSessionResult = awsCognitoIdentityPoolOperations.fetchAWSCognitoIdentityPoolDetails(logins, false)
     *
     * val session = OIDCAmplifySession(
     *      true,
     *      authSessionResult.identityIdResult, authSessionResult.awsCredentialsResult,
     *      AuthSessionResult.success(tokens.getUserSub()), AuthSessionResult.success(tokens)
     * )
     * ```
     * @param logins fetch AWS credentials for a authenticated user if LoginProvider info is present,
     * fetch AWS credentials for a unauthenticated (guest) user if empty.
     * @param forceRefresh fetch new AWS credentials if true
     * @return identityId and awsCredentials results
     */
    suspend fun fetchAWSCognitoIdentityPoolDetails(
        logins: List<LoginProvider>,
        forceRefresh: Boolean
    ): AWSCognitoIdentityPoolDetails {
        logger.verbose("fetchAWSCognitoIdentityPoolDetails: get cached AWS credentials")
        val currentLoginProvider = deserializeLogins(awsAuthCredentialStore.get(KEY_LOGINS_PROVIDER))
        val currentIdentityId = awsAuthCredentialStore.get(KEY_IDENTITY_ID)
        val currentAWSCredentials = deserializeCredential(awsAuthCredentialStore.get(KEY_AWS_CREDENTIALS))

        logger.verbose("fetchAWSCognitoIdentityPoolDetails: start fetching identity id")
        val identityIdResult = if (currentIdentityId == null || currentIdentityId.isBlank()) {
            try {
                val identityId = getIdentityId(logins)
                AuthSessionResult.success(identityId)
            } catch (exception: AuthException) {
                AuthSessionResult.failure(exception)
            }
        } else {
            AuthSessionResult.success(currentIdentityId)
        }

        val newLogin = logins != currentLoginProvider
        logger.verbose("fetchAWSCognitoIdentityPoolDetails: start fetching AWS credentials")
        val awsCredentialsResult = if (currentAWSCredentials == null || !isValidSession(currentAWSCredentials) ||
            newLogin || forceRefresh
        ) {
            when (identityIdResult.type) {
                AuthSessionResult.Type.FAILURE -> AuthSessionResult.failure(identityIdResult.error)
                AuthSessionResult.Type.SUCCESS -> {
                    try {
                        val (identityId, awsCredentials) = getAWSCredentials(identityIdResult.value, logins)
                        awsAuthCredentialStore.put(KEY_LOGINS_PROVIDER, Json.encodeToString(logins))
                        awsAuthCredentialStore.put(KEY_IDENTITY_ID, identityId ?: "")
                        awsAuthCredentialStore.put(KEY_AWS_CREDENTIALS, Json.encodeToString(awsCredentials))
                        logger.verbose("fetchAWSCognitoIdentityPoolDetails: cached AWS credentials")
                        getCredentialsResult(awsCredentials)
                    } catch (exception: AuthException) {
                        AuthSessionResult.failure(exception)
                    }
                }
            }
        } else {
            getCredentialsResult(currentAWSCredentials)
        }

        return AWSCognitoIdentityPoolDetails(identityIdResult, awsCredentialsResult)
    }

    /**
     * clear cached AWS credentials.
     */
    fun clearCredentials() {
        logger.verbose("clearCredentials: clear cached AWS credentials")
        awsAuthCredentialStore.removeAll()
    }

    private fun deserializeCredential(encodedString: String?): AWSCredentialsInternal? {
        return try {
            encodedString?.let { Json.decodeFromString(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun deserializeLogins(encodedString: String?): List<LoginProvider> {
        return try {
            encodedString?.let { Json.decodeFromString(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
