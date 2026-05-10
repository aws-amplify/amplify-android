/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.statemachine.codegen.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.amplifyframework.core.store.EncryptedKeyValueRepository
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import com.amplifyframework.statemachine.util.LifoMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for managing per-user authentication states.
 *
 * Holds the most recently active user's state in an in-memory [LifoMap]; persists fully-established
 * sessions ([AuthState.Configured] with [AuthenticationState.SignedIn] + [AuthorizationState.SessionEstablished])
 * to encrypted key-value storage so they survive process death. Intermediate states (signing in,
 * resolving challenges, etc.) live in-memory only.
 *
 * Three rules govern [put]:
 *  - [AuthState.isSignedOut] → remove from both stores.
 *  - [AuthState.isSessionEstablished] → persist to encrypted store and clear in-memory map so a
 *    fresh login can stack on top.
 *  - Otherwise → keep in-memory only.
 *
 * @param context Application context used to back the encrypted key-value store.
 */
internal class AuthStateRepo private constructor(
    private val encryptedStoreFactory: () -> KeyValueRepository
) {

    private val authStateMap = LifoMap.empty<String, AuthState>()

    // Lazy so that simply constructing the repo doesn't immediately touch the keystore /
    // encrypted shared preferences. Tests that mock Context can construct AuthStateRepo
    // without configuring keystore access; only callers that actually persist or load fire it.
    private val encryptedStore by lazy { encryptedStoreFactory() }

    /**
     * Stores the given authentication state for [key] (typically the user id).
     *
     * - SignedOut → removes the entry from both in-memory and encrypted stores.
     * - SessionEstablished → persists to encrypted storage; clears the in-memory map to enable
     *   stacking another user's login on top.
     * - Intermediate states → in-memory only.
     */
    fun put(key: String, value: AuthState) {
        if (value.isSignedOut) {
            remove(key)
            return
        }
        if (value.isSessionEstablished) {
            val signedIn = value.authNState as AuthenticationState.SignedIn
            val sessionEstablished = value.authZState as AuthorizationState.SessionEstablished
            encryptedStore.put(
                key,
                serializeAuthNAndZState(
                    AuthNAndAuthZ(
                        signedInData = signedIn.signedInData,
                        deviceMetadata = signedIn.deviceMetadata,
                        amplifyCredential = sessionEstablished.amplifyCredential
                    )
                )
            )
            addToPersistedIndex(key)
            // Clear the in-memory map so a fresh login can stack on top.
            authStateMap.clear()
            return
        }
        authStateMap.push(key, value)
    }

    /**
     * Returns the auth state for [key], preferring the in-memory map over the encrypted store.
     * Returns null when neither store has an entry.
     */
    fun get(key: String): AuthState? = if (authStateMap.containsKey(key)) {
        authStateMap.get(key)
    } else {
        deserializeAuthNAndZState(encryptedStore.get(key))?.let { wrapper ->
            AuthState.Configured(
                AuthenticationState.SignedIn(wrapper.signedInData, wrapper.deviceMetadata),
                AuthorizationState.SessionEstablished(wrapper.amplifyCredential),
                null
            )
        }
    }

    /**
     * Removes the entry for [key] from both stores.
     */
    fun remove(key: String) {
        authStateMap.pop(key)
        encryptedStore.remove(key)
        removeFromPersistedIndex(key)
    }

    /**
     * Returns the most recently activated state (LIFO peek), or null if none.
     */
    fun activeState(): AuthState? = authStateMap.peek()

    /**
     * Returns the most recently activated user id (LIFO peek key), or null if none.
     * This is the canonical "default" user for no-userId calls.
     */
    fun activeStateKey(): String? = authStateMap.peekKey()

    /**
     * Snapshot of all in-memory user ids (insertion order). Used by the all-users sign-out path.
     */
    fun allInMemoryKeys(): List<String> = authStateMap.keys()

    /**
     * Returns every userId currently tracked by the repo: the union of in-memory keys
     * (intermediate flows like signing-in, MFA challenges) and persisted keys (users with an
     * established session that survived process death). Used by the all-users sign-out path.
     */
    fun allUserIds(): Set<String> = (authStateMap.keys() + loadPersistedIndex()).toSet()

    /**
     * Removes every entry from the in-memory map. Encrypted persistence is untouched —
     * callers that want to wipe persistence too must call [remove] per key.
     */
    fun clearInMemory() {
        authStateMap.clear()
    }

    /**
     * The default state used when a userId has no entry in either store. Represents a configured,
     * signed-out auth machine ready for a fresh login.
     */
    fun getDefaultConfiguredState(): AuthState = AuthState.Configured(
        authNState = AuthenticationState.SignedOut(SignedOutData()),
        authZState = AuthorizationState.Configured(),
        authSignUpState = SignUpState.NotStarted()
    )

    private fun serializeAuthNAndZState(authState: AuthNAndAuthZ): String = Json.encodeToString(authState)

    private fun deserializeAuthNAndZState(encodedState: String?): AuthNAndAuthZ? = runCatching {
        encodedState?.let { Json.decodeFromString<AuthNAndAuthZ>(it) }
    }.getOrNull()

    private fun loadPersistedIndex(): Set<String> = runCatching {
        encryptedStore.get(USER_INDEX_KEY)?.let {
            Json.decodeFromString(ListSerializer(String.serializer()), it).toSet()
        }
    }.getOrNull() ?: emptySet()

    private fun savePersistedIndex(index: Collection<String>) {
        encryptedStore.put(
            USER_INDEX_KEY,
            Json.encodeToString(ListSerializer(String.serializer()), index.toList())
        )
    }

    private fun addToPersistedIndex(userId: String) {
        val index = loadPersistedIndex()
        if (userId !in index) savePersistedIndex(index + userId)
    }

    private fun removeFromPersistedIndex(userId: String) {
        val index = loadPersistedIndex()
        if (userId in index) savePersistedIndex(index - userId)
    }

    companion object {
        private val PREF_KEY = AuthStateRepo::class.java.name

        // Reserved key inside the encrypted store: holds the JSON-encoded set of userIds whose
        // sessions have been persisted. Updated transactionally on put/remove for SessionEstablished
        // states so the all-users sign-out path can enumerate users that survived process death.
        // Underscore prefix avoids collision with userId-keyed entries.
        private const val USER_INDEX_KEY = "__amplify_auth_state_repo_user_index__"

        @Volatile
        private var instance: AuthStateRepo? = null

        /**
         * Returns the process-wide singleton, lazily initialized with the supplied context.
         * Thread-safe (double-checked locking). Matches the 2.26.14 fork's behaviour: the context
         * passed to first call is held; in production this is always the Amplify-supplied context
         * (an application context).
         */
        fun getInstance(context: Context): AuthStateRepo {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: AuthStateRepo {
                    EncryptedKeyValueRepository(context, PREF_KEY)
                }.also { instance = it }
            }
        }

        /**
         * Test seam: build an instance backed by an injected [KeyValueRepository] (typically an
         * in-memory fake) so unit tests can drive [AuthStateRepo] without keystore access.
         */
        @VisibleForTesting
        internal fun createForTest(store: KeyValueRepository): AuthStateRepo = AuthStateRepo { store }

        /**
         * Test seam: drop the singleton so the next [getInstance] call reinitializes. Lets tests
         * isolate persistence state without leaking across runs.
         */
        @VisibleForTesting
        internal fun resetInstanceForTest() {
            synchronized(this) { instance = null }
        }
    }
}

/**
 * Serializable wrapper for persisted auth state. Holds the underlying primitives ([SignedInData],
 * [DeviceMetadata], [AmplifyCredential]) rather than the [AuthState] sub-types directly, so we don't
 * need to add `@Serializable` to upstream state classes.
 */
@Serializable
private data class AuthNAndAuthZ(
    val signedInData: SignedInData,
    val deviceMetadata: DeviceMetadata,
    val amplifyCredential: AmplifyCredential
)

/**
 * True when [AuthState] represents a fully signed-in user with an established session.
 * Used by [AuthStateRepo.put] and the multi-user state-machine routing in [AuthStateMachine].
 */
internal val AuthState.isSessionEstablished: Boolean
    get() = this is AuthState.Configured &&
        this.authNState is AuthenticationState.SignedIn &&
        this.authZState is AuthorizationState.SessionEstablished

private val AuthState.isSignedOut: Boolean
    get() = this is AuthState.Configured && this.authNState is AuthenticationState.SignedOut
