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

package com.amplifyframework.testutils.featuretest.auth.serializers

import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.testutils.featuretest.auth.serializers.AuthStatesProxy.Companion.format
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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
    @Contextual @SerialName("AuthenticationState")
    val authNState: AuthenticationState? = null,
    @Contextual @SerialName("AuthorizationState")
    val authZState: AuthorizationState? = null,
    @Contextual
    val signedInData: SignedInData? = null,
    @Contextual
    val amplifyCredential: AmplifyCredential? = null
) {

    internal fun <T> toRealAuthState(): T {
        return when (type) {
            "AuthState.Configured" -> AuthState.Configured(authNState, authZState) as T
            "AuthenticationState.SignedIn" -> signedInData?.let { AuthenticationState.SignedIn(it) } as T
            "AuthorizationState.SessionEstablished" -> amplifyCredential?.let {
                AuthorizationState.SessionEstablished(it)
            } as T
            else -> {
                error("Cannot get real type!")
            }
        }
    }

    companion object {
        fun <T> toProxy(authState: T): AuthStatesProxy {
            return when (authState) {
                is AuthState -> {
                    when (authState) {
                        is AuthState.Configured -> AuthStatesProxy(
                            type = "AuthState.Configured",
                            authNState = authState.authNState,
                            authZState = authState.authZState
                        )
                        is AuthState.ConfiguringAuth -> TODO()
                        is AuthState.ConfiguringAuthentication -> TODO()
                        is AuthState.ConfiguringAuthorization -> TODO()
                        is AuthState.Error -> TODO()
                        is AuthState.NotConfigured -> TODO()
                        is AuthState.ValidatingCredentialsAndConfiguration -> TODO()
                        is AuthState.WaitingForCachedCredentials -> TODO()
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
                        is AuthenticationState.SignedOut -> TODO()
                        is AuthenticationState.SigningIn -> TODO()
                        is AuthenticationState.SigningOut -> TODO()
                    }
                }
                is AuthorizationState -> {
                    when (authState) {
                        is AuthorizationState.Configured -> TODO()
                        is AuthorizationState.DeletingUser -> TODO()
                        is AuthorizationState.Error -> TODO()
                        is AuthorizationState.FetchingAuthSession -> TODO()
                        is AuthorizationState.NotConfigured -> TODO()
                        is AuthorizationState.SessionEstablished -> AuthStatesProxy(
                            type = "AuthorizationState.SessionEstablished",
                            amplifyCredential = authState.amplifyCredential
                        )
                        is AuthorizationState.SigningIn -> TODO()
                        is AuthorizationState.SigningOut -> TODO()
                        is AuthorizationState.WaitingToStore -> TODO()
                    }
                }
                else -> {
                    error(" Cannot convert to proxy!")
                }
            }
        }

        val format = Json {
            serializersModule = SerializersModule {
                contextual(object : KSerializer<AuthState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthState.Configured> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthenticationState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthenticationState.SignedIn> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthorizationState> by AuthStatesSerializer() {})
                contextual(object : KSerializer<AuthorizationState.SessionEstablished> by AuthStatesSerializer() {})
            }
            prettyPrint = true
        }
    }
}

fun AuthState.serialize(): String = format.encodeToString(this)
fun String.deserializeToAuthState(): AuthState = format.decodeFromString(this)

private class AuthStatesSerializer<T> : KSerializer<T> {
    val serializer = AuthStatesProxy.serializer()

    override fun deserialize(decoder: Decoder): T {
        return decoder.decodeSerializableValue(serializer).toRealAuthState()
    }

    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(serializer, AuthStatesProxy.toProxy(value))
    }
}
