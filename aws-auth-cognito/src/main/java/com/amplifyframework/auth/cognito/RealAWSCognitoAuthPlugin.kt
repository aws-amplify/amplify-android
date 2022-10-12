/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito

import android.app.Activity
import android.content.Intent
import aws.sdk.kotlin.services.cognitoidentityprovider.confirmForgotPassword
import aws.sdk.kotlin.services.cognitoidentityprovider.confirmSignUp
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChangePasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceRememberedStatusType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserAttributeVerificationCodeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ListDevicesRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateDeviceStatusRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifyUserAttributeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.resendConfirmationCode
import aws.sdk.kotlin.services.cognitoidentityprovider.signUp
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthCategoryBehavior
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidOauthConfigurationException
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.cognito.exceptions.service.CodeDeliveryFailureException
import com.amplifyframework.auth.cognito.exceptions.service.UserCancelledException
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.HostedUIHelper
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.cognito.helpers.identityProviderName
import com.amplifyframework.auth.cognito.options.AWSAuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmSignInOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResendSignUpCodeOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignOutOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributeOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributesOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.cognito.result.GlobalSignOutError
import com.amplifyframework.auth.cognito.result.HostedUIError
import com.amplifyframework.auth.cognito.result.RevokeTokenError
import com.amplifyframework.auth.cognito.usecases.ResetPasswordUseCase
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.options.AWSCognitoAuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthNextUpdateAttributeStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.auth.result.step.AuthUpdateAttributeStep
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.StateChangeListenerToken
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.FederatedToken
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.HostedUIEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.DeleteUserState
import com.amplifyframework.statemachine.codegen.states.HostedUISignInState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

