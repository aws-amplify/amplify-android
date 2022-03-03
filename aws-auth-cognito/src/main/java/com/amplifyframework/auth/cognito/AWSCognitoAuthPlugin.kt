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
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.*
import com.amplifyframework.auth.cognito.data.*
import com.amplifyframework.auth.cognito.events.AuthEvent
import com.amplifyframework.auth.cognito.events.AuthEvent.EventType.ConfigureAuth
import com.amplifyframework.auth.cognito.events.AuthenticationEvent
import com.amplifyframework.auth.cognito.events.CredentialStoreEvent
import com.amplifyframework.auth.cognito.states.AuthenticationState
import com.amplifyframework.auth.cognito.states.CredentialStoreState
import com.amplifyframework.auth.cognito.states.SRPSignInState
import com.amplifyframework.auth.cognito.states.SignUpState
import com.amplifyframework.auth.options.*
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.statemachine.StateChangeListenerToken
import org.json.JSONException
import org.json.JSONObject

/**
 * A Cognito implementation of the Auth Plugin.
 */
class AWSCognitoAuthPlugin : AuthPlugin<AWSCognitoAuthServiceBehavior>() {
    companion object {
        private const val AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin"
    }

    private var authEnvironment = AuthEnvironment()
    private var authStateMachine = AuthStateMachine(authEnvironment)
    private lateinit var authConfiguration: AuthConfiguration

    private var credentialStoreEnvironment = CredentialStoreEnvironment()
    private var credentialStoreStateMachine = CredentialStoreStateMachine(credentialStoreEnvironment)


    override fun signUp(
            username: String,
            password: String,
            options: AuthSignUpOptions,
            onSuccess: Consumer<AuthSignUpResult>,
            onError: Consumer<AuthException>
    ) {
        authStateMachine.listen({ authState ->
            when (val signUpState = authState.authNState.let { it?.signUpState }) {
                is SignUpState.SigningUpInitiated -> {
                    val authSignUpResult = AuthSignUpResult(
                            true,
                            AuthNextSignUpStep(AuthSignUpStep.CONFIRM_SIGN_UP_STEP, mapOf(), null),
                            null
                    )
                    onSuccess.accept(authSignUpResult)
                }
                is SignUpState.Error -> onError.accept(CognitoAuthExceptionConverter.lookup(signUpState.exception, "Sign up failed."))
                else -> {}
            }
        }, {
            val event =
                    AuthenticationEvent(
                            AuthenticationEvent.EventType.SignUpRequested(username, password, options)
                    )
            authStateMachine.send(event)
        })
    }

    override fun confirmSignUp(
            username: String,
            confirmationCode: String,
            options: AuthConfirmSignUpOptions,
            onSuccess: Consumer<AuthSignUpResult>,
            onError: Consumer<AuthException>
    ) {
        authStateMachine.listen({ authState ->
            when (val signUpState = authState.authNState.let { it?.signUpState }) {
                is SignUpState.SignedUp -> {
                    val authSignUpResult = AuthSignUpResult(
                            true,
                            AuthNextSignUpStep(AuthSignUpStep.DONE, mapOf(), null),
                            null
                    )
                    onSuccess.accept(authSignUpResult)
                }
                is SignUpState.Error -> onError.accept(CognitoAuthExceptionConverter.lookup(signUpState.exception, "Confirm sign up failed."))
                else -> {}
            }
        }, {
            val event =
                    AuthenticationEvent(
                            AuthenticationEvent.EventType.ConfirmSignUpRequested(username, confirmationCode)
                    )
            authStateMachine.send(event)
        })
    }

    override fun confirmSignUp(
            username: String,
            confirmationCode: String,
            onSuccess: Consumer<AuthSignUpResult>,
            onError: Consumer<AuthException>
    ) {
        confirmSignUp(username, confirmationCode, AuthConfirmSignUpOptions.defaults(), onSuccess, onError)
    }

