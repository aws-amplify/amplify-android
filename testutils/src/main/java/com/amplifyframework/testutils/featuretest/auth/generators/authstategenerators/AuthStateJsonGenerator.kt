package com.amplifyframework.testutils.featuretest.auth.generators.authstategenerators

import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignInState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*

/**
 * Generates Json for given serializable class, this might be moved back to cognito auth due to added dependency on auth
 *
 */
class AuthStateJsonGenerator {
    private val encoder = Json { prettyPrint = true }

    fun generateAuthenticationStateSignedIn() {
        val state: AuthState = AuthState.Configured(
            AuthenticationState.SignedIn(
                SignedInData(
                    userId = "userId",
                    username = "username",
                    signedInDate = Date.from(Instant.ofEpochSecond(324234123)),
                    signInMethod = SignInMethod.SRP,
                    cognitoUserPoolTokens = CognitoUserPoolTokens(
                        idToken = "someToken",
                        accessToken = "someAccessToken",
                        refreshToken = "someRefreshToken",
                        expiration = 300
                    )
                )
            ),
            AuthorizationState.SessionEstablished(
                AmplifyCredential.UserAndIdentityPool(
                    CognitoUserPoolTokens(
                        idToken = "someToken",
                        accessToken = "someAccessToken",
                        refreshToken = "someRefreshToken",
                        expiration = 300
                    ),
                    identityId = "someIdentityId",
                    AWSCredentials(
                        accessKeyId = "someAccessKey",
                        secretAccessKey = "someSecretKey",
                        sessionToken = "someSessionToken",
                        expiration = 2342134
                    )
                )
            )
        )

        print("Example :\n ${encoder.encodeToString(state)} \n")
    }

    fun generateSRPSignedIn() {
        val state: AuthState = AuthState.Configured(
            AuthenticationState.SigningIn(
                SignInState.SigningInWithSRP(
                    SRPSignInState.NotStarted()
                )
            ),
            AuthorizationState.SessionEstablished(
                AmplifyCredential.UserAndIdentityPool(
                    CognitoUserPoolTokens(
                        idToken = "someToken",
                        accessToken = "someAccessToken",
                        refreshToken = "someRefreshToken",
                        expiration = 300
                    ),
                    identityId = "someIdentityId",
                    AWSCredentials(
                        accessKeyId = "someAccessKey",
                        secretAccessKey = "someSecretKey",
                        sessionToken = "someSessionToken",
                        expiration = 2342134
                    )
                )
            )
        )

        print("Example :\n ${encoder.encodeToString(state)} \n")
    }

    fun generateAuthStateConfigured() {
        val state: AuthState = AuthState.Configured(AuthenticationState.Configured(), null).printJson()
    }
}

private fun AuthState.printJson(): AuthState {
    val json = Json { prettyPrint = true }
    println("Json :\n ${json.encodeToString(this)} ")
    return this
}
