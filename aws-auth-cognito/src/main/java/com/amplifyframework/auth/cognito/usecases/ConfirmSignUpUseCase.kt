package com.amplifyframework.auth.cognito.usecases

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.Consumer

internal class ConfirmSignUpUseCase(environment: AuthEnvironment) {
    private val logger = environment.logger
    private val configuration = environment.configuration
    private val client = environment.cognitoAuthService.cognitoIdentityProviderClient

    suspend fun execute(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        logger?.verbose("ConfirmSignUp Starting execution")
        try {
            client?.confirmSignUp {
                this.username = username
                this.confirmationCode = confirmationCode
                this.clientId = configuration.userPool?.appClient
                this.secretHash = SRPHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
            }

            val authSignUpResult = AuthSignUpResult(
                true,
                AuthNextSignUpStep(AuthSignUpStep.DONE, mapOf(), null),
                null
            )
            onSuccess.accept(authSignUpResult)
            logger?.verbose("ConfirmSignUp Execution complete")
        } catch (exception: Exception) {
            onError.accept(
                CognitoAuthExceptionConverter.lookup(exception, "Confirm sign up failed.")
            )
        }
    }
}
