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
import androidx.annotation.WorkerThread
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AWSCognitoAuthMetadataType
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSTemporaryCredentials
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidOauthConfigurationException
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.exceptions.invalidstate.SignedInException
import com.amplifyframework.auth.cognito.exceptions.service.HostedUISignOutException
import com.amplifyframework.auth.cognito.exceptions.service.InvalidAccountTypeException
import com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException
import com.amplifyframework.auth.cognito.exceptions.service.UserCancelledException
import com.amplifyframework.auth.cognito.helpers.HostedUIHelper
import com.amplifyframework.auth.cognito.helpers.UserPoolSignInHelper
import com.amplifyframework.auth.cognito.helpers.getMFASetupTypeOrNull
import com.amplifyframework.auth.cognito.helpers.getMFATypeOrNull
import com.amplifyframework.auth.cognito.helpers.identityProviderName
import com.amplifyframework.auth.cognito.helpers.isMfaSetupSelectionChallenge
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmSignInOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignOutOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.options.FederateToIdentityPoolOptions
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.result.FederateToIdentityPoolResult
import com.amplifyframework.auth.cognito.result.GlobalSignOutError
import com.amplifyframework.auth.cognito.result.HostedUIError
import com.amplifyframework.auth.cognito.result.RevokeTokenError
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.StateChangeListenerToken
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.FederatedToken
import com.amplifyframework.statemachine.codegen.data.HostedUIErrorData
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import com.amplifyframework.statemachine.codegen.data.challengeNameType
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.HostedUIEvent
import com.amplifyframework.statemachine.codegen.events.SetupTOTPEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.HostedUISignInState
import com.amplifyframework.statemachine.codegen.states.SetupTOTPState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import com.amplifyframework.statemachine.codegen.states.WebAuthnSignInState
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

