package com.amplifyframework.auth.cognito.data

import android.content.Context
import java.util.*

class AWSCognitoLegacyCredentialStore(
    val context: Context,
    private val authConfiguration: AuthConfiguration,
) : AuthCredentialStore {

    companion object {
        private const val AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER: String = "com.amazonaws.android.auth"
        private const val ID_KEY: String = "identityId"
        private const val AK_KEY: String = "accessKey"
        private const val SK_KEY: String = "secretKey"
        private const val ST_KEY: String = "sessionToken"
        private const val EXP_KEY: String = "expirationDate"

        private const val APP_LAST_AUTH_USER = "LastAuthUser"
        private const val APP_LOCAL_CACHE = "CognitoIdentityProviderCache"
        private const val APP_LOCAL_CACHE_KEY_PREFIX = "CognitoIdentityProvider"

        private const val TOKEN_TYPE_ID = "idToken"
        private const val TOKEN_TYPE_ACCESS = "accessToken"
        private const val TOKEN_TYPE_REFRESH = "refreshToken"

        // TODO check if below exists
        private const val TOKEN_EXPIRATION = "tokenExpiration"
    }

    private val idAndCredentialsKeyValue: KeyValueRepository =
        LegacyKeyValueRepository(context, AWS_KEY_VALUE_STORE_NAMESPACE_IDENTIFIER)

    private val tokensKeyValue: KeyValueRepository = LegacyKeyValueRepository(context, APP_LOCAL_CACHE)

    override fun saveCredential(credential: AmplifyCredential) {
        val awsCredentials = credential.awsCredentials
        saveAWSCredentials(awsCredentials)
        idAndCredentialsKeyValue.put(namespace(ID_KEY), credential.identityId)
    }

    override fun retrieveCredential(): AmplifyCredential {
        val cognitoUserPoolTokens = retrieveCognitoUserPoolTokens()
        val awsCredentials = retrieveAWSCredentials()
        val identityPool = retrieveIdentityPool()
        return AmplifyCredential(cognitoUserPoolTokens, identityPool, awsCredentials)
    }

    override fun deleteCredential() {
        idAndCredentialsKeyValue.apply {
            remove(namespace(AK_KEY))
            remove(namespace(SK_KEY))
            remove(namespace(ST_KEY))
            remove(namespace(EXP_KEY))
        }
    }

    private fun saveAWSCredentials(awsCredentials: AWSCredentials?) {
        idAndCredentialsKeyValue.apply {
            put(namespace(AK_KEY), awsCredentials?.accessKeyId)
            put(namespace(SK_KEY), awsCredentials?.secretAccessKey)
            put(namespace(ST_KEY), awsCredentials?.sessionToken)
            put(namespace(EXP_KEY), awsCredentials?.expiration)
        }
    }

    private fun retrieveAWSCredentials(): AWSCredentials {
        val accessKey = idAndCredentialsKeyValue.get(namespace(AK_KEY))
        val secretKey = idAndCredentialsKeyValue.get(namespace(SK_KEY))
        val sessionToken = idAndCredentialsKeyValue.get(namespace(ST_KEY))
        val expiration = idAndCredentialsKeyValue.get(namespace(EXP_KEY))

        return AWSCredentials(accessKey, secretKey, sessionToken, expiration)
    }

    private fun retrieveIdentityPool(): String? {
        return idAndCredentialsKeyValue.get(namespace(ID_KEY))
    }

    private fun retrieveCognitoUserPoolTokens(): CognitoUserPoolTokens {
        val appClient = authConfiguration.userPool?.appClient

        val userIdTokenKey = String.format(
            Locale.US, "%s.%s.%s", APP_LOCAL_CACHE_KEY_PREFIX, appClient, APP_LAST_AUTH_USER
        )

        val userId  = tokensKeyValue.get(userIdTokenKey)

        val cachedIdTokenKey = String.format(
            Locale.US, "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX, appClient, userId, TOKEN_TYPE_ID
        )
        val cachedAccessTokenKey = String.format(
            Locale.US, "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX, appClient, userId, TOKEN_TYPE_ACCESS
        )
        val cachedRefreshTokenKey = String.format(
            Locale.US, "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX, appClient, userId, TOKEN_TYPE_REFRESH
        )

        val cachedTokenExpirationKey = String.format(
            Locale.US, "%s.%s.%s.%s",
            APP_LOCAL_CACHE_KEY_PREFIX, appClient, userId, TOKEN_EXPIRATION
        )

        val idToken = tokensKeyValue.get(cachedIdTokenKey)
        val accessToken = tokensKeyValue.get(cachedAccessTokenKey)
        val refreshToken = tokensKeyValue.get(cachedRefreshTokenKey)
        val expiration = tokensKeyValue.get(cachedTokenExpirationKey)

        return CognitoUserPoolTokens(idToken, accessToken, refreshToken, expiration)
    }

    // prefix the key with identity pool id
    private fun namespace(key: String): String = getIdentityPoolId() + "." + key

    private fun getIdentityPoolId(): String? {
        return authConfiguration.identityPool?.poolId
    }
}