internal class RealAWSCognitoAuthPlugin(
    private val configuration: AuthConfiguration,
    private val authEnvironment: AuthEnvironment,
    private val authStateMachine: AuthStateMachine,
    private val logger: Logger
) : AuthCategoryBehavior {

    private val lastPublishedHubEventName = AtomicReference<AuthChannelEventName>()

    init {
        addAuthStateChangeListener()
        configureAuthStates()
    }

    fun escapeHatch() = authEnvironment.cognitoAuthService

    override fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.NotConfigured -> onError.accept(
                    InvalidUserPoolConfigurationException()
                )
                is AuthenticationState.SignedIn, is AuthenticationState.SignedOut -> GlobalScope.launch {
                    _signUp(username, password, options, onSuccess, onError)
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private suspend fun _signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        logger.verbose("SignUp Starting execution")
        try {
            val userAttributes = options.userAttributes.map {
                AttributeType {
                    name = it.key.keyString
                    value = it.value
                }
            }

            val encodedContextData = authEnvironment.userContextDataProvider?.getEncodedContextData(username)

            val response = authEnvironment.cognitoAuthService.cognitoIdentityProviderClient?.signUp {
                this.username = username
                this.password = password
                this.userAttributes = userAttributes
                this.clientId = configuration.userPool?.appClient
                this.secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
                encodedContextData?.let { this.userContextData { encodedData = it } }
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
                response?.userSub
            )
            onSuccess.accept(authSignUpResult)
            logger.verbose("SignUp Execution complete")
        } catch (exception: Exception) {
            onError.accept(CognitoAuthExceptionConverter.lookup(exception, "Sign up failed."))
        }
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        confirmSignUp(username, confirmationCode, AuthConfirmSignUpOptions.defaults(), onSuccess, onError)
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.NotConfigured -> onError.accept(
                    InvalidUserPoolConfigurationException()
                )
                is AuthenticationState.SignedIn, is AuthenticationState.SignedOut -> GlobalScope.launch {
                    _confirmSignUp(username, confirmationCode, options, onSuccess, onError)
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private suspend fun _confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        logger.verbose("ConfirmSignUp Starting execution")
        try {
            val encodedContextData = authEnvironment.userContextDataProvider?.getEncodedContextData(username)

            authEnvironment.cognitoAuthService.cognitoIdentityProviderClient?.confirmSignUp {
                this.username = username
                this.confirmationCode = confirmationCode
                this.clientId = configuration.userPool?.appClient
                this.secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
                encodedContextData?.let { this.userContextData { encodedData = it } }
            }

            val authSignUpResult = AuthSignUpResult(
                true,
                AuthNextSignUpStep(AuthSignUpStep.DONE, mapOf(), null),
                null
            )
            onSuccess.accept(authSignUpResult)
            logger.verbose("ConfirmSignUp Execution complete")
        } catch (exception: Exception) {
            onError.accept(
                CognitoAuthExceptionConverter.lookup(exception, "Confirm sign up failed.")
            )
        }
    }

    override fun resendSignUpCode(
        username: String,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        resendSignUpCode(username, AuthResendSignUpCodeOptions.defaults(), onSuccess, onError)
    }

    override fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.NotConfigured -> onError.accept(
                    InvalidUserPoolConfigurationException()
                )
                is AuthenticationState.SignedIn, is AuthenticationState.SignedOut -> GlobalScope.launch {
                    _resendSignUpCode(username, options, onSuccess, onError)
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private suspend fun _resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        logger.verbose("ResendSignUpCode Starting execution")
        try {
            val metadata = (options as? AWSCognitoAuthResendSignUpCodeOptions)?.metadata
            val encodedContextData = authEnvironment.userContextDataProvider?.getEncodedContextData(username)

            val response = authEnvironment.cognitoAuthService.cognitoIdentityProviderClient?.resendConfirmationCode {
                clientId = configuration.userPool?.appClient
                this.username = username
                secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
                clientMetadata = metadata
                encodedContextData?.let { this.userContextData { encodedData = it } }
            }

            val deliveryDetails = response?.codeDeliveryDetails?.let { details ->
                mapOf(
                    "DESTINATION" to details.destination,
                    "MEDIUM" to details.deliveryMedium?.value,
                    "ATTRIBUTE" to details.attributeName
                )
            }

            val codeDeliveryDetails = AuthCodeDeliveryDetails(
                deliveryDetails?.getValue("DESTINATION") ?: "",
                AuthCodeDeliveryDetails.DeliveryMedium.fromString(
                    deliveryDetails?.getValue("MEDIUM")
                ),
                deliveryDetails?.getValue("ATTRIBUTE")
            )
            onSuccess.accept(codeDeliveryDetails)
            logger.verbose("ResendSignUpCode Execution complete")
        } catch (exception: Exception) {
            onError.accept(CognitoAuthExceptionConverter.lookup(exception, "Resend sign up code failed."))
        }
    }

    override fun signIn(
        username: String?,
        password: String?,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        signIn(username, password, AuthSignInOptions.defaults(), onSuccess, onError)
    }

    override fun signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.NotConfigured -> onError.accept(
                    InvalidUserPoolConfigurationException()
                )
                // Continue sign in
                is AuthenticationState.SignedOut, is AuthenticationState.Configured -> {
                    val signInOptions = options as? AWSCognitoAuthSignInOptions ?: AWSCognitoAuthSignInOptions.builder()
                        .authFlowType(configuration.authFlowType)
                        .build()

                    _signIn(username, password, signInOptions, onSuccess, onError)
                }
                is AuthenticationState.SignedIn -> onError.accept(SignedInException())
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private fun _signIn(
        username: String?,
        password: String?,
        options: AWSCognitoAuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.SigningIn -> {
                        val signInState = authNState.signInState
                        val srpSignInState = (signInState as? SignInState.SigningInWithSRP)?.srpSignInState
                        val challengeState = (signInState as? SignInState.ResolvingChallenge)?.challengeState
                        when {
                            srpSignInState is SRPSignInState.Error -> {
                                token?.let(authStateMachine::cancel)
                                onError.accept(
                                    CognitoAuthExceptionConverter.lookup(srpSignInState.exception, "Sign in failed.")
                                )
                            }
                            signInState is SignInState.Error -> {
                                token?.let(authStateMachine::cancel)
                                onError.accept(
                                    CognitoAuthExceptionConverter.lookup(signInState.exception, "Sign in failed.")
                                )
                            }
                            challengeState is SignInChallengeState.WaitingForAnswer -> {
                                token?.let(authStateMachine::cancel)
                                SignInChallengeHelper.getNextStep(challengeState.challenge, onSuccess, onError)
                            }
                        }
                    }
                    authNState is AuthenticationState.SignedIn
                        && authZState is AuthorizationState.SessionEstablished -> {
                        token?.let(authStateMachine::cancel)
                        val authSignInResult = AuthSignInResult(
                            true,
                            AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null)
                        )
                        onSuccess.accept(authSignInResult)
                    }
                    else -> Unit
                }
            },
            {
                val signInData = when (options.authFlowType) {
                    AuthFlowType.USER_SRP_AUTH -> {
                        SignInData.SRPSignInData(username, password, options.metadata)
                    }
                    AuthFlowType.CUSTOM_AUTH, AuthFlowType.CUSTOM_AUTH_WITHOUT_SRP -> {
                        SignInData.CustomAuthSignInData(username, options.metadata)
                    }
                    AuthFlowType.CUSTOM_AUTH_WITH_SRP -> {
                        SignInData.CustomSRPAuthSignInData(username, options.metadata)
                    }
                    AuthFlowType.USER_PASSWORD_AUTH -> {
                        SignInData.MigrationAuthSignInData(username, password, options.metadata)
                    }
                }
                val event = AuthenticationEvent(AuthenticationEvent.EventType.SignInRequested(signInData))
                authStateMachine.send(event)
            }
        )
    }

    override fun confirmSignIn(
        challengeResponse: String,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        confirmSignIn(challengeResponse, AuthConfirmSignInOptions.defaults(), onSuccess, onError)
    }

    override fun confirmSignIn(
        challengeResponse: String,
        options: AuthConfirmSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            val authNState = authState.authNState
            val signInState = (authNState as? AuthenticationState.SigningIn)?.signInState
            when ((signInState as? SignInState.ResolvingChallenge)?.challengeState) {
                is SignInChallengeState.WaitingForAnswer -> {
                    _confirmSignIn(challengeResponse, options, onSuccess, onError)
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private fun _confirmSignIn(
        challengeResponse: String,
        options: AuthConfirmSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                val authNState = authState.authNState
                val signInState = (authNState as? AuthenticationState.SigningIn)?.signInState
                val challengeState = (signInState as? SignInState.ResolvingChallenge)?.challengeState
                when {
                    authNState is AuthenticationState.SignedIn -> {
                        token?.let(authStateMachine::cancel)
                        val authSignInResult = AuthSignInResult(
                            true,
                            AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null)
                        )
                        onSuccess.accept(authSignInResult)
                    }
                    signInState is SignInState.Error -> {
                        token?.let(authStateMachine::cancel)
                        onError.accept(
                            CognitoAuthExceptionConverter.lookup(signInState.exception, "Confirm Sign in failed.")
                        )
                    }
                }
            },
            {
                val awsCognitoConfirmSignInOptions = options as? AWSCognitoAuthConfirmSignInOptions
                val event = SignInChallengeEvent(
                    SignInChallengeEvent.EventType.VerifyChallengeAnswer(
                        challengeResponse,
                        awsCognitoConfirmSignInOptions?.metadata ?: mapOf()
                    )
                )
                authStateMachine.send(event)
            }
        )
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        signInWithSocialWebUI(
            provider,
            callingActivity,
            AWSCognitoAuthWebUISignInOptions.builder().build(),
            onSuccess,
            onError
        )
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        signInWithHostedUI(
            provider = provider,
            callingActivity = callingActivity,
            options = options,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        signInWithWebUI(callingActivity, AuthWebUISignInOptions.builder().build(), onSuccess, onError)
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        signInWithHostedUI(
            callingActivity = callingActivity,
            options = options,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    private fun signInWithHostedUI(
        provider: AuthProvider? = null,
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.NotConfigured -> onError.accept(
                    InvalidUserPoolConfigurationException()
                )
                // Continue sign in
                is AuthenticationState.SignedOut -> {
                    if (configuration.oauth == null) {
                        onError.accept(InvalidOauthConfigurationException())
                        return@getCurrentState
                    }

                    _signInWithHostedUI(
                        callingActivity = callingActivity,
                        options = options,
                        onSuccess = onSuccess,
                        onError = onError,
                        provider = provider
                    )
                }
                is AuthenticationState.SignedIn -> onError.accept(SignedInException())
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private fun _signInWithHostedUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>,
        provider: AuthProvider? = null
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.SigningIn -> {
                        val hostedUISignInState = authNState.signInState.hostedUISignInState
                        if (hostedUISignInState is HostedUISignInState.Error) {
                            token?.let(authStateMachine::cancel)
                            val exception = hostedUISignInState.exception
                            onError.accept(
                                if (exception is AuthException) {
                                    exception
                                } else {
                                    UnknownException("Sign in failed", exception)
                                }
                            )
                            authStateMachine.send(AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn()))
                        }
                    }
                    authNState is AuthenticationState.SignedIn
                        && authZState is AuthorizationState.SessionEstablished -> {
                        token?.let(authStateMachine::cancel)
                        val authSignInResult =
                            AuthSignInResult(
                                true,
                                AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null)
                            )
                        onSuccess.accept(authSignInResult)
                    }
                    else -> Unit
                }
            },
            {
                val hostedUIOptions = HostedUIHelper.createHostedUIOptions(callingActivity, provider, options)
                authStateMachine.send(
                    AuthenticationEvent(
                        AuthenticationEvent.EventType.SignInRequested(
                            SignInData.HostedUISignInData(hostedUIOptions)
                        )
                    )
                )
            }
        )
    }

    override fun handleWebUISignInResponse(intent: Intent?) {
        authStateMachine.getCurrentState {
            val callbackUri = intent?.data
            when (val authNState = it.authNState) {
                is AuthenticationState.SigningOut -> {
                    (authNState.signOutState as? SignOutState.SigningOutHostedUI)?.let { signOutState ->
                        if (callbackUri == null) {
                            // Notify failed web sign out
                            authStateMachine.send(
                                SignOutEvent(SignOutEvent.EventType.UserCancelled(signOutState.signedInData))
                            )
                        }
                        if (signOutState.globalSignOut) {
                            authStateMachine.send(
                                SignOutEvent(SignOutEvent.EventType.SignOutGlobally(signOutState.signedInData))
                            )
                        } else {
                            authStateMachine.send(
                                SignOutEvent(SignOutEvent.EventType.RevokeToken(signOutState.signedInData))
                            )
                        }
                    }
                }
                is AuthenticationState.SigningIn -> {
                    if (callbackUri == null) {
                        authStateMachine.send(
                            HostedUIEvent(
                                HostedUIEvent.EventType.ThrowError(
                                    UserCancelledException(
                                        "The user cancelled the sign-in attempt, so it did not complete.",
                                        "To recover: catch this error, and show the sign-in screen again."
                                    )
                                )
                            )
                        )
                    } else {
                        authStateMachine.send(HostedUIEvent(HostedUIEvent.EventType.FetchToken(callbackUri)))
                    }
                }
                else -> Unit
            }
        }
    }

    private suspend fun getSession(): AWSCognitoAuthSession {
        return suspendCoroutine { continuation ->
            fetchAuthSession(
                { continuation.resume(it as AWSCognitoAuthSession) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override fun fetchAuthSession(onSuccess: Consumer<AuthSession>, onError: Consumer<AuthException>) {
        fetchAuthSession(AuthFetchSessionOptions.defaults(), onSuccess, onError)
    }

    override fun fetchAuthSession(
        options: AuthFetchSessionOptions,
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        val forceRefresh = options.forceRefresh
        authStateMachine.getCurrentState { authState ->
            when (val authZState = authState.authZState) {
                is AuthorizationState.Configured, is AuthorizationState.Error -> {
                    authStateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchUnAuthSession))
                    _fetchAuthSession(onSuccess, onError)
                }
                is AuthorizationState.SessionEstablished -> {
                    val credential = authZState.amplifyCredential
                    if (!credential.isValid() || forceRefresh) {
                        if (lastPublishedHubEventName.get() != AuthChannelEventName.SESSION_EXPIRED) {
                            lastPublishedHubEventName.set(AuthChannelEventName.SESSION_EXPIRED)
                            Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SESSION_EXPIRED))
                        }
                        authStateMachine.send(
                            AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(credential))
                        )
                        _fetchAuthSession(onSuccess, onError)
                    } else onSuccess.accept(credential.getCognitoSession())
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private fun _fetchAuthSession(
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                when (val authZState = authState.authZState) {
                    is AuthorizationState.SessionEstablished -> {
                        token?.let(authStateMachine::cancel)
                        onSuccess.accept(authZState.amplifyCredential.getCognitoSession())
                    }
                    is AuthorizationState.Error -> {
                        token?.let(authStateMachine::cancel)
                        onError.accept(
                            CognitoAuthExceptionConverter.lookup(
                                authZState.exception,
                                "Fetch auth session failed."
                            )
                        )
                    }
                    else -> Unit
                }
            },
            null
        )
    }

    override fun rememberDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        authStateMachine.getCurrentState { authState ->
            when (val state = authState.authNState) {
                is AuthenticationState.SignedIn -> {
                    updateDevice(
                        (state.deviceMetadata as? DeviceMetadata.Metadata)?.deviceKey,
                        DeviceRememberedStatusType.Remembered,
                        onSuccess,
                        onError
                    )
                }
                else -> {
                    onError.accept(SignedOutException())
                }
            }
        }
    }

    override fun forgetDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        forgetDevice(AuthDevice.fromId(""), onSuccess, onError)
    }

    private fun updateDevice(
        alternateDeviceId: String?,
        rememberedStatusType: DeviceRememberedStatusType,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        GlobalScope.async {
            try {
                val tokens = getSession().userPoolTokensResult
                authEnvironment.cognitoAuthService.cognitoIdentityProviderClient?.updateDeviceStatus(
                    UpdateDeviceStatusRequest.invoke {
                        accessToken = tokens.value?.accessToken
                        deviceKey = alternateDeviceId
                        deviceRememberedStatus = rememberedStatusType
                    }
                )
                onSuccess.call()
            } catch (e: Exception) {
                onError.accept(CognitoAuthExceptionConverter.lookup(e, "Update device ID failed."))
            }
        }
    }

    override fun forgetDevice(
        device: AuthDevice,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (val authState = authState.authNState) {
                is AuthenticationState.SignedIn -> {
                    if (device.id.isEmpty()) {
                        val deviceKey = (authState.deviceMetadata as? DeviceMetadata.Metadata)?.deviceKey
                        updateDevice(deviceKey, DeviceRememberedStatusType.NotRemembered, onSuccess, onError)
                    } else {
                        updateDevice(device.id, DeviceRememberedStatusType.NotRemembered, onSuccess, onError)
                    }
                }
                else -> {
                    onError.accept(SignedOutException())
                }
            }
        }
    }

    override fun fetchDevices(
        onSuccess: Consumer<MutableList<AuthDevice>>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.SignedIn -> {
                    _fetchDevices(onSuccess, onError)
                }
                else -> {
                    onError.accept(SignedOutException())
                }
            }
        }
    }

    private fun _fetchDevices(onSuccess: Consumer<MutableList<AuthDevice>>, onError: Consumer<AuthException>) {
        GlobalScope.async {
            try {
                val tokens = getSession().userPoolTokensResult
                val response =
                    authEnvironment.cognitoAuthService.cognitoIdentityProviderClient?.listDevices(
                        ListDevicesRequest.invoke {
                            accessToken = tokens.value?.accessToken
                        }
                    )
                val _devices = response?.devices
                val authdeviceList = mutableListOf<AuthDevice>()
                _devices?.forEach {
                    authdeviceList.add(AuthDevice.fromId(it.deviceKey ?: ""))
                }
                onSuccess.accept(authdeviceList)
            } catch (e: Exception) {
                onError.accept(CognitoAuthExceptionConverter.lookup(e, "Fetch devices failed."))
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun resetPassword(
        username: String,
        options: AuthResetPasswordOptions,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        try {
            val cognitoIdentityProviderClient = requireNotNull(
                authEnvironment.cognitoAuthService.cognitoIdentityProviderClient
            )

            val appClient = requireNotNull(configuration.userPool?.appClient)
            val encodedData = authEnvironment.userContextDataProvider?.getEncodedContextData(username)

            GlobalScope.launch {
                ResetPasswordUseCase(cognitoIdentityProviderClient, appClient).execute(
                    username,
                    options,
                    encodedData,
                    onSuccess,
                    onError
                )
            }
        } catch (ex: Exception) {
            onError.accept(InvalidUserPoolConfigurationException())
        }
    }

    override fun resetPassword(
        username: String,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        resetPassword(username, AuthResetPasswordOptions.defaults(), onSuccess, onError)
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            if (authState.authNState is AuthenticationState.NotConfigured) {
                onError.accept(
                    ConfigurationException(
                        "Confirm Reset Password failed.",
                        "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
                    )
                )
                return@getCurrentState
            }

            GlobalScope.launch {
                try {
                    val encodedContextData = authEnvironment.userContextDataProvider?.getEncodedContextData(username)

                    authEnvironment.cognitoAuthService.cognitoIdentityProviderClient!!.confirmForgotPassword {
                        this.username = username
                        this.confirmationCode = confirmationCode
                        password = newPassword
                        clientMetadata =
                            (options as? AWSCognitoAuthConfirmResetPasswordOptions)?.metadata ?: mapOf()
                        clientId = configuration.userPool?.appClient
                        encodedContextData?.let { this.userContextData { encodedData = it } }
                    }.let { onSuccess.call() }
                } catch (ex: Exception) {
                    onError.accept(
                        CognitoAuthExceptionConverter.lookup(ex, AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION)
                    )
                }
            }
        }
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        confirmResetPassword(
            username,
            newPassword,
            confirmationCode,
            AuthConfirmResetPasswordOptions.defaults(),
            onSuccess,
            onError
        )
    }

    override fun updatePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                // Check if user signed in
                is AuthenticationState.SignedIn -> {
                    _updatePassword(oldPassword, newPassword, onSuccess, onError)
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private fun _updatePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        GlobalScope.async {
            val tokens = getSession().userPoolTokensResult
            val changePasswordRequest = ChangePasswordRequest.invoke {
                previousPassword = oldPassword
                proposedPassword = newPassword
                this.accessToken = tokens.value?.accessToken
            }
            try {
                authEnvironment.cognitoAuthService
                    .cognitoIdentityProviderClient?.changePassword(
                        changePasswordRequest
                    )
                onSuccess.call()
            } catch (e: Exception) {
                onError.accept(CognitoAuthExceptionConverter.lookup(e, e.toString()))
            }
        }
    }

    override fun fetchUserAttributes(
        onSuccess: Consumer<MutableList<AuthUserAttribute>>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                // Check if user signed in
                is AuthenticationState.SignedIn -> {

                    GlobalScope.launch {
                        try {
                            val accessToken = getSession().userPoolTokensResult.value?.accessToken
                            val getUserRequest = GetUserRequest.invoke {
                                this.accessToken = accessToken
                            }
                            val user = authEnvironment.cognitoAuthService.cognitoIdentityProviderClient?.getUser(
                                getUserRequest
                            )
                            val userAttributes = buildList {
                                user?.userAttributes?.mapTo(this) {
                                    AuthUserAttribute(
                                        AuthUserAttributeKey.custom(it.name),
                                        it.value
                                    )
                                }
                            }
                            onSuccess.accept(userAttributes.toMutableList())
                        } catch (e: Exception) {
                            onError.accept(CognitoAuthExceptionConverter.lookup(e, e.toString()))
                        }
                    }
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        GlobalScope.launch {
            try {
                val attributes = listOf(attribute)
                val userAttributeOptions = options as? AWSCognitoAuthUpdateUserAttributeOptions
                val results = updateUserAttributes(attributes.toMutableList(), userAttributeOptions?.metadata)
                onSuccess.accept(results.entries.first().value)
            } catch (e: AuthException) {
                onError.accept(e)
            } catch (e: Exception) {
                onError.accept(CognitoAuthExceptionConverter.lookup(e, e.toString()))
            }
        }
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        updateUserAttribute(attribute, AuthUpdateUserAttributeOptions.defaults(), onSuccess, onError)
    }

    override fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions,
        onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        GlobalScope.launch {
            try {
                val userAttributesOptions = options as? AWSCognitoAuthUpdateUserAttributesOptions
                onSuccess.accept(updateUserAttributes(attributes, userAttributesOptions?.metadata))
            } catch (e: AuthException) {
                onError.accept(e)
            } catch (e: Exception) {
                onError.accept(CognitoAuthExceptionConverter.lookup(e, e.toString()))
            }
        }
    }

    override fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        updateUserAttributes(attributes, AuthUpdateUserAttributesOptions.defaults(), onSuccess, onError)
    }

    private suspend fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        userAttributesOptionsMetadata: Map<String, String>?,
    ): MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult> {

        return suspendCoroutine { continuation ->

            authStateMachine.getCurrentState { authState ->
                when (authState.authNState) {
                    // Check if user signed in
                    is AuthenticationState.SignedIn -> {
                        GlobalScope.launch {
                            try {
                                val accessToken = getSession().userPoolTokensResult.value?.accessToken
                                accessToken?.let {
                                    var userAttributes = attributes.map {
                                        AttributeType.invoke {
                                            name = it.key.keyString
                                            value = it.value
                                        }
                                    }
                                    val userAttributesRequest = UpdateUserAttributesRequest.invoke {
                                        this.accessToken = accessToken
                                        this.userAttributes = userAttributes
                                        this.clientMetadata = userAttributesOptionsMetadata
                                    }
                                    val userAttributeResponse = authEnvironment.cognitoAuthService
                                        .cognitoIdentityProviderClient?.updateUserAttributes(
                                            userAttributesRequest
                                        )

                                    continuation.resume(
                                        getUpdateUserAttributeResult(userAttributeResponse, userAttributes)
                                    )
                                } ?: continuation.resumeWithException(
                                    InvalidUserPoolConfigurationException()
                                )
                            } catch (e: Exception) {
                                continuation.resumeWithException(CognitoAuthExceptionConverter.lookup(e, e.toString()))
                            }
                        }
                    }
                    else -> continuation.resumeWithException(InvalidStateException())
                }
            }
        }
    }

    private fun getUpdateUserAttributeResult(
        response: UpdateUserAttributesResponse?,
        userAttributeList: List<AttributeType>
    ): MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult> {

        val finalResult = HashMap<AuthUserAttributeKey, AuthUpdateAttributeResult>()

        response?.codeDeliveryDetailsList?.let {
            val codeDeliveryDetailsList = it
            for (item in codeDeliveryDetailsList) {
                item.attributeName?.let {

                    val deliveryMedium = AuthCodeDeliveryDetails.DeliveryMedium.fromString(item.deliveryMedium?.value)
                    val authCodeDeliveryDetails = AuthCodeDeliveryDetails(
                        item.destination.toString(),
                        deliveryMedium,
                        item.attributeName
                    )
                    val nextStep = AuthNextUpdateAttributeStep(
                        AuthUpdateAttributeStep.CONFIRM_ATTRIBUTE_WITH_CODE,
                        HashMap(),
                        authCodeDeliveryDetails
                    )
                    val updateAttributeResult = AuthUpdateAttributeResult(false, nextStep)
                    finalResult[AuthUserAttributeKey.custom(item.attributeName)] = updateAttributeResult
                }
            }
        }

        // Check if all items are added to the dictionary
        for (item in userAttributeList) {
            if (!finalResult.containsKey(AuthUserAttributeKey.custom(item.name))) {
                val completeStep = AuthNextUpdateAttributeStep(
                    AuthUpdateAttributeStep.DONE,
                    HashMap(),
                    null
                )
                val updateAttributeResult = AuthUpdateAttributeResult(true, completeStep)
                finalResult[AuthUserAttributeKey.custom(item.name)] = updateAttributeResult
            }
        }
        return finalResult
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        val metadataOptions = options as? AWSAuthResendUserAttributeConfirmationCodeOptions
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                // Check if user signed in
                is AuthenticationState.SignedIn -> {
                    GlobalScope.launch {
                        try {
                            val accessToken = getSession().userPoolTokensResult.value?.accessToken
                            accessToken?.let {
                                val getUserAttributeVerificationCodeRequest =
                                    GetUserAttributeVerificationCodeRequest.invoke {
                                        this.accessToken = accessToken
                                        this.attributeName = attributeKey.keyString
                                        this.clientMetadata = metadataOptions?.metadata
                                    }

                                val getUserAttributeVerificationCodeResponse = authEnvironment.cognitoAuthService
                                    .cognitoIdentityProviderClient?.getUserAttributeVerificationCode(
                                        getUserAttributeVerificationCodeRequest
                                    )

                                getUserAttributeVerificationCodeResponse?.codeDeliveryDetails?.let {
                                    val codeDeliveryDetails = it
                                    codeDeliveryDetails.attributeName?.let {

                                        val deliveryMedium = AuthCodeDeliveryDetails.DeliveryMedium.fromString(
                                            codeDeliveryDetails.deliveryMedium?.value
                                        )
                                        val authCodeDeliveryDetails = AuthCodeDeliveryDetails(
                                            codeDeliveryDetails.destination.toString(),
                                            deliveryMedium,
                                            codeDeliveryDetails.attributeName
                                        )
                                        onSuccess.accept(authCodeDeliveryDetails)
                                    } ?: {
                                        onError.accept(CodeDeliveryFailureException())
                                    }
                                }
                            } ?: onError.accept(
                                InvalidUserPoolConfigurationException()
                            )
                        } catch (e: Exception) {
                            onError.accept(CognitoAuthExceptionConverter.lookup(e, e.toString()))
                        }
                    }
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        resendUserAttributeConfirmationCode(
            attributeKey,
            AuthResendUserAttributeConfirmationCodeOptions.defaults(),
            onSuccess,
            onError
        )
    }

    override fun confirmUserAttribute(
        attributeKey: AuthUserAttributeKey,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                // Check if user signed in
                is AuthenticationState.SignedIn -> {
                    GlobalScope.launch {
                        try {
                            val accessToken = getSession().userPoolTokensResult.value?.accessToken
                            accessToken?.let {
                                val verifyUserAttributeRequest = VerifyUserAttributeRequest.invoke {
                                    this.accessToken = accessToken
                                    this.attributeName = attributeKey.keyString
                                    this.code = confirmationCode
                                }
                                authEnvironment.cognitoAuthService
                                    .cognitoIdentityProviderClient?.verifyUserAttribute(
                                        verifyUserAttributeRequest
                                    )
                                onSuccess.call()
                            } ?: onError.accept(InvalidUserPoolConfigurationException())
                        } catch (e: Exception) {
                            onError.accept(CognitoAuthExceptionConverter.lookup(e, e.toString()))
                        }
                    }
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    override fun getCurrentUser(
        onSuccess: Consumer<AuthUser>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            if (authState.authNState !is AuthenticationState.SignedIn) {
                onError.accept(SignedOutException())
                return@getCurrentState
            }

            GlobalScope.async {
                val accessToken = getSession().userPoolTokensResult.value?.accessToken
                accessToken?.run {
                    val userid = SessionHelper.getUserSub(accessToken) ?: ""
                    val username = SessionHelper.getUsername(accessToken) ?: ""
                    onSuccess.accept(AuthUser(userid, username))
                } ?: onError.accept(InvalidUserPoolConfigurationException())
            }
        }
    }

    override fun signOut(onComplete: Consumer<AuthSignOutResult>) {
        signOut(AuthSignOutOptions.builder().build(), onComplete)
    }

    override fun signOut(options: AuthSignOutOptions, onComplete: Consumer<AuthSignOutResult>) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.NotConfigured ->
                    onComplete.accept(AWSCognitoAuthSignOutResult.CompleteSignOut)
                // Continue sign out and clear auth or guest credentials
                is AuthenticationState.SignedIn, is AuthenticationState.SignedOut -> {
                    // Send SignOut event here instead of OnSubscribedCallback handler to ensure we do not fire
                    // onComplete immediately, which would happen if calling signOut while signed out
                    val event = AuthenticationEvent(
                        AuthenticationEvent.EventType.SignOutRequested(
                            SignOutData(
                                options.isGlobalSignOut,
                                (options as? AWSCognitoAuthSignOutOptions)?.browserPackage
                            )
                        )
                    )
                    authStateMachine.send(event)
                    _signOut(onComplete)
                }
                is AuthenticationState.FederatedToIdentityPool -> {
                    onComplete.accept(
                        AWSCognitoAuthSignOutResult.FailedSignOut(
                            InvalidStateException(
                                "The user is currently federated to identity pool. " +
                                    "You must call clearFederationToIdentityPool to clear credentials."
                            )
                        )
                    )
                }
                else -> onComplete.accept(
                    AWSCognitoAuthSignOutResult.FailedSignOut(InvalidStateException())
                )
            }
        }
    }

    private fun _signOut(onComplete: Consumer<AuthSignOutResult>) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                if (authState is AuthState.Configured) {
                    val (authNState, authZState) = authState
                    when {
                        authNState is AuthenticationState.SignedOut && authZState is AuthorizationState.Configured -> {
                            token?.let(authStateMachine::cancel)
                            if (authNState.signedOutData.hasError) {
                                val signedOutData = authNState.signedOutData
                                onComplete.accept(
                                    AWSCognitoAuthSignOutResult.PartialSignOut(
                                        hostedUIError = signedOutData.hostedUIErrorData?.let { HostedUIError(it) },
                                        globalSignOutError = signedOutData.globalSignOutErrorData?.let {
                                            GlobalSignOutError(it)
                                        },
                                        revokeTokenError = signedOutData.revokeTokenErrorData?.let {
                                            RevokeTokenError(
                                                it
                                            )
                                        }
                                    )
                                )
                            } else {
                                onComplete.accept(AWSCognitoAuthSignOutResult.CompleteSignOut)
                            }
                        }
                        authNState is AuthenticationState.Error -> {
                            token?.let(authStateMachine::cancel)
                            onComplete.accept(
                                AWSCognitoAuthSignOutResult.FailedSignOut(
                                    CognitoAuthExceptionConverter.lookup(authNState.exception, "Sign out failed.")
                                )
                            )
                        }
                        else -> {
                            // No - op
                        }
                    }
                }
            },
            {
            }
        )
    }

    override fun deleteUser(onSuccess: Action, onError: Consumer<AuthException>) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.SignedIn -> {
                    GlobalScope.launch {
                        val accessToken = getSession().userPoolTokensResult.value?.accessToken
                        accessToken?.let {
                            _deleteUser(accessToken, onSuccess, onError)
                        } ?: onError.accept(SignedOutException())
                    }
                }
                is AuthenticationState.SignedOut -> onError.accept(SignedOutException())
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private fun _deleteUser(token: String, onSuccess: Action, onError: Consumer<AuthException>) {
        var listenerToken: StateChangeListenerToken? = null
        listenerToken = authStateMachine.listen(
            { authState ->
                val signOutState = (authState.authNState as? AuthenticationState.SigningOut)?.signOutState
                when (signOutState) {
                    is SignOutState.SignedOut -> {
                        val event = DeleteUserEvent(DeleteUserEvent.EventType.SignOutDeletedUser())
                        authStateMachine.send(event)
                    }
                    is SignOutState.Error -> {
                        val event = DeleteUserEvent(DeleteUserEvent.EventType.ThrowError(signOutState.exception))
                        authStateMachine.send(event)
                    }
                    else -> {
                        // No-op
                    }
                }
                val authZState = authState.authZState as? AuthorizationState.DeletingUser
                when (val deleteUserState = authZState?.deleteUserState) {
                    is DeleteUserState.UserDeleted -> {
                        onSuccess.call()
                        Amplify.Hub.publish(
                            HubChannel.AUTH,
                            HubEvent.create(AuthChannelEventName.USER_DELETED)
                        )
                        listenerToken?.let(authStateMachine::cancel)
                    }
                    is DeleteUserState.Error -> {
                        listenerToken?.let(authStateMachine::cancel)
                        onError.accept(
                            CognitoAuthExceptionConverter.lookup(
                                deleteUserState.exception,
                                "Request to delete user may have failed. Please check exception stack"
                            )
                        )
                    }
                    else -> {
                        // No-op
                    }
                }
            },
            {
                val event = DeleteUserEvent(DeleteUserEvent.EventType.DeleteUser(accessToken = token))
                authStateMachine.send(event)
            }
        )
    }

    private fun addAuthStateChangeListener() {
        authStateMachine.listen(
            { authState ->
                logger.verbose("Auth State Change: $authState")

                when (authState) {
                    is AuthState.Configured -> {
                        val (authNState, authZState) = authState
                        val deleteUserAuthZState = authZState as? AuthorizationState.DeletingUser
                        when {
                            authNState is AuthenticationState.SignedOut &&
                                authZState is AuthorizationState.Configured
                                && lastPublishedHubEventName.get() != AuthChannelEventName.SIGNED_OUT -> {
                                lastPublishedHubEventName.set(AuthChannelEventName.SIGNED_OUT)
                                Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SIGNED_OUT))
                            }
                            authNState is AuthenticationState.SignedIn &&
                                authZState is AuthorizationState.SessionEstablished
                                && lastPublishedHubEventName.get() != AuthChannelEventName.SIGNED_IN -> {
                                lastPublishedHubEventName.set(AuthChannelEventName.SIGNED_IN)
                                Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SIGNED_IN))
                            }
                            deleteUserAuthZState?.deleteUserState is DeleteUserState.UserDeleted
                                && lastPublishedHubEventName.get() != AuthChannelEventName.USER_DELETED -> {
                                Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.USER_DELETED))
                            }
                        }
                    }
                    else -> Unit
                }
            },
            null
        )
    }

    private fun configureAuthStates() {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                when (authState) {
                    is AuthState.Configured -> {
                        token?.let(authStateMachine::cancel)
                    }
                    else -> {} // handle errors
                }
            },
            {
                authStateMachine.send(AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration)))
            }
        )
    }

    fun federateToIdentityPool(
        providerToken: String,
        authProvider: AuthProvider,
        options: FederateToIdentityPoolOptions?,
        onSuccess: Consumer<FederateToIdentityPoolResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            val authNState = authState.authNState
            val authZState = authState.authZState
            when {
                authState !is AuthState.Configured -> onError.accept(
                    InvalidStateException("Federation could not be completed.")
                )
                authNState is AuthenticationState.FederatedToIdentityPool -> {
                    onError.accept(
                        InvalidStateException(
                            "The user is currently federated to identity pool. You " +
                                "must call clearFederationToIdentityPool to clear credentials."
                        )
                    )
                }
                (
                    authNState is AuthenticationState.SignedOut ||
                        authNState is AuthenticationState.Error ||
                        authNState is AuthenticationState.NotConfigured
                    ) && (
                    authZState is AuthorizationState.Configured ||
                        authZState is AuthorizationState.SessionEstablished ||
                        authZState is AuthorizationState.Error
                    ) -> {
                    _federateToIdentityPool(providerToken, authProvider, options, onSuccess, onError)
                }
                else -> onError.accept(
                    InvalidStateException("Federation could not be completed.")
                )
            }
        }
    }

    private fun _federateToIdentityPool(
        providerToken: String,
        authProvider: AuthProvider,
        options: FederateToIdentityPoolOptions?,
        onSuccess: Consumer<FederateToIdentityPoolResult>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.FederatedToIdentityPool
                        && authZState is AuthorizationState.SessionEstablished -> {
                        token?.let(authStateMachine::cancel)
                        val credential = authZState.amplifyCredential as? AmplifyCredential.IdentityPoolFederated
                        val identityId = credential?.identityId
                        val awsCredentials = credential?.credentials
                        if (identityId != null && awsCredentials != null) {
                            val result = FederateToIdentityPoolResult(
                                credentials = awsCredentials,
                                identityId = identityId
                            )
                            onSuccess.accept(result)
                        } else {
                            onError.accept(
                                UnknownException(
                                    message = "Unable to parse credentials to expected output."
                                )
                            )
                        }
                    }
                    authNState is AuthenticationState.Error && authZState is AuthorizationState.Error -> {
                        token?.let(authStateMachine::cancel)
                        onError.accept(
                            CognitoAuthExceptionConverter.lookup(
                                authZState.exception,
                                "Federation could not be completed."
                            )
                        )
                    }
                }
            },
            {
                authStateMachine.send(
                    AuthorizationEvent(
                        AuthorizationEvent.EventType.StartFederationToIdentityPool(
                            token = FederatedToken(providerToken, authProvider.identityProviderName),
                            identityId = options?.developerProvidedIdentityId
                        )
                    )
                )
            }
        )
    }

    fun clearFederationToIdentityPool(
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            val authNState = authState.authNState
            val authZState = authState.authZState
            when {
                authState is AuthState.Configured &&
                    authNState is AuthenticationState.FederatedToIdentityPool &&
                    authZState is AuthorizationState.SessionEstablished -> {
                    val event = AuthenticationEvent(AuthenticationEvent.EventType.SignOutRequested(SignOutData()))
                    authStateMachine.send(event)
                    _clearFederationToIdentityPool(onSuccess, onError)
                }
                else -> {
                    onError.accept(InvalidStateException("Clearing of federation failed."))
                }
            }
        }
    }

    private fun _clearFederationToIdentityPool(onSuccess: Action, onError: Consumer<AuthException>) {
        _signOut {
            when (it) {
                is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                    onError.accept(it.error)
                }
                else -> onSuccess.call()
            }
        }
    }
}
