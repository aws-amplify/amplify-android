package com.amplifyframework.auth.cognito.usecases

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthException.REPORT_BUG_TO_AWS_SUGGESTION
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import java.lang.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase to run after [ResetPasswordUseCase] to respond to any challenges that service may request.
 * [Request API](https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_ConfirmForgotPassword.html)
 */
internal class ConfirmResetPasswordUseCase(
    private val cognitoIdentityProviderClient: CognitoIdentityProviderClient,
    private val appClientId: String
) {
    suspend fun execute(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions,
        onSuccess: Action,
        onException: Consumer<AuthException>
    ) {
        try {
            withContext(Dispatchers.IO) {
                cognitoIdentityProviderClient.confirmForgotPassword {
                    this.username = username
                    this.password = newPassword
                    this.confirmationCode = confirmationCode
                    this.clientMetadata = (options as? AWSCognitoAuthConfirmResetPasswordOptions)?.metadata ?: mapOf()
                    this.clientId = appClientId
                }
            }

            withContext(Dispatchers.Main) { onSuccess.call() }
        } catch (ex: Exception) {
            withContext(Dispatchers.Main) {
                onException.accept(CognitoAuthExceptionConverter.lookup(ex, REPORT_BUG_TO_AWS_SUGGESTION))
            }
        }
    }
}

/**
 * Cognito extension of confirm reset password options to add the platform specific fields.
 */
data class AWSCognitoAuthConfirmResetPasswordOptions(
    val metadata: Map<String, String>
) : AuthConfirmResetPasswordOptions()
