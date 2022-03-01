package com.amplifyframework.auth.cognito.actions

import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.data.AuthenticationError
import com.amplifyframework.auth.cognito.data.SignInMethod
import com.amplifyframework.auth.cognito.data.SignedInData
import com.amplifyframework.auth.cognito.data.SignedOutData
import com.amplifyframework.auth.cognito.events.*
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.codegen.actions.AuthenticationActions
import java.util.*

object AuthenticationCognitoActions : AuthenticationActions {
    override fun configureAuthenticationAction(event: AuthenticationEvent.EventType.Configure) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                val userPoolTokens = event.storedCredentials?.cognitoUserPoolTokens
                val authenticationEvent = userPoolTokens?.let {
                    val signedInData = SignedInData("", "", Date(), SignInMethod.SRP, it)
                    AuthenticationEvent(
                        AuthenticationEvent.EventType.InitializedSignedIn(signedInData)
                    )
                } ?: AuthenticationEvent(
                    AuthenticationEvent.EventType.InitializedSignedOut(SignedOutData())
                )
                dispatcher.send(authenticationEvent)
                dispatcher.send(AuthEvent(AuthEvent.EventType.ConfiguredAuthentication(event.configuration)))
            }
        }

    override fun initiateSRPSignInAction(event: AuthenticationEvent.EventType.SignInRequested) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                with(event) {
                    val srpEvent = username?.run {
                        password?.run {
                            SRPEvent(SRPEvent.EventType.InitiateSRP(username, password))
                        }
                    } ?: SRPEvent(SRPEvent.EventType.ThrowAuthError(AuthenticationError("")))
                    dispatcher.send(srpEvent)
                }
            }
        }

    override fun initiateSignOutAction(
        event: AuthenticationEvent.EventType.SignOutRequested,
        signedInData: SignedInData
    ) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                if (event.isGlobalSignOut) {
                    dispatcher.send(
                        SignOutEvent(SignOutEvent.EventType.SignOutGlobally(signedInData))
                    )
                } else {
                    dispatcher.send(
                        SignOutEvent(
                            SignOutEvent.EventType.SignOutLocally(
                                signedInData,
                                isGlobalSignOut = false,
                                invalidateTokens = false
                            )
                        )
                    )
                }
            }
        }

    override fun initiateSignUpAction(event: AuthenticationEvent.EventType.SignUpRequested) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                with(event) {
                    val signUpEvent = username?.run {
                        password?.run {
                            SignUpEvent(SignUpEvent.EventType.InitiateSignUp(username, password))
                        }
                    } ?: SignUpEvent(
                        SignUpEvent.EventType.InitiateSignUpFailure(AuthenticationError(""))
                    )
                    dispatcher.send(signUpEvent)
                }
            }
        }
}