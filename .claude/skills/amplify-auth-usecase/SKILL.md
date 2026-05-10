---
name: amplify-auth-usecase
description: Add or modify a Cognito auth use case (signIn/signOut/fetchAuthSession/deleteUser/etc.) in aws-auth-cognito. Use when adding a new public auth API, refactoring code out of RealAWSCognitoAuthPlugin into a use case, or threading multi-user userId through an existing use case.
---

# Adding or modifying a Cognito auth use case

This is the dominant pattern in the upstream library between v2.26 and v2.36 — most public auth API surface has been (or is being) extracted out of `RealAWSCognitoAuthPlugin` into use cases. New auth work goes in a use case.

Read `.claude/rules.md §11` first if you haven't.

## Where things live

```
aws-auth-cognito/src/main/java/com/amplifyframework/auth/cognito/
├── AWSCognitoAuthPlugin.kt           thin public facade (Plugin<AWSCognitoAuthService>)
├── RealAWSCognitoAuthPlugin.kt       the (shrinking) coordinator — delegate to use cases
├── usecases/                         <-- use cases live here, one file per public API
│   ├── SignInUseCase.kt
│   ├── SignOutUseCase.kt
│   ├── FetchAuthSessionUseCase.kt
│   ├── DeleteUserUseCase.kt
│   └── …
└── statemachine/                     state machine, resolvers, actions, events, states
```

Tests: `aws-auth-cognito/src/test/java/com/amplifyframework/auth/cognito/usecases/<UseCase>Test.kt`.

## Anatomy of a use case

Look at `SignInUseCase` (release_v2.36.0) as the canonical reference:

```kotlin
internal class SignInUseCase(
    private val stateMachine: AuthStateMachine,
    private val configuration: AuthConfiguration,
    private val hubEmitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(
        username: String?,
        password: String?,
        options: AuthSignInOptions = AuthSignInOptions.defaults()
    ): AuthSignInResult {
        val signInData = getSignInData(username, password, options)
        waitForStateThatAllowsSignIn()                                      // 1
        val event = AuthenticationEvent(EventType.SignInRequested(signInData))
        val result = stateMachine.sendEventAndGetSignInResult(event)        // 2
        if (result.isSignedIn) {
            hubEmitter.sendHubEvent(AuthChannelEventName.SIGNED_IN.toString())   // 3
        }
        return result                                                       // 4
    }

    private suspend fun waitForStateThatAllowsSignIn(): AuthState =
        stateMachine.state.mapNotNull { authState ->
            when (authState.authNState) {
                is NotConfigured  -> throw InvalidUserPoolConfigurationException()
                is SignedOut, is Configured -> authState
                is SignedIn       -> throw SignedInException()
                is SigningOut     -> null                                   // wait
                is SigningIn      -> { stateMachine.send(CancelSignIn()); null }
                is Error          -> throw it.exception.toAuthException(...)
                else              -> throw InvalidStateException()
            }
        }.first()
}
```

Four required steps in order:

1. **Validate preconditions** by collecting `stateMachine.state.mapNotNull { … }.first()`. Branch on every state: throw a typed `AuthException` for impossible states, return `null` (continue) for transient states, return the state for valid ones.
2. **Dispatch the event** and **collect the terminal state**. Use the existing extension helpers (`sendEventAndGetSignInResult`, `sendEventAndGetAuthSession`, …) under `auth/cognito/util/` whenever one already exists for your flow.
3. **Emit a Hub event on success only.** Use `AuthHubEventEmitter` and the `AuthChannelEventName` enum — never a string literal.
4. **Return a typed result** (`AuthSignInResult`, `AuthSignOutResult`, `AuthSession`, …). Never `Unit` if the caller could care.

## Steps to add a new use case

1. **Decide if it's really new.** If the API already exists in `RealAWSCognitoAuthPlugin`, you're refactoring (next section), not adding.
2. **Pick the result type.** Reuse an existing `AuthXxxResult` if possible. Add a new typed result class only if no existing one fits.
3. **Pick the event(s).** Find or add an `AuthenticationEvent.EventType.*` / `AuthorizationEvent.EventType.*` / `SignInEvent.EventType.*`. If you add an event, the `amplify-state-machine` skill covers that flow.
4. **Create `usecases/<Name>UseCase.kt`** as `internal class`, constructor-inject `AuthStateMachine`, `AuthConfiguration`, `AuthHubEventEmitter` (default-init), and any other env you need.
5. **Write `suspend fun execute(...)` following the four steps above.**
6. **Wire it into `RealAWSCognitoAuthPlugin`.** Add a property `private val xxxUseCase by lazy { XxxUseCase(authStateMachine, configuration) }` and have the public method just call `coroutineScope.launch { … xxxUseCase.execute(...) }` translating to the Java callback.
7. **(Fork-specific) Multi-user.** If the use case touches the credential store, accept `userId: String?` in the constructor, forward it to the credential store / fetch operations, and add a `userId`-accepting overload on `AWSCognitoAuthPlugin`. Cross-reference existing patterns in `SignOutUseCase`, `FetchAuthSessionUseCase`, `ClearFederationToIdentityPoolUseCase` on this branch.
8. **Test.** Mirror an existing test file (`SignInUseCaseTest.kt` is a good one). Use `runTest { }`, `MutableStateFlow<AuthState>` to drive `stateMachine.state`, MockK for collaborators, Kotest assertions.
9. **`./gradlew :aws-auth-cognito:test ktlintFormat apiCheck`** before review. If `apiCheck` complains, run `apiDump` and inspect the diff — public surface changes need explicit sign-off.

## Steps to refactor a method out of `RealAWSCognitoAuthPlugin`

1. Find every code path the method takes in `RealAWSCognitoAuthPlugin` (it's usually a tangle of state listeners and callbacks).
2. Move the logic into an `internal class XxxUseCase` under `usecases/`. Convert listener-based waiting into `state.mapNotNull { … }.first()`.
3. Replace the body of the original method with `coroutineScope.launch { … xxxUseCase.execute(...).also(onSuccess) }` — preserve the public Java signature exactly.
4. Move the unit tests for that path into `<XxxUseCase>Test.kt`. Delete the duplicated assertions from `RealAWSCognitoAuthPluginTest`.
5. Run `apiCheck` — refactor must NOT change public API.

## Anti-patterns (block at review)

- A use case that throws `RuntimeException`. Always typed `AuthException`.
- Direct `try/catch` around `AwsServiceException` in the use case. Use `toAuthException(...)`.
- Reading state via `getCurrentState()` inside the use case (snapshot can race the dispatch). Use `state.mapNotNull { … }.first()`.
- Emitting Hub events from inside an `Action`. Use cases own emission.
- A public `class XxxUseCase`. Always `internal`.
- Adding a `Consumer<T>` callback parameter. Use cases are `suspend fun`.