    override fun resendSignUpCode(
            username: String,
            options: AuthResendSignUpCodeOptions,
            onSuccess: Consumer<AuthSignUpResult>,
            onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun resendSignUpCode(
            username: String,
            onSuccess: Consumer<AuthSignUpResult>,
            onError: Consumer<AuthException>
    ) {
        TODO("Not yet implemented")
    }

    override fun signIn(
            username: String?,
            password: String?,
            options: AuthSignInOptions,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
    ) {
        authStateMachine.listen({ authState ->
            when (val srpSignInState = authState.authNState.let { it?.srpSignInState }) {
                is SRPSignInState.SignedIn -> {
                    val authSignInResult = AuthSignInResult(
                            true, AuthNextSignInStep(AuthSignInStep.DONE, mapOf(), null)
                    )
                    onSuccess.accept(authSignInResult)
                }
                is SRPSignInState.Error -> onError.accept(CognitoAuthExceptionConverter.lookup(srpSignInState.exception, "Sign in failed."))
                else -> {}
            }
        }, {
            val event =
                    AuthenticationEvent(
                            AuthenticationEvent.EventType.SignInRequested(username, password, options)
                    )
            authStateMachine.send(event)
        })
    }

    override fun signIn(
            username: String?,
            password: String?,
            onSuccess: Consumer<AuthSignInResult>,
            onError: Consumer<AuthException>
    ) {
        signIn(username, password, AuthSignInOptions.defaults(), onSuccess, onError)
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
        TODO("Not yet implemented")
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

    override fun getCurrentUser(): AuthUser {
        TODO("Not yet implemented")
    }

    override fun signOut(onSuccess: Action, onError: Consumer<AuthException>) {
        signOut(AuthSignOutOptions.builder().build(), onSuccess, onError)
    }

    override fun signOut(
            options: AuthSignOutOptions,
            onSuccess: Action,
            onError: Consumer<AuthException>
    ) {
        authStateMachine.listen({ authState ->
            val authNSState = authState.authNState.takeIf { it is AuthenticationState.SignedOut }
            authNSState?.apply { onSuccess.call() }
        }, {
            val event =
                    AuthenticationEvent(AuthenticationEvent.EventType.SignOutRequested(options.isGlobalSignOut))
            authStateMachine.send(event)
        })
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            authConfiguration = AuthConfiguration.fromJson(pluginConfiguration).build()
            credentialStoreEnvironment.applicationContext = context.applicationContext
            val awsCognitoAuthCredentialStore = AWSCognitoAuthCredentialStore(credentialStoreEnvironment.applicationContext, authConfiguration)
            authEnvironment.awsCognitoAuthCredentialStore = awsCognitoAuthCredentialStore
            credentialStoreEnvironment.credentialStore =
                    awsCognitoAuthCredentialStore
            credentialStoreEnvironment.legacyCredentialStore =
                    AWSCognitoLegacyCredentialStore(credentialStoreEnvironment.applicationContext, authConfiguration)
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
        token = credentialStoreStateMachine.listen({
            when (it) {
                is CredentialStoreState.Error -> {
                    authStateMachine.send(AuthEvent(ConfigureAuth(authConfiguration, null)))
                    token?.let(credentialStoreStateMachine::cancel)
                }
                is CredentialStoreState.Success -> {
                    authStateMachine.send(AuthEvent(ConfigureAuth(authConfiguration, it.storedCredentials)))
                    token?.let(credentialStoreStateMachine::cancel)
                }
                else -> {
                    // no - op
                }
            }
        }, {
            credentialStoreStateMachine
                    .send(CredentialStoreEvent(CredentialStoreEvent.EventType.MigrateLegacyCredentialStore()))
        })
    }

    override fun getEscapeHatch() = authEnvironment.cognitoAuthService

    override fun getPluginKey() = AUTH_PLUGIN_KEY

    override fun getVersion() = BuildConfig.VERSION_NAME
}