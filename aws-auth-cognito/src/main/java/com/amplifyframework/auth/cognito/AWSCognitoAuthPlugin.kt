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
import android.content.Context
import android.content.Intent
import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthPlugin
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
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
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.statemachine.StateChangeListenerToken
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.CredentialStoreState
import com.amplifyframework.statemachine.codegen.states.FetchAwsCredentialsState
import com.amplifyframework.statemachine.codegen.states.FetchIdentityState
import com.amplifyframework.statemachine.codegen.states.FetchUserPoolTokensState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import java.util.concurrent.Semaphore
import org.json.JSONException
import org.json.JSONObject

/**
 * A Cognito implementation of the Auth Plugin.
 */
class AWSCognitoAuthPlugin : AuthPlugin<AWSCognitoAuthServiceBehavior>() {
    companion object {
        private const val AWS_COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin"
    }

    private var authEnvironment = AuthEnvironment()
    private var authStateMachine = AuthStateMachine(authEnvironment)
    private lateinit var authConfiguration: AuthConfiguration

    private var credentialStoreEnvironment = CredentialStoreEnvironment()
    private var credentialStoreStateMachine =
        CredentialStoreStateMachine(credentialStoreEnvironment)

    override fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                // Continue sign up
                is AuthenticationState.SignedOut -> _signUp(username, password, options, onSuccess, onError)
                // Clean up from signing up state
                is AuthenticationState.SigningUp -> {
                    authStateMachine.send(AuthenticationEvent(AuthenticationEvent.EventType.ResetSignUp()))
                }
                else -> onError.accept(AuthException.InvalidStateException())
            }
        }
    }

    private fun _signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                val signUpState = authState.authNState.takeIf { it is AuthenticationState.SigningUp }?.signUpState
                when (signUpState) {
                    is SignUpState.SigningUpInitiated -> {
                        token?.let(authStateMachine::cancel)
                        val user = AuthUser(
                            signUpState.signedUpData.userId ?: "",
                            signUpState.signedUpData.username
                        )
                        val deliveryDetails = signUpState.signedUpData.codeDeliveryDetails
                        val authSignUpResult = AuthSignUpResult(
                            true,
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
                            user
                        )
                        onSuccess.accept(authSignUpResult)
                    }
                    is SignUpState.Error -> {
                        token?.let(authStateMachine::cancel)
                        onError.accept(CognitoAuthExceptionConverter.lookup(signUpState.exception, "Sign up failed."))
                    }
                    else -> {
                        // no-op
                    }
                }
            },
            {
                val event = SignUpEvent(SignUpEvent.EventType.InitiateSignUp(username, password, options))
                authStateMachine.send(event)
            }
        )
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
                is AuthenticationState.SignedOut, is AuthenticationState.SigningUp -> {
                    // Continue confirm sign up
                    _confirmSignUp(username, confirmationCode, options, onSuccess, onError)
                }
                else -> onError.accept(AuthException.InvalidStateException())
            }
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

    private fun _confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                val signUpState = authState.authNState.takeIf { it is AuthenticationState.SigningUp }?.signUpState
                when (signUpState) {
                    is SignUpState.SignedUp -> {
                        token?.let(authStateMachine::cancel)
                        val authSignUpResult = AuthSignUpResult(
                            true,
                            AuthNextSignUpStep(AuthSignUpStep.DONE, mapOf(), null),
                            null
                        )
                        onSuccess.accept(authSignUpResult)
                    }
                    is SignUpState.Error -> {
                        token?.let(authStateMachine::cancel)
                        onError.accept(
                            CognitoAuthExceptionConverter.lookup(signUpState.exception, "Confirm sign up failed.")
                        )
                    }
                    else -> {
                        // no-op
                    }
                }
            },
            {
                val event = SignUpEvent(SignUpEvent.EventType.ConfirmSignUp(username, confirmationCode))
                authStateMachine.send(event)
            }
        )
    }

    override fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.SignedOut, is AuthenticationState.SigningUp -> {
                    // Continue resend signup code
                    TODO("Not yet implemented")
                }
                else -> onError.accept(AuthException.InvalidStateException())
            }
        }
    }

    override fun resendSignUpCode(
        username: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        resendSignUpCode(username, AuthResendSignUpCodeOptions.defaults(), onSuccess, onError)
    }

    private fun _resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                val signUpState = authState.authNState.takeIf { it is AuthenticationState.SigningUp }?.signUpState
                when (signUpState) {
                    // TODO("Not yet implemented")
                    is SignUpState.Error -> {
                        token?.let(authStateMachine::cancel)
                        onError.accept(CognitoAuthExceptionConverter.lookup(signUpState.exception, "Sign up failed."))
                    }
                    else -> {
                        // no-op
                    }
                }
            },
            {
                val event = SignUpEvent(SignUpEvent.EventType.ResendSignUpCode(username, options))
                authStateMachine.send(event)
            }
        )
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
                // Continue sign in
                is AuthenticationState.SignedOut -> _signIn(username, password, options, onSuccess, onError)
                // Clean up from signing up state
                is AuthenticationState.SigningUp -> {
                    authStateMachine.send(AuthenticationEvent(AuthenticationEvent.EventType.ResetSignUp()))
                }
                is AuthenticationState.SignedIn -> onSuccess.accept(
                    AuthSignInResult(true, AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null))
                )
                else -> onError.accept(AuthException.InvalidStateException())
            }
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

    private fun _signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                when (val authNState = authState.authNState) {
                    is AuthenticationState.SigningIn -> {
                        val srpSignInState = authNState.srpSignInState
                        if (srpSignInState is SRPSignInState.Error) {
                            token?.let(authStateMachine::cancel)
                            onError.accept(
                                CognitoAuthExceptionConverter.lookup(srpSignInState.exception, "Sign in failed.")
                            )
                        }
                    }
                    is AuthenticationState.SignedIn -> {
                        token?.let(authStateMachine::cancel)
                        val cognitoUserPoolTokens = authNState.signedInData.cognitoUserPoolTokens
                        // Store tokens to credential store
                        waitForSignInCompletion(cognitoUserPoolTokens, onSuccess, onError)
                    }
                    else -> {
                        // no-op
                    }
                }
            },
            {
                val event = AuthenticationEvent(
                    AuthenticationEvent.EventType.SignInRequested(username, password, options)
                )
                authStateMachine.send(event)
            }
        )
    }

    private fun waitForSignInCompletion(
        tokens: CognitoUserPoolTokens,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = credentialStoreStateMachine.listen(
            {
                when {
                    it is CredentialStoreState.Success -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        val authSignInResult = AuthSignInResult(
                            true,
                            AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null)
                        )
                        onSuccess.accept(authSignInResult)
                        Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SIGNED_IN))
                    }
                    it is CredentialStoreState.Error -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        onError.accept(AuthException(it.error.message, "Try signing in again."))
                    }
                }
            },
            {
                credentialStoreStateMachine.send(
                    CredentialStoreEvent(
                        CredentialStoreEvent.EventType.StoreCredentials(AmplifyCredential(tokens, null, null))
                    )
                )
            }
        )
    }

    override fun confirmSignIn(
        confirmationCode: String,
        options: AuthConfirmSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun confirmSignIn(
        confirmationCode: String,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun handleWebUISignInResponse(intent: Intent?) {
        TODO("Not yet implemented")
    }

    override fun fetchAuthSession(
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = credentialStoreStateMachine.listen(
            {
                when (it) {
                    is CredentialStoreState.Success -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        _fetchAuthSession(it.storedCredentials, onSuccess, onError)
                    }
                    is CredentialStoreState.Error -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        _fetchAuthSession(null, onSuccess, onError)
                    }
                    else -> {
                        // no-op
                    }
                }
            },
            {
                credentialStoreStateMachine.send(
                    CredentialStoreEvent(CredentialStoreEvent.EventType.LoadCredentialStore())
                )
            }
        )
    }

    private fun _fetchAuthSession(
        credentials: AmplifyCredential?,
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        var userPoolTokensResult: AuthSessionResult<AWSCognitoUserPoolTokens>? = null
        var identityIdResult: AuthSessionResult<String>? = null
        var awsCredentialsResult: AuthSessionResult<Credentials>? = null
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                when (val authZState = authState.authZState) {
                    // TODO: handle if already in SessionEstablished
                    is AuthorizationState.SessionEstablished -> {
                        token?.let(authStateMachine::cancel)
                        val authSession = AWSCognitoAuthSession.fromAmplifyCredential(
                            authZState.amplifyCredential,
                            userPoolTokensResult,
                            identityIdResult,
                            awsCredentialsResult
                        )
                        authZState.amplifyCredential?.let { storeAuthSession(authSession, it, onSuccess, onError) }
                            ?: onSuccess.accept(authSession)
                    }
                    is AuthorizationState.FetchingAuthSession -> {
                        val fetchUserPoolTokensState = authZState.fetchAuthSessionState?.fetchUserPoolTokensState
                        val fetchIdentityState = authZState.fetchAuthSessionState?.fetchIdentityState
                        val fetchAwsCredentialsState = authZState.fetchAuthSessionState?.fetchAwsCredentialsState
                        when {
                            fetchUserPoolTokensState is FetchUserPoolTokensState.Error -> {
                                // invalid account type or unknown error - Ref #AWSCognitoAuthSession.SignedOutOrUnknown
                                // if no tokens found and no error -> signed out exception - Ref #AWSCognitoAuthSession.SignedOutOrUnknown
                                userPoolTokensResult = AuthSessionResult.failure(
                                    AuthException(
                                        "Signed out or refresh token expired.",
                                        fetchUserPoolTokensState.exception,
                                        "Sign in and try again. See the attached exception for more details."
                                    )
                                )
                            }
                            fetchIdentityState is FetchIdentityState.Error -> {
                                // if aws creds but no id -> should never happen - Ref #AWSCognitoAuthSession.UnreachableCase
                                // if no tokens and no id but has aws creds -> should never happen - Ref #AWSCognitoAuthSession.UnreachableCase
                                identityIdResult = when (authEnvironment.configuration.identityPool) {
                                    null -> AuthSessionResult.failure(AuthException.InvalidAccountTypeException())
                                    else -> AuthSessionResult.failure(
                                        AuthException(
                                            "Failed to fetch identity.",
                                            fetchIdentityState.exception,
                                            "Sign in or enable guest access. See the attached exception for more" +
                                                " details."
                                        )
                                    )
                                }
                            }
                            fetchAwsCredentialsState is FetchAwsCredentialsState.Error -> {
                                // invalid account type or unknown error
                                // if cognito identity configured -> guest access possible - Ref #AWSCognitoAuthSession.GuestAccessPossible, else -> invalid account type - Ref #AWSCognitoAuthSession.NoAWSCredentials
                                awsCredentialsResult = AuthSessionResult.failure(
                                    AuthException(
                                        "Failed to fetch AWS Credentials.",
                                        fetchAwsCredentialsState.exception,
                                        "Sign in or enable guest access. See the attached exception for more details."
                                    )
                                )
                            }
                        }
                    }
                    is AuthorizationState.Error -> {
                        token?.let(authStateMachine::cancel)
                        onError.accept(
                            CognitoAuthExceptionConverter.lookup(authZState.exception, "Fetch auth session failed.")
                        )
                    }
                    else -> {
                        // no-op
                    }
                }
            },
            {
                val event = AuthorizationEvent(AuthorizationEvent.EventType.FetchAuthSession(credentials))
                authStateMachine.send(event)
            }
        )
    }

    private fun storeAuthSession(
        session: AuthSession,
        credentials: AmplifyCredential,
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = credentialStoreStateMachine.listen(
            {
                when (it) {
                    is CredentialStoreState.Success -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        onSuccess.accept(session)
                    }
                    is CredentialStoreState.Error -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        onError.accept(AuthException.UnknownException(it.error))
                    }
                    else -> {
                        // no-op
                    }
                }
            },
            {
                credentialStoreStateMachine.send(
                    CredentialStoreEvent(CredentialStoreEvent.EventType.StoreCredentials(credentials))
                )
            }
        )
    }

    override fun rememberDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun forgetDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        TODO("Not yet implemented")
    }

    override fun forgetDevice(
        device: AuthDevice,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun fetchDevices(
        onSuccess: Consumer<MutableList<AuthDevice>>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun resetPassword(
        username: String,
        options: AuthResetPasswordOptions,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun resetPassword(
        username: String,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun confirmResetPassword(
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun confirmResetPassword(
        newPassword: String,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun updatePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun fetchUserAttributes(
        onSuccess: Consumer<MutableList<AuthUserAttribute>>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions,
        onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun confirmUserAttribute(
        attributeKey: AuthUserAttributeKey,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun getCurrentUser(): AuthUser? {
        var authUser: AuthUser? = null
        val semaphore = Semaphore(0)
        authStateMachine.getCurrentState { authState ->
            when (val authorizationState = authState.authNState) {
                is AuthenticationState.SignedIn -> {
                    authUser = AuthUser(
                        authorizationState.signedInData.userId,
                        authorizationState.signedInData.username
                    )
                }
            }
            semaphore.release()
        }
        try {
            semaphore.acquire()
        } catch (ex: InterruptedException) {
            throw Exception("Interrupted while waiting for current user", ex)
        }
        return authUser
    }

    override fun signOut(onSuccess: Action, onError: Consumer<AuthException>) {
        signOut(AuthSignOutOptions.builder().build(), onSuccess, onError)
    }

    override fun signOut(
        options: AuthSignOutOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                // Continue sign out
                is AuthenticationState.SignedIn -> _signOut(options, onSuccess, onError)
                is AuthenticationState.SignedOut -> onError.accept(AuthException.SignedOutException())
                else -> onError.accept(AuthException.InvalidStateException())
            }
        }
    }

    private fun _signOut(
        options: AuthSignOutOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                when (val authNState = authState.authNState) {
                    is AuthenticationState.SignedOut -> {
                        token?.let(authStateMachine::cancel)
                        onSuccess.call()
                        Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SIGNED_OUT))
                    }
                    is AuthenticationState.SigningOut -> {
                        val signOutState = authNState.signOutState
                        when {
                            signOutState is SignOutState.SigningOutLocally -> {
                                // Clear stored credentials
                                waitForSignOut(signOutState.signedInData.username)
                            }
                            signOutState is SignOutState.Error -> {
                                token?.let(authStateMachine::cancel)
                                onError.accept(
                                    CognitoAuthExceptionConverter.lookup(signOutState.exception, "Sign out failed.")
                                )
                            }
                        }
                    }
                    else -> {
                        // no-op
                    }
                }
            },
            {
                val event = AuthenticationEvent(AuthenticationEvent.EventType.SignOutRequested(options.isGlobalSignOut))
                authStateMachine.send(event)
            }
        )
    }

    private fun waitForSignOut(username: String) {
        var token: StateChangeListenerToken? = null
        token = credentialStoreStateMachine.listen(
            {
                when {
                    it is CredentialStoreState.Success -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        authStateMachine.send(
                            AuthenticationEvent(
                                AuthenticationEvent.EventType.InitializedSignedOut(SignedOutData(username))
                            )
                        )
                    }
                    it is CredentialStoreState.Error -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        authStateMachine.send(
                            SignOutEvent(SignOutEvent.EventType.SignedOutFailure(AuthException(it.error.message, "")))
                        )
                    }
                }
            },
            {
                credentialStoreStateMachine.send(
                    CredentialStoreEvent(CredentialStoreEvent.EventType.ClearCredentialStore())
                )
            }
        )
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            authConfiguration = AuthConfiguration.fromJson(pluginConfiguration).build()
            credentialStoreEnvironment.applicationContext = context.applicationContext
            val awsCognitoAuthCredentialStore = AWSCognitoAuthCredentialStore(
                credentialStoreEnvironment.applicationContext,
                authConfiguration
            )
            credentialStoreEnvironment.credentialStore = awsCognitoAuthCredentialStore

            credentialStoreEnvironment.legacyCredentialStore = AWSCognitoLegacyCredentialStore(
                credentialStoreEnvironment.applicationContext,
                authConfiguration
            )
        } catch (exception: JSONException) {
            throw AuthException(
                "Failed to configure AWSCognitoAuthPlugin.",
                exception,
                "Make sure your amplifyconfiguration.json is valid."
            )
        }

        sendCredentialStoreConfigure()
    }

    private fun sendCredentialStoreConfigure() {
        var token: StateChangeListenerToken? = null
        token = credentialStoreStateMachine.listen(
            {
                when {
                    it is CredentialStoreState.Error -> {
                        authStateMachine.send(AuthEvent(AuthEvent.EventType.ConfigureAuth(authConfiguration, null)))
                        token?.let(credentialStoreStateMachine::cancel)
                    }
                    it is CredentialStoreState.Success -> {
                        authStateMachine.send(
                            AuthEvent(AuthEvent.EventType.ConfigureAuth(authConfiguration, it.storedCredentials))
                        )
                        token?.let(credentialStoreStateMachine::cancel)
                    }
                }
            },
            {
                credentialStoreStateMachine.send(
                    CredentialStoreEvent(CredentialStoreEvent.EventType.MigrateLegacyCredentialStore())
                )
            }
        )
    }

    override fun getEscapeHatch() = authEnvironment.cognitoAuthService

    override fun getPluginKey() = AWS_COGNITO_AUTH_PLUGIN_KEY

    override fun getVersion() = BuildConfig.VERSION_NAME
}
