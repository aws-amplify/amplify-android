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

package com.amplifyframework.statemachine

import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.codegen.data.AuthStateRepo
import com.amplifyframework.statemachine.codegen.data.isSessionEstablished
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

/**
 * Multi-user-aware [AuthState] state machine.
 *
 * Mirrors the contract of [StateMachine] but maintains per-user state through [AuthStateRepo]:
 *
 *  - [send] without `userId` routes the event to the most recently active user (LIFO peek);
 *    [send] with `userId` routes to that specific user's state.
 *  - [_state] is a non-conflated [MutableSharedFlow] (replay=1) — every state transition is delivered
 *    to subscribers. When a state reaches `SessionEstablished`, the established state is emitted
 *    first (so use cases observe the terminal transition), then the flow is reset to a default
 *    configured state so a *second* user can sign in on top of the first. The first user's terminal
 *    state lives in [AuthStateRepo] for retrieval via [getStateForUser].
 *
 * This class is fork-only — it has no upstream counterpart. [com.amplifyframework.auth.cognito.AuthStateMachine]
 * extends it instead of [StateMachine].
 *
 * @param resolver mutates state in response to events.
 * @param environment auth environment (provides context for [AuthStateRepo]).
 * @param dispatcherQueue dispatcher actions run on (default [Dispatchers.Default]).
 * @param executor side-effect executor (default [ConcurrentEffectExecutor]).
 * @param initialState starting state (defaults to the resolver's default state).
 */
internal open class StateMachineForAuth(
    resolver: StateMachineResolver<AuthState>,
    val environment: AuthEnvironment,
    private val dispatcherQueue: CoroutineDispatcher = Dispatchers.Default,
    private val executor: EffectExecutor = ConcurrentEffectExecutor(dispatcherQueue),
    initialState: AuthState? = null
) : EventDispatcher {

    private val resolver = resolver.eraseToAnyResolver()

    private val authStateRepo: AuthStateRepo = AuthStateRepo.getInstance(environment.context)

    /**
     * The current state of the state machine. Non-conflated [MutableSharedFlow] so all transitions
     * are delivered (matches upstream `StateMachine`'s SharedFlow choice).
     */
    private val _state = MutableSharedFlow<AuthState>(
        replay = 1,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        tryEmit(initialState ?: resolver.defaultState)
    }

    val state = _state.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val stateMachineContext = SupervisorJob() + newSingleThreadContext("StateMachineContext")
    private val stateMachineScope = CoroutineScope(stateMachineContext)

    /**
     * Returns the auth state for [userId]. When [userId] is null/empty or [ignoreUserId] is true,
     * falls back to the global [_state]. Otherwise reads from [AuthStateRepo], or the repo's default
     * configured state when no entry exists.
     */
    private suspend fun getAuthStateForUser(userId: String?, ignoreUserId: Boolean = false): AuthState =
        if (userId.isNullOrEmpty() || ignoreUserId) {
            _state.first()
        } else {
            authStateRepo.get(userId) ?: authStateRepo.getDefaultConfiguredState()
        }

    /**
     * Persists the new state into the per-user repo and emits to [_state].
     *
     * When [userId] is non-empty (multi-user context), the new state is written to [AuthStateRepo].
     * Additionally, when the state is `SessionEstablished`, the established state is emitted first
     * (so subscribers observe the terminal transition via `state.first()`), then the flow is reset
     * to a default configured state so a *second* user can sign in on top of the first.
     *
     * When [userId] is empty AND the new state is a `SessionEstablished` carrying a [SignedInData],
     * the userId is recovered from `signedInData.userId`. This handles every code path where the
     * sign-in was dispatched without an explicit userId — first sign-in (no active key yet), web UI
     * redirect (OAuth flow doesn't know the userId until tokens arrive), and process-death recovery
     * (the redirect callback rebuilds `SignedInData` from the ID token). Without this fallback the
     * established state would emit to `_state` but never reach `AuthStateRepo`, leaving subsequent
     * `signOut(userId, …)` / `fetchAuthSession(userId, …)` calls with no per-user entry to find.
     *
     * When [userId] is empty AND the state has no `SignedInData` to recover from, behaviour matches
     * upstream `AuthStateMachine`: emit to `_state`, no `AuthStateRepo` write. This keeps single-user
     * code paths and existing single-user tests unchanged.
     */
    private fun setAuthState(userId: String, value: AuthState) {
        _state.tryEmit(value)
        val effectiveUserId = userId.ifEmpty { value.recoverableUserIdOrEmpty() }
        if (effectiveUserId.isNotEmpty()) {
            authStateRepo.put(effectiveUserId, value)
            if (value.isSessionEstablished) {
                _state.tryEmit(authStateRepo.getDefaultConfiguredState())
            }
        }
    }

    /**
     * Returns `signedInData.userId` when [this] is a `SessionEstablished` state, else empty.
     * Used by [setAuthState] to recover the userId for sign-ins dispatched without one.
     */
    private fun AuthState.recoverableUserIdOrEmpty(): String = if (isSessionEstablished) {
        ((this as? AuthState.Configured)?.authNState as? AuthenticationState.SignedIn)
            ?.signedInData?.userId.orEmpty()
    } else {
        ""
    }

    /**
     * Returns the most recently activated user id (LIFO peek), or null if none.
     */
    fun activeStateKey(): String? = authStateRepo.activeStateKey()

    /**
     * Returns every userId currently tracked by [AuthStateRepo] — the union of in-memory keys
     * (intermediate flows) and persisted keys (users with established sessions). Used by
     * [com.amplifyframework.auth.cognito.usecases.SignOutUseCase] to iterate every user when the
     * caller invokes a no-userId sign-out and the options opt into all-users semantics.
     */
    fun allUserIds(): Set<String> = authStateRepo.allUserIds()

    /**
     * Returns the auth state for [userId]. If [userId] is null, returns the active user's state
     * (or the global [_state] if no active user).
     */
    suspend fun getStateForUser(userId: String?): AuthState = withContext(stateMachineContext) {
        getAuthStateForUser(userId)
    }

    /**
     * Returns the current state. Prefers the active user's persisted state from [AuthStateRepo];
     * falls back to the global [_state] when no active user is set.
     */
    suspend fun getCurrentState(): AuthState = withContext(stateMachineContext) {
        authStateRepo.activeState() ?: _state.first()
    }

    /**
     * Dispatches [event] for resolution against the most recently active user's state. Subclasses
     * (e.g. `AuthStateMachine`) inherit this behaviour from [StateMachine]'s contract, but routed
     * per user.
     */
    override fun send(event: StateMachineEvent) {
        stateMachineScope.launch {
            process(activeStateKey().orEmpty(), event)
        }
    }

    /**
     * Dispatches [event] for resolution against the state of [userId]. When [ignoreUserId] is true,
     * resolves against the global [_state] regardless of [userId].
     */
    fun send(event: StateMachineEvent, userId: String, ignoreUserId: Boolean = false) {
        stateMachineScope.launch {
            process(userId, event, ignoreUserId)
        }
    }

    private suspend fun process(userId: String, event: StateMachineEvent, ignoreUserId: Boolean = false) {
        val currentState = getAuthStateForUser(userId, ignoreUserId)
        val resolution = resolver.resolve(currentState, event)
        if (currentState != resolution.newState) {
            setAuthState(userId, resolution.newState)
        }
        executor.execute(resolution.actions, this, environment)
    }
}
