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
import androidx.annotation.VisibleForTesting
import com.amplifyframework.AmplifyException
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
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
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
import com.amplifyframework.statemachine.codegen.states.FetchAwsCredentialsState
import com.amplifyframework.statemachine.codegen.states.FetchIdentityState
import com.amplifyframework.statemachine.codegen.states.FetchUserPoolTokensState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import com.amplifyframework.util.UserAgent
import org.json.JSONException
import org.json.JSONObject

/**
 * A Cognito implementation of the Auth Plugin.
 */
class AWSCognitoAuthPlugin : AuthPlugin<AWSCognitoAuthServiceBehavior>() {
    companion object {
        const val AWS_COGNITO_AUTH_LOG_NAMESPACE = "amplify:aws-cognito-auth:%s"

        private const val AWS_COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin"
    }

    private val logger =
        Amplify.Logging.forNamespace(AWS_COGNITO_AUTH_LOG_NAMESPACE.format(this::class.java.simpleName))

    @VisibleForTesting
    internal lateinit var realPlugin: RealAWSCognitoAuthPlugin

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            val configuration = AuthConfiguration.fromJson(pluginConfiguration).build()
            val authEnvironment = AuthEnvironment(
                configuration,
                AWSCognitoAuthServiceBehavior.fromConfiguration(configuration),
                logger
            )
            val authStateMachine = AuthStateMachine(authEnvironment)
            System.setProperty("aws.frameworkMetadata", UserAgent.string())
            val credentialStoreStateMachine = createCredentialStoreStateMachine(configuration, context)
            realPlugin = RealAWSCognitoAuthPlugin(
                configuration,
                authEnvironment,
                authStateMachine,
                credentialStoreStateMachine,
                logger
            )
        } catch (exception: JSONException) {
            throw AuthException(
                "Failed to configure AWSCognitoAuthPlugin.",
                exception,
                "Make sure your amplifyconfiguration.json is valid."
            )
        }
    }

    override fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signUp(username, password, options, onSuccess, onError)
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmSignUp(username, confirmationCode, options, onSuccess, onError)
    }

    override fun confirmSignUp(
        username: String,
        confirmationCode: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmSignUp(username, confirmationCode, onSuccess, onError)
    }

    override fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resendSignUpCode(username, options, onSuccess, onError)
    }

    override fun resendSignUpCode(
        username: String,
        onSuccess: Consumer<AuthSignUpResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resendSignUpCode(username, onSuccess, onError)
    }

    override fun signIn(
        username: String?,
        password: String?,
        options: AuthSignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signIn(username, password, options, onSuccess, onError)
    }

    override fun signIn(
        username: String?,
        password: String?,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signIn(username, password, onSuccess, onError)
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
        realPlugin.confirmSignIn(confirmationCode, options, onSuccess, onError)
    }

    override fun confirmSignIn(
        confirmationCode: String,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmSignIn(confirmationCode, onSuccess, onError)
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signInWithSocialWebUI(provider, callingActivity, onSuccess, onError)
    }

    override fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signInWithSocialWebUI(provider, callingActivity, options, onSuccess, onError)
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signInWithWebUI(callingActivity, onSuccess, onError)
    }

    override fun signInWithWebUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signInWithWebUI(callingActivity, options, onSuccess, onError)
    }

    override fun handleWebUISignInResponse(intent: Intent?) {
        realPlugin.handleWebUISignInResponse(intent)
    }

    override fun fetchAuthSession(
        onSuccess: Consumer<AuthSession>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.fetchAuthSession(onSuccess, onError)
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
                        val fetchUserPoolTokensState = authZState.fetchAuthSessionState.fetchUserPoolTokensState
                        val fetchIdentityState = authZState.fetchAuthSessionState.fetchIdentityState
                        val fetchAwsCredentialsState = authZState.fetchAuthSessionState.fetchAwsCredentialsState
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
                                identityIdResult = when (configuration.identityPool) {
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
        realPlugin.rememberDevice(onSuccess, onError)
    }

    override fun forgetDevice(onSuccess: Action, onError: Consumer<AuthException>) {
        realPlugin.forgetDevice(onSuccess, onError)
    }

    override fun forgetDevice(
        device: AuthDevice,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.forgetDevice(device, onSuccess, onError)
    }

    override fun fetchDevices(
        onSuccess: Consumer<MutableList<AuthDevice>>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.fetchDevices(onSuccess, onError)
    }

    override fun resetPassword(
        username: String,
        options: AuthResetPasswordOptions,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resetPassword(username, options, onSuccess, onError)
    }

    override fun resetPassword(
        username: String,
        onSuccess: Consumer<AuthResetPasswordResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resetPassword(username, onSuccess, onError)
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmResetPassword(username, newPassword, confirmationCode, options, onSuccess, onError)
    }

    override fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmResetPassword(username, newPassword, confirmationCode, onSuccess, onError)
    }

    override fun updatePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updatePassword(oldPassword, newPassword, onSuccess, onError)
    }

    override fun fetchUserAttributes(
        onSuccess: Consumer<MutableList<AuthUserAttribute>>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.fetchUserAttributes(onSuccess, onError)
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updateUserAttribute(attribute, options, onSuccess, onError)
    }

    override fun updateUserAttribute(
        attribute: AuthUserAttribute,
        onSuccess: Consumer<AuthUpdateAttributeResult>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updateUserAttribute(attribute, onSuccess, onError)
    }

    override fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions,
        onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updateUserAttributes(attributes, options, onSuccess, onError)
    }

    override fun updateUserAttributes(
        attributes: MutableList<AuthUserAttribute>,
        onSuccess: Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.updateUserAttributes(attributes, onSuccess, onError)
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resendUserAttributeConfirmationCode(attributeKey, options, onSuccess, onError)
    }

    override fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        onSuccess: Consumer<AuthCodeDeliveryDetails>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.resendUserAttributeConfirmationCode(attributeKey, onSuccess, onError)
    }

    override fun confirmUserAttribute(
        attributeKey: AuthUserAttributeKey,
        confirmationCode: String,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.confirmUserAttribute(attributeKey, confirmationCode, onSuccess, onError)
    }

    override fun getCurrentUser(
        onSuccess: Consumer<AuthUser>,
        onError: Consumer<AuthException>
    ) {
        realPlugin.getCurrentUser(onSuccess, onError)
    }

    override fun signOut(onSuccess: Action, onError: Consumer<AuthException>) {
        realPlugin.signOut(onSuccess, onError)
    }

    override fun signOut(
        options: AuthSignOutOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        realPlugin.signOut(options, onSuccess, onError)

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

    override fun deleteUser(onSuccess: Action, onError: Consumer<AuthException>) {
        realPlugin.deleteUser(onSuccess, onError)

        var listenerToken: StateChangeListenerToken? = null
        listenerToken = credentialStoreStateMachine.listen(
            {
                when (it) {
                    is CredentialStoreState.Success -> {
                        listenerToken?.let(credentialStoreStateMachine::cancel)
                        if (it.storedCredentials?.cognitoUserPoolTokens?.accessToken != null) {
                            _deleteUser(it.storedCredentials.cognitoUserPoolTokens.accessToken, onSuccess, onError)
                        } else {
                            onError.accept(AuthException.InvalidAccountTypeException())
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

    private fun _signOut(
        options: AuthSignOutOptions,
        onSuccess: Action,
        onError: Consumer<AuthException>
    ) {
        var token: StateChangeListenerToken? = null
        token = authStateMachine.listen(
            { authState ->
                if (authState is AuthState.Configured) {
                    val (authNState, authZState) = authState
                    when {
                        authNState is AuthenticationState.SignedOut -> {
                            token?.let(authStateMachine::cancel)
                            onSuccess.call()
                            Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SIGNED_OUT))
                        }
                        authZState is AuthorizationState.Configured
                            || authZState is AuthorizationState.SessionEstablished -> {
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

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            configuration = AuthConfiguration.fromJson(pluginConfiguration).build()
            val authEnvironment = AuthEnvironment(configuration, configureCognitoClients(), logger)
            authStateMachine = AuthStateMachine(authEnvironment)
            System.setProperty("aws.frameworkMetadata", UserAgent.string())

            configureCredentialStore(pluginConfiguration, context)
            addStateChangeListeners()
            configureAuthStates()
        } catch (exception: JSONException) {
            throw AuthException(
                "Failed to configure AWSCognitoAuthPlugin.",
                exception,
                "Make sure your amplifyconfiguration.json is valid."
            )
        }
    }

    override fun getEscapeHatch() = realPlugin.escapeHatch()

    override fun getPluginKey() = AWS_COGNITO_AUTH_PLUGIN_KEY

    override fun getVersion() = BuildConfig.VERSION_NAME

    private fun createCredentialStoreStateMachine(
        configuration: AuthConfiguration,
        context: Context
    ): CredentialStoreStateMachine {
        val awsCognitoAuthCredentialStore = AWSCognitoAuthCredentialStore(context.applicationContext, configuration)
        val legacyCredentialStore = AWSCognitoLegacyCredentialStore(context.applicationContext, configuration)
        val credentialStoreEnvironment =
            CredentialStoreEnvironment(awsCognitoAuthCredentialStore, legacyCredentialStore)
        return CredentialStoreStateMachine(credentialStoreEnvironment)
    }

    private fun addStateChangeListeners() {
        authStateMachine.listen(
            { authState ->
                logger.verbose("Auth State Change: $authState")

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