@Suppress("ktlint:standard:function-naming")
internal class RealAWSCognitoAuthPlugin(
    val configuration: AuthConfiguration,
    private val authEnvironment: AuthEnvironment,
    private val authStateMachine: AuthStateMachine,
    private val logger: Logger
) {

    private val lastPublishedHubEventName = AtomicReference<String>()

    init {
        addAuthStateChangeListener()
        configureAuthStates()
    }

    fun escapeHatch() = authEnvironment.cognitoAuthService

    @InternalAmplifyApi
    fun addToUserAgent(type: AWSCognitoAuthMetadataType, value: String) {
        authEnvironment.cognitoAuthService.customUserAgentPairs[type.key] = value
    }

    @WorkerThread
    @Throws(AmplifyException::class)
    fun initialize() {
        val token = StateChangeListenerToken()
        val latch = CountDownLatch(1)
        authStateMachine.listen(
            token,
            { authState ->
                if (authState is AuthState.Configured) {
                    authStateMachine.cancel(token)
                    latch.countDown()
                }
            },
            { }
        )
        try {
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw AmplifyException(
                "Failed to configure auth plugin.",
                "Make sure your amplifyconfiguration.json is valid"
            )
        }
    }

    internal suspend fun suspendWhileConfiguring() {
        authStateMachine.state.takeWhile { it !is AuthState.Configured && it !is AuthState.Error }.collect()
    }

    fun signIn(
        username: String?,
        password: String?,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        signIn(username, password, AuthSignInOptions.defaults(), onSuccess, onError)
    }

    fun signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            val signInOptions = options as? AWSCognitoAuthSignInOptions ?: AWSCognitoAuthSignInOptions.builder()
                .authFlowType(configuration.authFlowType)
                .build()
            when (authState.authNState) {
                is AuthenticationState.NotConfigured -> onError.accept(
                    InvalidUserPoolConfigurationException()
                )
                // Continue sign in
                is AuthenticationState.SignedOut,
                is AuthenticationState.Configured
                -> {
                    _signIn(username, password, signInOptions, onSuccess, onError)
                }
                is AuthenticationState.SignedIn -> onError.accept(SignedInException())
                is AuthenticationState.SigningIn -> {
                    val token = StateChangeListenerToken()
                    authStateMachine.listen(
                        token,
                        { authState ->
                            when (authState.authNState) {
                                is AuthenticationState.SignedOut -> {
                                    authStateMachine.cancel(token)
                                    _signIn(username, password, signInOptions, onSuccess, onError)
                                }
                                else -> Unit
                            }
                        },
                        {
                            authStateMachine.send(AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn()))
                        }
                    )
                }
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
        val token = StateChangeListenerToken()
        authStateMachine.listen(
            token,
            { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.Error -> {
                        authStateMachine.cancel(token)
                        onError.accept(
                            CognitoAuthExceptionConverter.lookup(authNState.exception, "Confirm sign in failed")
                        )
                    }
                    authNState is AuthenticationState.SignedIn &&
                        authZState is AuthorizationState.SessionEstablished -> {
                        authStateMachine.cancel(token)
                        onSuccess.accept(UserPoolSignInHelper.signInDoneResult())
                        sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
                    }
                    authNState is AuthenticationState.SigningIn -> {
                        var result: AuthSignInResult? = null
                        try {
                            result = UserPoolSignInHelper.checkNextStep(authNState.signInState)
                        } catch (e: Exception) {
                            authStateMachine.cancel(token)
                            onError.accept(CognitoAuthExceptionConverter.lookup(e, "Confirm sign in failed"))
                        }
                        if (result != null) {
                            authStateMachine.cancel(token)
                            onSuccess.accept(result)
                        }
                    }
                }
            },
            {
                val signInData = when (options.authFlowType ?: configuration.authFlowType) {
                    AuthFlowType.USER_SRP_AUTH -> {
                        SignInData.SRPSignInData(username, password, options.metadata, AuthFlowType.USER_SRP_AUTH)
                    }
                    AuthFlowType.CUSTOM_AUTH, AuthFlowType.CUSTOM_AUTH_WITHOUT_SRP -> {
                        SignInData.CustomAuthSignInData(username, options.metadata)
                    }
                    AuthFlowType.CUSTOM_AUTH_WITH_SRP -> {
                        SignInData.CustomSRPAuthSignInData(username, password, options.metadata)
                    }
                    AuthFlowType.USER_PASSWORD_AUTH -> {
                        SignInData.MigrationAuthSignInData(
                            username = username,
                            password = password,
                            metadata = options.metadata,
                            authFlowType = AuthFlowType.USER_PASSWORD_AUTH
                        )
                    }
                    AuthFlowType.USER_AUTH -> {
                        when (options.preferredFirstFactor) {
                            AuthFactorType.PASSWORD -> {
                                SignInData.MigrationAuthSignInData(
                                    username = username,
                                    password = password,
                                    metadata = options.metadata,
                                    authFlowType = AuthFlowType.USER_AUTH
                                )
                            }
                            AuthFactorType.PASSWORD_SRP -> {
                                SignInData.SRPSignInData(username, password, options.metadata, AuthFlowType.USER_AUTH)
                            }
                            else -> {
                                SignInData.UserAuthSignInData(
                                    username = username,
                                    preferredChallenge = options.preferredFirstFactor,
                                    callingActivity = options.callingActivity,
                                    metadata = options.metadata
                                )
                            }
                        }
                    }
                }
                val event = AuthenticationEvent(AuthenticationEvent.EventType.SignInRequested(signInData))
                authStateMachine.send(event)
            }
        )
    }

    fun confirmSignIn(
        challengeResponse: String,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        confirmSignIn(challengeResponse, AuthConfirmSignInOptions.defaults(), onSuccess, onError)
    }

    fun confirmSignIn(
        challengeResponse: String,
        options: AuthConfirmSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            val authNState = authState.authNState
            val signInState = (authNState as? AuthenticationState.SigningIn)?.signInState
            if (signInState is SignInState.ResolvingChallenge) {
                when (signInState.challengeState) {
                    is SignInChallengeState.WaitingForAnswer, is SignInChallengeState.Error -> {
                        _confirmSignIn(signInState, challengeResponse, options, onSuccess, onError)
                    }
                    else -> {
                        onError.accept(InvalidStateException())
                    }
                }
            } else if (signInState is SignInState.ResolvingTOTPSetup) {
                when (signInState.setupTOTPState) {
                    is SetupTOTPState.WaitingForAnswer, is SetupTOTPState.Error -> {
                        _confirmSignIn(signInState, challengeResponse, options, onSuccess, onError)
                    }

                    else -> onError.accept(InvalidStateException())
                }
            } else if (signInState is SignInState.SigningInWithWebAuthn) {
                when (signInState.webAuthnSignInState) {
                    is WebAuthnSignInState.Error -> _confirmSignIn(
                        signInState,
                        challengeResponse,
                        options,
                        onSuccess,
                        onError
                    )
                    else -> onError.accept(InvalidStateException())
                }
            } else {
                onError.accept(InvalidStateException())
            }
        }
    }

    private fun _confirmSignIn(
        signInState: SignInState,
        challengeResponse: String,
        options: AuthConfirmSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        val token = StateChangeListenerToken()
        authStateMachine.listen(
            token,
            { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.Error -> {
                        authStateMachine.cancel(token)
                        onError.accept(
                            CognitoAuthExceptionConverter.lookup(authNState.exception, "Confirm sign in failed")
                        )
                    }
                    authNState is AuthenticationState.SignedIn &&
                        authZState is AuthorizationState.SessionEstablished -> {
                        authStateMachine.cancel(token)
                        onSuccess.accept(UserPoolSignInHelper.signInDoneResult())
                        sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
                    }
                    authNState is AuthenticationState.SigningIn -> {
                        var result: AuthSignInResult? = null
                        try {
                            result = UserPoolSignInHelper.checkNextStep(authNState.signInState)
                        } catch (e: Exception) {
                            authStateMachine.cancel(token)
                            onError.accept(CognitoAuthExceptionConverter.lookup(e, "Confirm sign in failed"))
                        }
                        if (result != null) {
                            authStateMachine.cancel(token)
                            onSuccess.accept(result)
                        }
                    }
                }
            },
            {
                val awsCognitoConfirmSignInOptions = options as? AWSCognitoAuthConfirmSignInOptions
                val metadata = awsCognitoConfirmSignInOptions?.metadata ?: emptyMap()
                val userAttributes = awsCognitoConfirmSignInOptions?.userAttributes ?: emptyList()
                when (signInState) {
                    is SignInState.ResolvingChallenge -> {
                        val challengeState = signInState.challengeState
                        if (challengeState is SignInChallengeState.WaitingForAnswer &&
                            challengeState.challenge.challengeNameType == ChallengeNameType.SelectMfaType &&
                            getMFATypeOrNull(challengeResponse) == null
                        ) {
                            val error = InvalidParameterException(
                                message = "Value for challengeResponse must be one of " +
                                    "SMS_MFA, EMAIL_OTP or SOFTWARE_TOKEN_MFA"
                            )
                            onError.accept(error)
                            authStateMachine.cancel(token)
                        } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                            isMfaSetupSelectionChallenge(challengeState.challenge) &&
                            getMFASetupTypeOrNull(challengeResponse) == null
                        ) {
                            val error = InvalidParameterException(
                                message = "Value for challengeResponse must be one of EMAIL_OTP or SOFTWARE_TOKEN_MFA"
                            )
                            onError.accept(error)
                            authStateMachine.cancel(token)
                        } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                            challengeState.challenge.challengeNameType == ChallengeNameType.SelectChallenge &&
                            challengeResponse == AuthFactorType.WEB_AUTHN.challengeResponse
                        ) {
                            val username = challengeState.challenge.username!!
                            val session = challengeState.challenge.session
                            val signInContext = WebAuthnSignInContext(
                                username = username,
                                callingActivity = awsCognitoConfirmSignInOptions?.callingActivity ?: WeakReference(
                                    null
                                ),
                                session = session
                            )
                            val event = SignInEvent(SignInEvent.EventType.InitiateWebAuthnSignIn(signInContext))
                            authStateMachine.send(event)
                        } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                            challengeState.challenge.challengeNameType == ChallengeNameType.SelectChallenge &&
                            challengeResponse == ChallengeNameType.Password.value
                        ) {
                            val event = SignInEvent(
                                SignInEvent.EventType.ReceivedChallenge(
                                    AuthChallenge(
                                        challengeName = ChallengeNameType.Password.value,
                                        username = challengeState.challenge.username,
                                        session = challengeState.challenge.session,
                                        parameters = challengeState.challenge.parameters
                                    ),
                                    signInMethod = challengeState.signInMethod
                                )
                            )
                            authStateMachine.send(event)
                        } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                            challengeState.challenge.challengeNameType == ChallengeNameType.SelectChallenge &&
                            challengeResponse == ChallengeNameType.PasswordSrp.value
                        ) {
                            val event = SignInEvent(
                                SignInEvent.EventType.ReceivedChallenge(
                                    AuthChallenge(
                                        challengeName = ChallengeNameType.PasswordSrp.value,
                                        username = challengeState.challenge.username,
                                        session = challengeState.challenge.session,
                                        parameters = challengeState.challenge.parameters
                                    ),
                                    signInMethod = challengeState.signInMethod
                                )
                            )
                            authStateMachine.send(event)
                        } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                            challengeState.challenge.challengeNameType == ChallengeNameType.Password
                        ) {
                            val event = SignInEvent(
                                SignInEvent.EventType.InitiateMigrateAuth(
                                    username = challengeState.challenge.username!!,
                                    password = challengeResponse,
                                    metadata = metadata,
                                    authFlowType = AuthFlowType.USER_AUTH,
                                    respondToAuthChallenge = AuthChallenge(
                                        challengeName = ChallengeNameType.SelectChallenge.value,
                                        username = challengeState.challenge.username,
                                        session = challengeState.challenge.session!!,
                                        parameters = null
                                    )
                                )
                            )
                            authStateMachine.send(event)
                        } else if (challengeState is SignInChallengeState.WaitingForAnswer &&
                            challengeState.challenge.challengeNameType == ChallengeNameType.PasswordSrp
                        ) {
                            val event = SignInEvent(
                                SignInEvent.EventType.InitiateSignInWithSRP(
                                    username = challengeState.challenge.username!!,
                                    password = challengeResponse,
                                    metadata = metadata,
                                    authFlowType = AuthFlowType.USER_AUTH,
                                    respondToAuthChallenge = AuthChallenge(
                                        challengeName = ChallengeNameType.SelectChallenge.value,
                                        username = challengeState.challenge.username,
                                        session = challengeState.challenge.session!!,
                                        parameters = null
                                    )
                                )
                            )
                            authStateMachine.send(event)
                        } else {
                            val event = SignInChallengeEvent(
                                SignInChallengeEvent.EventType.VerifyChallengeAnswer(
                                    challengeResponse,
                                    metadata,
                                    userAttributes
                                )
                            )
                            authStateMachine.send(event)
                        }
                    }

                    is SignInState.ResolvingTOTPSetup -> {
                        when (signInState.setupTOTPState) {
                            is SetupTOTPState.WaitingForAnswer -> {
                                val setupTOTPState =
                                    (signInState.setupTOTPState as SetupTOTPState.WaitingForAnswer)

                                val event = SetupTOTPEvent(
                                    SetupTOTPEvent.EventType.VerifyChallengeAnswer(
                                        challengeResponse,
                                        setupTOTPState.signInTOTPSetupData.username,
                                        setupTOTPState.signInTOTPSetupData.session,
                                        awsCognitoConfirmSignInOptions?.friendlyDeviceName,
                                        setupTOTPState.signInMethod
                                    )
                                )
                                authStateMachine.send(event)
                            }
                            is SetupTOTPState.Error -> {
                                val username =
                                    (signInState.setupTOTPState as SetupTOTPState.Error).username
                                val session =
                                    (signInState.setupTOTPState as SetupTOTPState.Error).session
                                val signInMethod =
                                    (signInState.setupTOTPState as SetupTOTPState.Error).signInMethod

                                val event = SetupTOTPEvent(
                                    SetupTOTPEvent.EventType.VerifyChallengeAnswer(
                                        challengeResponse,
                                        username,
                                        session,
                                        awsCognitoConfirmSignInOptions?.friendlyDeviceName,
                                        signInMethod
                                    )
                                )
                                authStateMachine.send(event)
                            }

                            else -> {
                                onError.accept(InvalidStateException())
                                authStateMachine.cancel(token)
                            }
                        }
                    }

                    is SignInState.SigningInWithWebAuthn -> {
                        if (signInState.webAuthnSignInState is WebAuthnSignInState.Error &&
                            challengeResponse == AuthFactorType.WEB_AUTHN.challengeResponse
                        ) {
                            val signInContext = (signInState.webAuthnSignInState as WebAuthnSignInState.Error).context
                            val event = SignInEvent(SignInEvent.EventType.InitiateWebAuthnSignIn(signInContext))
                            authStateMachine.send(event)
                        } else {
                            onError.accept(InvalidStateException())
                            authStateMachine.cancel(token)
                        }
                    }

                    else -> {
                        onError.accept(InvalidStateException())
                        authStateMachine.cancel(token)
                    }
                }
            }
        )
    }

    fun signInWithSocialWebUI(
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

    fun signInWithSocialWebUI(
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

    fun signInWithWebUI(
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        signInWithWebUI(callingActivity, AuthWebUISignInOptions.builder().build(), onSuccess, onError)
    }

    fun signInWithWebUI(
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
                is AuthenticationState.SigningIn -> {
                    val token = StateChangeListenerToken()
                    authStateMachine.listen(
                        token,
                        { authState ->
                            when (authState.authNState) {
                                is AuthenticationState.SignedOut -> {
                                    authStateMachine.cancel(token)
                                    _signInWithHostedUI(
                                        callingActivity = callingActivity,
                                        options = options,
                                        onSuccess = onSuccess,
                                        onError = onError,
                                        provider = provider
                                    )
                                }
                                else -> Unit
                            }
                        },
                        {
                            authStateMachine.send(AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn()))
                        }
                    )
                }
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
        val token = StateChangeListenerToken()
        authStateMachine.listen(
            token,
            { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.SigningIn -> {
                        val hostedUISignInState = authNState.signInState.hostedUISignInState
                        if (hostedUISignInState is HostedUISignInState.Error) {
                            authStateMachine.cancel(token)
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
                    authNState is AuthenticationState.SignedIn &&
                        authZState is AuthorizationState.SessionEstablished -> {
                        authStateMachine.cancel(token)
                        val authSignInResult =
                            AuthSignInResult(
                                true,
                                AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null, null, null, null)
                            )
                        onSuccess.accept(authSignInResult)
                        sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())
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

    fun handleWebUISignInResponse(intent: Intent?) {
        authStateMachine.getCurrentState {
            val callbackUri = intent?.data
            when (val authNState = it.authNState) {
                is AuthenticationState.SigningOut -> {
                    (authNState.signOutState as? SignOutState.SigningOutHostedUI)?.let { signOutState ->
                        if (callbackUri == null &&
                            !signOutState.bypassCancel &&
                            signOutState.signedInData.signInMethod !=
                            SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.UNKNOWN)
                        ) {
                            authStateMachine.send(
                                SignOutEvent(SignOutEvent.EventType.UserCancelled(signOutState.signedInData))
                            )
                        } else {
                            val hostedUIErrorData = if (callbackUri == null) {
                                // This error will be appended if sign out redirect failed with an UNKNOWN sign in
                                // method. We will provide a URL to allow the developer to manually retry.
                                HostedUIErrorData(
                                    url = authEnvironment.hostedUIClient?.createSignOutUri()?.toString(),
                                    error = HostedUISignOutException(authEnvironment.hostedUIClient != null)
                                )
                            } else {
                                null
                            }
                            if (signOutState.globalSignOut) {
                                authStateMachine.send(
                                    SignOutEvent(
                                        SignOutEvent.EventType.SignOutGlobally(
                                            signOutState.signedInData,
                                            hostedUIErrorData
                                        )
                                    )
                                )
                            } else {
                                authStateMachine.send(
                                    SignOutEvent(
                                        SignOutEvent.EventType.RevokeToken(
                                            signOutState.signedInData,
                                            hostedUIErrorData
                                        )
                                    )
                                )
                            }
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
                else -> {
                    logger.warn(
                        "Received handleWebUIResponse but ignoring because the user is not currently signing in " +
                            "or signing out"
                    )
                    Unit
                }
            }
        }
    }

    fun fetchAuthSession(onSuccess: Consumer<AuthSession>, onError: Consumer<AuthException>) {
        fetchAuthSession(AuthFetchSessionOptions.defaults(), onSuccess, onError)
    }

    fun fetchAuthSession(
        options: AuthFetchSessionOptions,
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        val forceRefresh = options.forceRefresh
        authStateMachine.getCurrentState { authState ->
            when (val authZState = authState.authZState) {
                is AuthorizationState.Configured -> {
                    authStateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchUnAuthSession))
                    _fetchAuthSession(onSuccess)
                }
                is AuthorizationState.SessionEstablished -> {
                    val credential = authZState.amplifyCredential
                    if (!credential.isValid() || forceRefresh) {
                        if (credential is AmplifyCredential.IdentityPoolFederated) {
                            authStateMachine.send(
                                AuthorizationEvent(
                                    AuthorizationEvent.EventType.StartFederationToIdentityPool(
                                        credential.federatedToken,
                                        credential.identityId,
                                        credential
                                    )
                                )
                            )
                        } else {
                            authStateMachine.send(
                                AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(credential))
                            )
                        }
                        _fetchAuthSession(onSuccess)
                    } else {
                        onSuccess.accept(credential.getCognitoSession())
                    }
                }
                is AuthorizationState.Error -> {
                    val error = authZState.exception
                    if (error is SessionError) {
                        val amplifyCredential = error.amplifyCredential
                        if (amplifyCredential is AmplifyCredential.IdentityPoolFederated) {
                            authStateMachine.send(
                                AuthorizationEvent(
                                    AuthorizationEvent.EventType.StartFederationToIdentityPool(
                                        amplifyCredential.federatedToken,
                                        amplifyCredential.identityId,
                                        amplifyCredential
                                    )
                                )
                            )
                        } else {
                            authStateMachine.send(
                                AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(amplifyCredential))
                            )
                        }
                        _fetchAuthSession(onSuccess)
                    } else {
                        onError.accept(InvalidStateException())
                    }
                }
                else -> onError.accept(InvalidStateException())
            }
        }
    }

    private fun _fetchAuthSession(onSuccess: Consumer<AuthSession>) {
        val token = StateChangeListenerToken()
        authStateMachine.listen(
            token,
            { authState ->
                when (val authZState = authState.authZState) {
                    is AuthorizationState.SessionEstablished -> {
                        authStateMachine.cancel(token)
                        onSuccess.accept(authZState.amplifyCredential.getCognitoSession())
                    }
                    is AuthorizationState.Error -> {
                        authStateMachine.cancel(token)
                        when (val error = authZState.exception) {
                            is SessionError -> {
                                when (val innerException = error.exception) {
                                    is SignedOutException -> {
                                        onSuccess.accept(error.amplifyCredential.getCognitoSession(innerException))
                                    }
                                    is SessionExpiredException -> {
                                        onSuccess.accept(error.amplifyCredential.getCognitoSession(innerException))
                                        sendHubEvent(AuthChannelEventName.SESSION_EXPIRED.toString())
                                    }
                                    is ServiceException -> {
                                        onSuccess.accept(error.amplifyCredential.getCognitoSession(innerException))
                                    }
                                    is NotAuthorizedException -> {
                                        onSuccess.accept(error.amplifyCredential.getCognitoSession(innerException))
                                    }
                                    else -> {
                                        val errorResult = UnknownException("Fetch auth session failed.", innerException)
                                        onSuccess.accept(error.amplifyCredential.getCognitoSession(errorResult))
                                    }
                                }
                            }
                            is ConfigurationException -> {
                                val errorResult = InvalidAccountTypeException(error)
                                onSuccess.accept(AmplifyCredential.Empty.getCognitoSession(errorResult))
                            }
                            else -> {
                                val errorResult = UnknownException("Fetch auth session failed.", error)
                                onSuccess.accept(AmplifyCredential.Empty.getCognitoSession(errorResult))
                            }
                        }
                    }
                    else -> Unit
                }
            },
            null
        )
    }

    fun signOut(onComplete: Consumer<AuthSignOutResult>) {
        signOut(AuthSignOutOptions.builder().build(), onComplete)
    }

    fun signOut(options: AuthSignOutOptions, onComplete: Consumer<AuthSignOutResult>) {
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
                    _signOut(onComplete = onComplete)
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

    private fun _signOut(sendHubEvent: Boolean = true, onComplete: Consumer<AuthSignOutResult>) {
        val token = StateChangeListenerToken()
        var cancellationException: UserCancelledException? = null
        authStateMachine.listen(
            token,
            { authState ->
                if (authState is AuthState.Configured) {
                    val (authNState, authZState) = authState
                    when {
                        authNState is AuthenticationState.SignedOut && authZState is AuthorizationState.Configured -> {
                            authStateMachine.cancel(token)
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
                                if (sendHubEvent) {
                                    sendHubEvent(AuthChannelEventName.SIGNED_OUT.toString())
                                }
                            } else {
                                onComplete.accept(AWSCognitoAuthSignOutResult.CompleteSignOut)
                                if (sendHubEvent) {
                                    sendHubEvent(AuthChannelEventName.SIGNED_OUT.toString())
                                }
                            }
                        }
                        authNState is AuthenticationState.Error -> {
                            authStateMachine.cancel(token)
                            onComplete.accept(
                                AWSCognitoAuthSignOutResult.FailedSignOut(
                                    CognitoAuthExceptionConverter.lookup(authNState.exception, "Sign out failed.")
                                )
                            )
                        }
                        authNState is AuthenticationState.SigningOut -> {
                            val state = authNState.signOutState
                            if (state is SignOutState.Error && state.exception is UserCancelledException) {
                                cancellationException = state.exception
                            }
                        }
                        authNState is AuthenticationState.SignedIn && cancellationException != null -> {
                            authStateMachine.cancel(token)
                            cancellationException?.let {
                                onComplete.accept(AWSCognitoAuthSignOutResult.FailedSignOut(it))
                            }
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

    private fun addAuthStateChangeListener() {
        authStateMachine.listen(
            StateChangeListenerToken(),
            { authState -> logger.verbose("Auth State Change: $authState") },
            null
        )
    }

    private fun configureAuthStates() {
        val token = StateChangeListenerToken()
        authStateMachine.listen(
            token,
            { authState ->
                when (authState) {
                    is AuthState.Configured -> {
                        authStateMachine.cancel(token)
                    }
                    else -> Unit // handle errors
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
                (
                    authNState is AuthenticationState.SignedOut ||
                        authNState is AuthenticationState.Error ||
                        authNState is AuthenticationState.NotConfigured ||
                        authNState is AuthenticationState.FederatedToIdentityPool
                    ) &&
                    (
                        authZState is AuthorizationState.Configured ||
                            authZState is AuthorizationState.SessionEstablished ||
                            authZState is AuthorizationState.Error
                        ) -> {
                    val existingCredential = when (authZState) {
                        is AuthorizationState.SessionEstablished -> authZState.amplifyCredential
                        is AuthorizationState.Error -> {
                            (authZState.exception as? SessionError)?.amplifyCredential
                        }
                        else -> null
                    }
                    authStateMachine.send(
                        AuthorizationEvent(
                            AuthorizationEvent.EventType.StartFederationToIdentityPool(
                                token = FederatedToken(providerToken, authProvider.identityProviderName),
                                identityId = options?.developerProvidedIdentityId,
                                existingCredential
                            )
                        )
                    )

                    _federateToIdentityPool(onSuccess, onError)
                }
                else -> onError.accept(
                    InvalidStateException("Federation could not be completed.")
                )
            }
        }
    }

    private fun _federateToIdentityPool(
        onSuccess: Consumer<FederateToIdentityPoolResult>,
        onError: Consumer<AuthException>
    ) {
        val token = StateChangeListenerToken()
        authStateMachine.listen(
            token,
            { authState ->
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.FederatedToIdentityPool &&
                        authZState is AuthorizationState.SessionEstablished -> {
                        authStateMachine.cancel(token)
                        val credential = authZState.amplifyCredential as? AmplifyCredential.IdentityPoolFederated
                        val identityId = credential?.identityId
                        val awsCredentials = credential?.credentials
                        val temporaryAwsCredentials = AWSCredentials.createAWSCredentials(
                            awsCredentials?.accessKeyId,
                            awsCredentials?.secretAccessKey,
                            awsCredentials?.sessionToken,
                            awsCredentials?.expiration
                        ) as? AWSTemporaryCredentials
                        if (identityId != null && temporaryAwsCredentials != null) {
                            val result = FederateToIdentityPoolResult(
                                credentials = temporaryAwsCredentials,
                                identityId = identityId
                            )
                            onSuccess.accept(result)
                            sendHubEvent(AWSCognitoAuthChannelEventName.FEDERATED_TO_IDENTITY_POOL.toString())
                        } else {
                            onError.accept(
                                UnknownException(
                                    message = "Unable to parse credentials to expected output."
                                )
                            )
                        }
                    }
                    authNState is AuthenticationState.Error && authZState is AuthorizationState.Error -> {
                        authStateMachine.cancel(token)
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
            }
        )
    }

    fun clearFederationToIdentityPool(onSuccess: Action, onError: Consumer<AuthException>) {
        authStateMachine.getCurrentState { authState ->
            val authNState = authState.authNState
            val authZState = authState.authZState
            when {
                authState is AuthState.Configured &&
                    (
                        authNState is AuthenticationState.FederatedToIdentityPool &&
                            authZState is AuthorizationState.SessionEstablished
                        ) ||
                    (
                        authZState is AuthorizationState.Error &&
                            authZState.exception is SessionError &&
                            authZState.exception.amplifyCredential is AmplifyCredential.IdentityPoolFederated
                        ) -> {
                    val event = AuthenticationEvent(AuthenticationEvent.EventType.ClearFederationToIdentityPool())
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
        _signOut(sendHubEvent = false) {
            when (it) {
                is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                    onError.accept(it.exception)
                }
                else -> {
                    onSuccess.call()
                    sendHubEvent(AWSCognitoAuthChannelEventName.FEDERATION_TO_IDENTITY_POOL_CLEARED.toString())
                }
            }
        }
    }

    private inline fun <T : Any> callSafely(
        onSuccess: Consumer<T>,
        onError: Consumer<AuthException>,
        fallbackMessage: String,
        func: () -> T?
    ) {
        val result: T?
        try {
            result = func()
        } catch (e: Exception) {
            onError.accept(CognitoAuthExceptionConverter.lookup(e, fallbackMessage))
            return
        }
        if (result != null) {
            onSuccess.accept(result)
        }
    }

    private fun sendHubEvent(eventName: String) {
        if (lastPublishedHubEventName.get() != eventName) {
            lastPublishedHubEventName.set(eventName)
            Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(eventName))
        }
    }
}
