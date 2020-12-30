package com.amplifyframework.auth

import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep.DONE
import com.amplifyframework.core.Consumer

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest

internal class ConfirmSignUpOperation(
        private val cognito: CognitoIdentityProviderClient,
        private val clientId: String,
        private val clientSecret: String,
        private val username: String,
        private val confirmationCode: String,
        private val onSuccess: Consumer<AuthSignUpResult>,
        private val onError: Consumer<AuthException>) {
    fun start() {
        // This returns an empty response body with a 200 on success.
        val request = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .secretHash(SecretHash.of(username, clientId, clientSecret))
                .username(username)
                .confirmationCode(confirmationCode)
                .clientMetadata(emptyMap())
                .build()
        android.util.Log.w("ConfirmSignUp", request.toString())
        cognito.confirmSignUp(request)

        val nextStep = AuthNextSignUpStep(DONE, emptyMap<String, String>(), null)
        val userId = "TODO. How do I include this without keeping some global state?"
        val user = AuthUser(userId, username)
        val result = AuthSignUpResult(true, nextStep, user)

        onSuccess.accept(result)
    }
}
