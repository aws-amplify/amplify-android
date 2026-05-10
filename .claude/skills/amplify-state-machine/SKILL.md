---
name: amplify-state-machine
description: Add or modify auth state machine pieces — states, events, actions, resolvers — in aws-auth-cognito. Use when introducing a new auth flow (e.g. a new sign-in factor), fixing a missing state transition, or wiring a new side effect to a state change.
---

# Working with the auth state machine

`aws-auth-cognito` models authentication as a hierarchy of finite-state machines. The pattern lives at `aws-auth-cognito/src/main/java/com/amplifyframework/statemachine/`. Read `.claude/rules.md §10` first.

## The pieces

```
statemachine/
├── StateMachine.kt              the FSM engine (open class<S, E>)
├── StateMachineResolver.kt      (oldState, event) → StateResolution(newState, [actions])
├── State.kt                     marker interface — immutable, value-semantics
├── Event.kt / StateMachineEvent
├── Action.kt                    suspend fn side effect
├── Environment.kt               non-state context (logger, store, SDK clients)
├── EffectExecutor.kt            runs actions (default ConcurrentEffectExecutor)
└── codegen/
    ├── states/                  AuthenticationState, AuthorizationState, SignInState, SignOutState, …
    ├── events/                  AuthenticationEvent, SignInEvent, SignOutEvent, …
    ├── actions/                 (mostly `aws-auth-cognito/.../actions/`)
    └── data/                    SignInData, AuthChallenge, …
```

`AuthState` is the top-level state and contains nested machines: `authNState` (AuthenticationState), `authZState` (AuthorizationState), `signInState`, `signOutState`, etc.

## Important threading invariants

- All transitions run on a **single-thread** coroutine context (`newSingleThreadContext("StateMachineContext")`). Resolvers are pure — they MUST NOT do IO.
- State is exposed as a `MutableSharedFlow(replay = 1)` — values are NOT conflated. Every emitted state is delivered. (Don't change this to `StateFlow`.)
- Actions run on the executor's dispatcher (`Dispatchers.Default` by default). They CAN do IO; they're the place to do it.

## When you need to ...

### Add a new state

1. Edit the relevant sealed class under `codegen/states/` (e.g. `SignInState`). Add a `data class` or `object` subtype. Keep it immutable.
2. Add a transition into it from the resolver: open the matching `*Resolver` in `aws-auth-cognito/.../statemachine/codegen/actions/...` or `aws-auth-cognito/.../auth/cognito/statemachine/...`. Add the `(oldState, event) → StateResolution(newState, actions)` branch.
3. If the new state is terminal for a use case, update the use case's `state.mapNotNull { … }.first()` to recognize it.
4. If callers can subscribe to the state externally (rare), document the new variant in the state's KDoc.
5. Test the resolver's new branch in isolation — resolvers are pure functions, so this is the easiest test.

### Add a new event

1. Open the matching `*Event` sealed class under `codegen/events/`. Add a `data class` to the `EventType` sealed inner class.
2. Pick the resolver(s) that should react. Add a branch in `resolve(...)` for the new event.
3. Wire dispatching into the use case (or action) that should fire it. Use the existing `sendEventAndGet*` helper if a synchronous-style result is wanted.

### Add a new action (side effect)

1. Add a function in the `*CognitoActions` (or equivalent) interface under `auth/cognito/actions/` — actions are usually grouped by machine.
2. Implement it in the matching `*CognitoActionsImpl`. Use `suspend fun` semantics; do all your IO here.
3. Return the action from the resolver branch that needs the side effect: `StateResolution(newState, listOf(action))`.
4. Don't emit Hub events from an action — that's the use case's job.

### Fix a missing transition

The frequent shape: a state arrives that the current resolver doesn't handle, and the machine sits there. Symptom is a use case `state.mapNotNull { … }.first()` that hangs.

1. Reproduce with a unit test on the resolver alone — supply the exact `(state, event)` pair, assert the result.
2. Add the missing branch. Be conservative — pick the smallest legal transition and let later events drive further progress.
3. Reference the upstream repo for similar fixes: `git log release_v2.30.x..release_v2.36.x -- aws-auth-cognito/src/main/java/com/amplifyframework/statemachine` is full of "Add missing state transitions for X" commits.

### Change which dispatcher actions run on

You almost certainly don't want to. The default `ConcurrentEffectExecutor(Dispatchers.Default)` is correct. Override only at the `StateMachine` constructor — never globally.

## Test patterns

- Resolver tests are the cheapest. Plain unit tests, no coroutines: `assertEquals(expectedResolution, resolver.resolve(state, event))`.
- StateMachine tests: drive a `MutableSharedFlow<State>` for `stateMachine.state`, send events, assert with Turbine.
- Action tests: MockK the `Environment`, call the suspend fn, verify SDK / Hub interactions.
- Use the existing `StateMachineForAuthTests.kt` and `StateMachineForAuthListenerTests.kt` (renamed on this fork) as references.

## Anti-patterns

- Doing IO in a resolver. Resolvers are pure. IO goes in actions.
- Mutating a `State` instance. States are immutable; create a new one.
- Catching exceptions in a resolver to "recover". Errors flow through an `Error` state.
- Adding a `StateFlow` to "see all states" externally. The `SharedFlow(replay=1)` exists specifically because conflation breaks the FSM contract.
- Emitting Hub events from an action. Use cases publish.
- Reading `getCurrentState()` to make a transition decision. Race-prone. Use the resolver.
