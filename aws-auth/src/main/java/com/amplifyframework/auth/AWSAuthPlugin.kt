package com.amplifyframework.auth

import android.content.Context

import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Consumer

import org.json.JSONObject

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AWSAuthPlugin : NotImplementedAuthPlugin<CognitoIdentityProviderClient>() {
    private val client: CognitoIdentityProviderClient =
        CognitoIdentityProviderClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .build()
    private lateinit var poolId: String
    private lateinit var clientId: String
    private lateinit var clientSecret: String

    override fun getPluginKey(): String {
        // TODO: This is a lie. But this is what the CLI generates right now!
        return "awsCognitoAuthPlugin"
    }

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        val userPoolJson = pluginConfiguration!!
                .getJSONObject("CognitoUserPool")
                .getJSONObject("Default")
        clientId = userPoolJson.getString("AppClientId")
        clientSecret = userPoolJson.getString("AppClientSecret")
        poolId = userPoolJson.getString("PoolId")
    }

    override fun getEscapeHatch(): CognitoIdentityProviderClient {
        return client
    }

    override fun getVersion(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun signUp(username: String, password: String, options: AuthSignUpOptions, onSuccess: Consumer<AuthSignUpResult>, onError: Consumer<AuthException>) {
        SignUpOperation(client, clientId, clientSecret, username, password, options, onSuccess, onError).start()
    }

    suspend fun signUp(username: String, password: String, options: AuthSignUpOptions): AuthSignUpResult {
        return suspendCoroutine { continuation ->
            signUp(username, password, options,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
            )
        }
    }

    override fun confirmSignUp(username: String, confirmationCode: String, onSuccess: Consumer<AuthSignUpResult>, onError: Consumer<AuthException>) {
        ConfirmSignUpOperation(client, clientId, clientSecret, username, confirmationCode, onSuccess, onError).start()
    }

    suspend fun confirmSignUp(username: String, confirmationCode: String): AuthSignUpResult {
        return suspendCoroutine { continuation ->
            confirmSignUp(username, confirmationCode,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
            )
        }
    }

    override fun signIn(username: String?, password: String?, options: AuthSignInOptions, onSuccess: Consumer<AuthSignInResult>, onError: Consumer<AuthException>) {
        SignInOperation(client, clientId, clientSecret, poolId, username!!, password!!, options, onSuccess, onError).start()
    }

    suspend fun signIn(username: String, password: String, options: AuthSignInOptions): AuthSignInResult {
        return suspendCoroutine { continuation ->
            signIn(username, password, options,
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }
            )
        }
    }
}
