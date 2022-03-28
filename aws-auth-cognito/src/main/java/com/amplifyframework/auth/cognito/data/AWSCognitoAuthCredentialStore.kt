package com.amplifyframework.auth.cognito.data

import android.content.Context
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.AuthCredentialStore
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AWSCognitoAuthCredentialStore(
    val context: Context,
    private val authConfiguration: AuthConfiguration,
    isPersistenceEnabled: Boolean = true,
    keyValueRepoFactory: KeyValueRepositoryFactory = KeyValueRepositoryFactory(),
) : AuthCredentialStore {

    companion object {
        const val awsKeyValueStoreIdentifier = "com.amplify.credentialStore"
    }

    private val key = generateKey()
    private var keyValue: KeyValueRepository =
        keyValueRepoFactory.create(context, awsKeyValueStoreIdentifier, isPersistenceEnabled)

    override fun saveCredential(credential: AmplifyCredential) =
        keyValue.put(key, serializeCredential(credential.minimize()))

    override fun savePartialCredential(
        cognitoUserPoolTokens: CognitoUserPoolTokens?,
        identityId: String?,
        awsCredentials: AWSCredentials?
    ) {
        val currentCredentials = retrieveCredential()

        saveCredential(
            AmplifyCredential(
                cognitoUserPoolTokens ?: currentCredentials?.cognitoUserPoolTokens,
                identityId ?: currentCredentials?.identityId,
                awsCredentials ?: currentCredentials?.awsCredentials
            )
        )
    }

    override fun retrieveCredential(): AmplifyCredential? =
        deserializeCredential(keyValue.get(key))?.minimize()

    override fun deleteCredential() = keyValue.remove(key)

    private fun generateKey(): String {
        var prefix = "amplify"
        val sessionKeySuffix = "session"

        authConfiguration.userPool?.let {
            prefix += ".${it.poolId}"
        }
        authConfiguration.identityPool?.let {
            prefix += ".${it.poolId}"
        }

        return prefix.plus(".$sessionKeySuffix")
    }

    private fun deserializeCredential(encodedCredential: String?): AmplifyCredential? =
        encodedCredential?.let { Json.decodeFromString<AmplifyCredential>(it) }

    private fun serializeCredential(credential: AmplifyCredential) = Json.encodeToString(credential)

    private fun AmplifyCredential.minimize(): AmplifyCredential {
        return copy(
            cognitoUserPoolTokens = cognitoUserPoolTokens?.trim(),
            awsCredentials = awsCredentials?.trim()
        )
    }

    private fun CognitoUserPoolTokens.trim(): CognitoUserPoolTokens? {
        return if (idToken != null || accessToken != null || refreshToken != null) this else null
    }

    private fun AWSCredentials.trim(): AWSCredentials? {
        return if (accessKeyId != null || secretAccessKey != null || sessionToken != null) this else null
    }
}
