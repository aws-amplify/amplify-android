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

@file:Suppress("UNCHECKED_CAST")

package com.amplifyframework.auth.cognito.featuretest.serializers

import com.amplifyframework.auth.cognito.featuretest.serializers.AuthStatesProxy.Companion.format
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

@Serializable
internal data class AuthStatesProxy(
    val type: String = "AuthState",
    @Contextual
    @SerialName("AuthenticationState")
    val authNState: AuthenticationState? = null,
    @Contextual
    @SerialName("AuthorizationState")
    val authZState: AuthorizationState? = null,
    @Contextual
    @SerialName("SignUpState")
    val signUpState: SignUpState? = null,
    @Contextual
    @SerialName("SignInState")
    val signInState: SignInState = SignInState.NotStarted(),
    @Contextual
    @SerialName("SignInChallengeState")
    val signInChallengeState: SignInChallengeState? = null,
    @Contextual
    val signedInData: SignedInData? = null,
    @Contextual
    val signedOutData: SignedOutData? = null,
    @Contextual
    val signUpData: SignUpData? = null,
    @Serializable(with = AuthSignUpResultSerializer::class)
    val signUpResult: AuthSignUpResult? = null,
    @Contextual
    val authChallenge: AuthChallenge? = null,
    @Contextual
    val amplifyCredential: AmplifyCredential? = null,
    @Contextual
    val signInMethod: SignInMethod? = null
) {

    internal fun <T> toRealAuthState(): T = when (type) {
        "AuthState.Configured" -> AuthState.Configured(authNState, authZState, signUpState) as T
        "AuthenticationState.SignedOut" -> signedOutData?.let { AuthenticationState.SignedOut(it) } as T
        "AuthenticationState.SignedIn" -> signedInData?.let {
            AuthenticationState.SignedIn(it, DeviceMetadata.Empty)
        } as T
        "AuthenticationState.SigningIn" -> AuthenticationState.SigningIn(signInState) as T
        "AuthorizationState.Configured" -> AuthorizationState.Configured() as T
        "AuthorizationState.SessionEstablished" -> amplifyCredential?.let {
            AuthorizationState.SessionEstablished(it)
        } as T
        "SignUpState.NotStarted" -> SignUpState.NotStarted("") as T
        "SignUpState.InitiatingSignUp" -> signUpData?.let { SignUpState.InitiatingSignUp(it) } as T
        "SignUpState.ConfirmingSignUp" -> signUpData?.let { SignUpState.ConfirmingSignUp(it) } as T
        "SignUpState.AwaitingUserConfirmation" -> {
            signUpData?.let { data ->
                signUpResult?.let { result ->
                    SignUpState.AwaitingUserConfirmation(data, result)
                }
            } as T
        }
        "SignUpState.SignedUp" -> {
            signUpData?.let { data ->
                signUpResult?.let { result ->
                    SignUpState.SignedUp(data, result)
                }
            } as T
        }
        "AuthorizationState.SigningIn" -> AuthorizationState.SigningIn() as T
        "SignInState.ResolvingChallenge" -> SignInState.ResolvingChallenge(signInChallengeState) as T
        "SignInChallengeState.WaitingForAnswer" -> authChallenge?.let {
            SignInChallengeState.WaitingForAnswer(it, signInMethod!!)
        } as T
        else -> {
            error("Cannot get real type!")
        }
    }

    companion object {
        fun <T> toProxy(authState: T): AuthStatesProxy = when (authState) {
            is AuthState -> {
                when (authState) {
                    is AuthState.Configured -> AuthStatesProxy(
                        type = "AuthState.Configured",
                        authNState = authState.authNState,
                        authZState = authState.authZState,
                        signUpState = authState.authSignUpState
                    )
                    is AuthState.ConfiguringAuth -> TODO()
                    is AuthState.ConfiguringAuthentication -> TODO()
                    is AuthState.ConfiguringAuthorization -> TODO()
                    is AuthState.Error -> TODO()
                    is AuthState.NotConfigured -> TODO()
                }
            }
            is AuthenticationState -> {
                when (authState) {
                    is AuthenticationState.Configured -> TODO()
                    is AuthenticationState.Error -> TODO()
                    is AuthenticationState.NotConfigured -> TODO()
                    is AuthenticationState.SignedIn -> AuthStatesProxy(
                        type = "AuthenticationState.SignedIn",
                        signedInData = authState.signedInData
                    )
                    is AuthenticationState.SignedOut -> AuthStatesProxy(
                        type = "AuthenticationState.SignedOut",
                        signedOutData = authState.signedOutData
                    )
                    is AuthenticationState.SigningIn -> AuthStatesProxy(
                        type = "AuthenticationState.SigningIn",
                        signInState = authState.signInState
                    )
                    is AuthenticationState.SigningOut -> TODO()
                    is AuthenticationState.FederatingToIdentityPool -> TODO()
                    is AuthenticationState.FederatedToIdentityPool -> TODO()
                }
            }
            is AuthorizationState -> {
                when (authState) {
                    is AuthorizationState.Configured -> AuthStatesProxy(
                        type = "AuthorizationState.Configured"
                    )
                    is AuthorizationState.DeletingUser -> AuthStatesProxy(
                        type = "AuthorizationState.DeletingUser"
                    )
                    is AuthorizationState.Error -> TODO()
                    is AuthorizationState.FetchingAuthSession -> TODO()
                    is AuthorizationState.FetchingUnAuthSession -> TODO()
                    is AuthorizationState.RefreshingSession -> TODO()
                    is AuthorizationState.NotConfigured -> TODO()
                    is AuthorizationState.SessionEstablished -> AuthStatesProxy(
                        type = "AuthorizationState.SessionEstablished",
                        amplifyCredential = authState.amplifyCredential
                    )
                    is AuthorizationState.SigningIn -> AuthStatesProxy(
                        type = "AuthorizationState.SigningIn"
                    )
                    is AuthorizationState.SigningOut -> TODO()
                    is AuthorizationState.StoringCredentials -> TODO()
                    is AuthorizationState.FederatingToIdentityPool -> TODO()
                }
            }
            is SignInState -> {
                when (authState) {
                    is SignInState.NotStarted -> TODO()
                    is SignInState.ConfirmingDevice -> TODO()
                    is SignInState.Done -> TODO()
                    is SignInState.Error -> TODO()
                    is SignInState.ResolvingChallenge -> AuthStatesProxy(
                        type = "SignInState.ResolvingChallenge",
                        signInChallengeState = authState.challengeState
                    )
                    is SignInState.ResolvingDeviceSRP -> TODO()
                    is SignInState.SignedIn -> TODO()
                    is SignInState.SigningInViaMigrateAuth -> TODO()
                    is SignInState.SigningInWithCustom -> TODO()
                    is SignInState.SigningInWithHostedUI -> TODO()
                    is SignInState.SigningInWithSRP -> TODO()
                    is SignInState.SigningInWithSRPCustom -> TODO()
                    is SignInState.ResolvingTOTPSetup -> TODO()
                    is SignInState.SigningInWithUserAuth -> TODO()
                    is SignInState.SigningInWithWebAuthn -> TODO()
                    is SignInState.AutoSigningIn -> TODO()
                }
            }
            is SignInChallengeState -> {
                when (authState) {
                    is SignInChallengeState.NotStarted -> TODO()
                    is SignInChallengeState.Verified -> TODO()
                    is SignInChallengeState.Verifying -> TODO()
                    is SignInChallengeState.WaitingForAnswer -> AuthStatesProxy(
                        type = "SignInChallengeState.WaitingForAnswer",
                        authChallenge = authState.challenge,
                        signInMethod = authState.signInMethod
                    )
                    is SignInChallengeState.Error -> TODO()
                }
            }
            is SignUpState -> {
                when (authState) {
                    is SignUpState.NotStarted -> AuthStatesProxy(
                        type = "SignUpState.NotStarted"
                    )
                    is SignUpState.AwaitingUserConfirmation -> AuthStatesProxy(
                        type = "SignUpState.AwaitingUserConfirmation",
                        signUpData = authState.signUpData,
                        signUpResult = authState.signUpResult
                    )
                    is SignUpState.ConfirmingSignUp -> AuthStatesProxy(
                        type = "SignUpState.ConfirmingSignUp",
                        signUpData = authState.signUpData
                    )
                    is SignUpState.Error -> TODO()
                    is SignUpState.InitiatingSignUp -> AuthStatesProxy(
                        type = "SignUpState.InitiatingSignUp",
                        signUpData = authState.signUpData
                    )
                    is SignUpState.SignedUp -> AuthStatesProxy(
                        type = "SignUpState.SignedUp",
                        signUpData = authState.signUpData,
                        signUpResult = authState.signUpResult
                    )
                }
            }
            else -> {
                error(" Cannot convert to proxy!")
            }
        }

        val format = Json {
            serializersModule = SerializersModule {
                contextual(object : KSerializer<AuthState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthState.Configured> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthenticationState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthenticationState.SignedIn> by AuthStatesSerializer() {})
                contextual(object : KSerializer<SignInState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<SignInChallengeState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthorizationState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthorizationState.SessionEstablished> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthenticationState.SignedOut> by AuthStatesSerializer() {})
                contextual(object : KSerializer<SignUpState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<SignUpState.InitiatingSignUp> by AuthStatesSerializer() {})
                contextual(object : KSerializer<SignUpState.ConfirmingSignUp> by AuthStatesSerializer() {})
                contextual(object : KSerializer<SignUpState.AwaitingUserConfirmation> by AuthStatesSerializer() {})
                contextual(object : KSerializer<SignUpState.SignedUp> by AuthStatesSerializer() {})
            }
            prettyPrint = true
        }
    }
}

internal fun AuthState.serialize(): String = format.encodeToString(this)
internal fun String.deserializeToAuthState(): AuthState = format.decodeFromString(this)

private class AuthStatesSerializer<T> : KSerializer<T> {
    val serializer = AuthStatesProxy.serializer()

    override fun deserialize(decoder: Decoder): T = decoder.decodeSerializableValue(serializer).toRealAuthState()

    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(serializer, AuthStatesProxy.toProxy(value))
    }
}
