package com.amplifyframework.auth.cognito.usecases

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.Consumer

internal class SignUpUseCase(environment: AuthEnvironment) {
    private val logger = environment.logger
    private val configuration = environment.configuration
    private val client = environment.cognitoAuthService.cognitoIdentityProviderClient

    suspend fun execute(
        username: String,
        password: String,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        logger?.verbose("SignUp Starting execution")
        try {
            val userAttributes = options.userAttributes.map {
                AttributeType {
                    name = it.key.keyString
                    value = it.value
                }
            }

            val response = client?.signUp {
                this.username = username
                this.password = password
                this.userAttributes = userAttributes
                this.clientId = configuration.userPool?.appClient
                this.secretHash = SRPHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
            }

            val deliveryDetails = response?.codeDeliveryDetails?.let { details ->
                mapOf(
                    "DESTINATION" to details.destination,
                    "MEDIUM" to details.deliveryMedium?.value,
                    "ATTRIBUTE" to details.attributeName
                )
            }

            val authSignUpResult = AuthSignUpResult(
                false,
                AuthNextSignUpStep(
                    AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                    mapOf(),
                    AuthCodeDeliveryDetails(
                        deliveryDetails?.getValue("DESTINATION") ?: "",
                        AuthCodeDeliveryDetails.DeliveryMedium.fromString(
                            deliveryDetails?.getValue("MEDIUM")
                        ),
                        deliveryDetails?.getValue("ATTRIBUTE")
                    )
                ),
                AuthUser(response?.userSub ?: "", username)
            )
            onSuccess.accept(authSignUpResult)
            logger?.verbose("SignUp Execution complete")
        } catch (exception: Exception) {
            onError.accept(CognitoAuthExceptionConverter.lookup(exception, "Sign up failed."))
        }
    }
}
