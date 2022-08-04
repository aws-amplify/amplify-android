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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChangePasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceRememberedStatusType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ListDevicesRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateDeviceStatusRequest
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
import com.amplifyframework.auth.cognito.helpers.JWTParser
import com.amplifyframework.auth.cognito.usecases.ResetPasswordUseCase
import com.amplifyframework.auth.options.AWSCognitoAuthConfirmResetPasswordOptions
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
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.StateChangeListenerToken
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.CredentialStoreState
import com.amplifyframework.statemachine.codegen.states.DeleteUserState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import com.amplifyframework.statemachine.codegen.states.SignUpState
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
    private val credentialStoreStateMachine: CredentialStoreStateMachine,
    private val logger: Logger

) : AuthCategoryBehavior {

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
                    AuthException(
                        "Sign up failed.",
                        "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
                    )
                )
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
                    AuthException(
                        "Confirm sign up failed.",
                        "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
                    )
                )
                is AuthenticationState.SignedOut, is AuthenticationState.SigningUp -> {
                    // Continue confirm sign up
                    _confirmSignUp(username, confirmationCode, options, onSuccess, onError)
                }
                else -> onError.accept(AuthException.InvalidStateException())
            }
        }
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
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        resendSignUpCode(username, AuthResendSignUpCodeOptions.defaults(), onSuccess, onError)
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
                    AuthException(
                        "Sign in failed.",
                        "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
                    )
                )
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
                val authNState = authState.authNState
                val authZState = authState.authZState
                when {
                    authNState is AuthenticationState.SigningIn -> {
                        val srpSignInState = authNState.signInState?.srpSignInState
                        if (srpSignInState is SRPSignInState.Error) {
                            token?.let(authStateMachine::cancel)
                            onError.accept(
                                CognitoAuthExceptionConverter.lookup(srpSignInState.exception, "Sign in failed.")
                            )
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
                        Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SIGNED_IN))
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

    private suspend fun getSession(): AWSCognitoAuthSession {
        return suspendCoroutine { continuation ->
            fetchAuthSession(
                { continuation.resume(it as AWSCognitoAuthSession) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override fun fetchAuthSession(
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->

//            val (_,state) = (authState as AuthState.Configured)

            when (val authZState = authState.authZState) {
                is AuthorizationState.Configured -> _fetchAuthSession(onSuccess = onSuccess, onError = onError)
                is AuthorizationState.SessionEstablished -> {
                    val credential = authZState.amplifyCredential
                    if (credential.isValid()) onSuccess.accept(credential.getCognitoSession())
                    else _fetchAuthSession(true, credential, onSuccess = onSuccess, onError = onError)
                }
                else -> {
                    // no-op
                }
            }
        }
    }

    private fun _fetchAuthSession(
        refresh: Boolean = false,
        amplifyCredential: AmplifyCredential = AmplifyCredential.Empty,
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                when (val authZState = authState.authZState) {
                    is AuthorizationState.SessionEstablished -> {
                        // TODO: fix immediate session success
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
                    else -> {
                        // no-op
                    }
                }
            },
            {
                if (refresh) authStateMachine.send(
                    AuthorizationEvent(AuthorizationEvent.EventType.RefreshAuthSession(amplifyCredential))
                )
                else authStateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchAuthSession))
            }
        )
    }

    override fun rememberDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.SignedIn -> {
                    updateDevice(null, DeviceRememberedStatusType.Remembered, onSuccess, onError)
                }
                else -> {
                    onError.accept(AuthException.SignedOutException())
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
                val tokens = getSession().userPoolTokens
                // TODO: Update the stubbed device key when device SRP auth is implemented with its own store.
                authEnvironment.cognitoAuthService.cognitoIdentityProviderClient?.updateDeviceStatus(
                    UpdateDeviceStatusRequest.invoke {
                        accessToken = tokens.value?.accessToken
                        deviceKey = alternateDeviceId ?: "STUB_DEVICE_KEY"
                        deviceRememberedStatus = rememberedStatusType
                    }
                )
                onSuccess.call()
            } catch (e: Exception) {
                onError.accept(AuthException(e.localizedMessage, e, AuthException.TODO_RECOVERY_SUGGESTION))
            }
        }
    }

    override fun forgetDevice(
        device: AuthDevice,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            when (authState.authNState) {
                is AuthenticationState.SignedIn -> {
                    val deviceID = device.deviceId.ifEmpty { null }
                    updateDevice(deviceID, DeviceRememberedStatusType.NotRemembered, onSuccess, onError)
                }
                else -> {
                    onError.accept(AuthException.SignedOutException())
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
                    onError.accept(AuthException.SignedOutException())
                }
            }
        }
    }

    private fun _fetchDevices(onSuccess: Consumer<MutableList<AuthDevice>>, onError: Consumer<AuthException>) {
        GlobalScope.async {
            try {
                val tokens = getSession().userPoolTokens
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
                onError.accept(AuthException(e.localizedMessage, e, AuthException.TODO_RECOVERY_SUGGESTION))
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

            GlobalScope.launch {
                ResetPasswordUseCase(cognitoIdentityProviderClient, appClient).execute(
                    username,
                    options,
                    onSuccess,
                    onError
                )
            }
        } catch (ex: Exception) {
            onError.accept(AuthException.InvalidUserPoolConfigurationException(ex))
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
                    AuthException(
                        "Confirm Reset Password failed.",
                        "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
                    )
                )
                return@getCurrentState
            }

            GlobalScope.launch {
                try {
                    authEnvironment.cognitoAuthService.cognitoIdentityProviderClient!!.confirmForgotPassword {
                        this.username = username
                        this.confirmationCode = confirmationCode
                        password = newPassword
                        clientMetadata =
                            (options as? AWSCognitoAuthConfirmResetPasswordOptions)?.metadata ?: mapOf()
                        clientId = configuration.userPool?.appClient
                    }.let { onSuccess.call() }
                } catch (ex: Exception) {
                    onError.accept(CognitoAuthExceptionConverter.lookup(ex, AuthException.REPORT_BUG_TO_AWS_SUGGESTION))
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
                else -> onError.accept(AuthException.InvalidStateException())
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
            val tokens = getSession().userPoolTokens
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
                            val accessToken = getSession().userPoolTokens.value?.accessToken
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
                else -> onError.accept(AuthException.InvalidStateException())
            }
        }
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

    override fun getCurrentUser(
        onSuccess: Consumer<AuthUser>,
        onError: Consumer<AuthException>
    ) {
        authStateMachine.getCurrentState { authState ->
            if (authState.authNState !is AuthenticationState.SignedIn) {
                onError.accept(AuthException.SignedOutException())
                return@getCurrentState
            }

            GlobalScope.async {
                val accessToken = getSession().userPoolTokens.value?.accessToken
                accessToken?.run {
                    val userid = JWTParser.getClaim(accessToken, "sub") ?: ""
                    val username = JWTParser.getClaim(accessToken, "username") ?: ""
                    onSuccess.accept(AuthUser(userid, username))
                } ?: onError.accept(AuthException.InvalidUserPoolConfigurationException())
            }
        }
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
                is AuthenticationState.NotConfigured -> onSuccess.call()
                // Continue sign out and clear auth or guest credentials
                is AuthenticationState.SignedIn, is AuthenticationState.SignedOut ->
                    _signOut(options, onSuccess, onError)
                else -> onError.accept(AuthException.InvalidStateException())
            }
        }
    }

    private fun _signOut(options: AuthSignOutOptions, onSuccess: Action, onError: Consumer<AuthException>) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                if (authState is AuthState.Configured) {
                    val (authNState, authZState) = authState
                    when {
                        authNState is AuthenticationState.SignedOut && authZState is AuthorizationState.Configured -> {
                            token?.let(authStateMachine::cancel)
                            onSuccess.call()
                            Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SIGNED_OUT))
                        }
                        authNState is AuthenticationState.Error -> {
                            token?.let(authStateMachine::cancel)
                            onError.accept(
                                CognitoAuthExceptionConverter.lookup(authNState.exception, "Sign out failed.")
                            )
                        }
                        else -> {
                            // no-op
                        }
                    }
                }
            },
            {
                val event = AuthenticationEvent(AuthenticationEvent.EventType.SignOutRequested(options.isGlobalSignOut))
                authStateMachine.send(event)
            }
        )
    }

    override fun deleteUser(onSuccess: Action, onError: Consumer<AuthException>) {
        var listenerToken: StateChangeListenerToken? = null
        listenerToken = credentialStoreStateMachine.listen(
            {
                when (it) {
                    is CredentialStoreState.Success -> {
                        listenerToken?.let(credentialStoreStateMachine::cancel)
                        when (val credential = it.storedCredentials) {
                            is AmplifyCredential.UserPool -> _deleteUser(
                                credential.tokens.accessToken!!,
                                onSuccess,
                                onError
                            )
                            is AmplifyCredential.UserAndIdentityPool -> _deleteUser(
                                credential.tokens.accessToken!!,
                                onSuccess,
                                onError
                            )
                            else -> onError.accept(AuthException.InvalidAccountTypeException())
                        }
                    }
                    is CredentialStoreState.Error -> {
                        listenerToken?.let(credentialStoreStateMachine::cancel)
                        DeleteUserEvent(DeleteUserEvent.EventType.ThrowError(AuthException.UnknownException(it.error)))
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

    private fun _deleteUser(token: String, onSuccess: Action, onError: Consumer<AuthException>) {
        var listenerToken: StateChangeListenerToken? = null
        listenerToken = authStateMachine.listen(
            { authState ->
                when (authState.authNState?.signOutState) {
                    is SignOutState.SignedOut -> {
                        clearCredentialStore(
                            onSuccess = {
                                val event = DeleteUserEvent(DeleteUserEvent.EventType.SignOutDeletedUser())
                                authStateMachine.send(event)
                            },
                            onError = {
                                val event = DeleteUserEvent(DeleteUserEvent.EventType.ThrowError(it.error))
                                authStateMachine.send(event)
                            }
                        )
                    }
                    else -> {
                        // No-op
                    }
                }
                when (val deleteUserState = authState.authZState?.deleteUserState) {
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

    private fun clearCredentialStore(onSuccess: () -> Unit, onError: (error: CredentialStoreState.Error) -> Unit) {
        var token: StateChangeListenerToken? = null
        token = credentialStoreStateMachine.listen(
            {
                when (it) {
                    is CredentialStoreState.Success -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        onSuccess()
                    }
                    is CredentialStoreState.Error -> {
                        token?.let(credentialStoreStateMachine::cancel)
                        onError(it)
                    }
                    else -> {
                        // no op
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

    private fun addAuthStateChangeListener() {
        authStateMachine.listen(
            { authState ->
                logger.verbose("Auth State Change: $authState")

                // TODO: listen and dispatch hub events

                when (authState) {
                    is AuthState.WaitingForCachedCredentials -> credentialStoreStateMachine.send(
                        CredentialStoreEvent(CredentialStoreEvent.EventType.LoadCredentialStore())
                    )
                    is AuthState.Configured -> when (val authZState = authState.authZState) {
                        is AuthorizationState.WaitingToStore -> credentialStoreStateMachine.send(
                            CredentialStoreEvent(
                                CredentialStoreEvent.EventType.StoreCredentials(authZState.amplifyCredential)
                            )
                        )
                    }
                }
            },
            null
        )

        credentialStoreStateMachine.listen(
            { storeState ->
                logger.verbose("Credential Store State Change: $storeState")

                when (storeState) {
                    is CredentialStoreState.Success -> authStateMachine.send(
                        AuthEvent(AuthEvent.EventType.ReceivedCachedCredentials(storeState.storedCredentials))
                    )
                    is CredentialStoreState.Error -> authStateMachine.send(
                        AuthEvent(AuthEvent.EventType.CachedCredentialsFailed)
                    )
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
                        token?.let(credentialStoreStateMachine::cancel)
                    }
                    else -> {} // handle errors
                }
            },
            {
                authStateMachine.send(AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration)))
            }
        )
    }
}
