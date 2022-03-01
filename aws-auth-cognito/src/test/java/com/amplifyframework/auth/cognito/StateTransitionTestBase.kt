package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.cognito.data.*
import com.amplifyframework.auth.cognito.events.*
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.codegen.actions.*
import org.mockito.Mock
import org.mockito.Mockito

open class StateTransitionTestBase {

    internal object MockitoHelper {
        fun <T> anyObject(): T {
            Mockito.any<T>()
            return uninitialized()
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> uninitialized(): T = null as T
    }

    @Mock
    internal lateinit var signedInData: SignedInData

    @Mock
    internal lateinit var credentials: AmplifyCredential

    @Mock
    internal lateinit var configuration: AuthConfiguration

    @Mock
    internal lateinit var mockAuthActions: AuthActions

    @Mock
    internal lateinit var mockAuthenticationActions: AuthenticationActions

    @Mock
    internal lateinit var mockAuthorizationActions: AuthorizationActions

    @Mock
    internal lateinit var mockSignUpActions: SignUpActions

    @Mock
    internal lateinit var mockSRPActions: SRPActions

    @Mock
    internal lateinit var mockSignOutActions: SignOutActions

    internal fun setupAuthActions() {
        Mockito.`when`(mockAuthActions.initializeAuthConfigurationAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            AuthEvent(
                                AuthEvent.EventType.ConfigureAuthentication(
                                    configuration,
                                    credentials
                                )
                            )
                        )
                    }
                }
            )

        Mockito.`when`(
            mockAuthActions.initializeAuthenticationConfigurationAction(
                MockitoHelper.anyObject()
            )
        )
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            AuthenticationEvent(
                                AuthenticationEvent.EventType.Configure(
                                    configuration,
                                    credentials
                                )
                            )
                        )
                    }
                }
            )

        Mockito.`when`(
            mockAuthActions.initializeAuthorizationConfigurationAction(
                MockitoHelper.anyObject()
            )
        )
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            AuthorizationEvent(AuthorizationEvent.EventType.Configure(configuration))
                        )
                    }
                }
            )
    }

    internal fun setupAuthNActions() {
        Mockito.`when`(mockAuthenticationActions.configureAuthenticationAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            AuthenticationEvent(
                                AuthenticationEvent.EventType.InitializedSignedOut(SignedOutData())
                            )
                        )
                        dispatcher.send(
                            AuthEvent(
                                AuthEvent.EventType.ConfiguredAuthentication(configuration)
                            )
                        )
                    }
                })

        Mockito.`when`(mockAuthenticationActions.initiateSRPSignInAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            SRPEvent(
                                SRPEvent.EventType.InitiateSRP(
                                    "username",
                                    "password"
                                )
                            )
                        )
                    }
                })

        Mockito.`when`(mockAuthenticationActions.initiateSignUpAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            SignUpEvent(
                                SignUpEvent.EventType.InitiateSignUp(
                                    "username",
                                    "password"
                                )
                            )
                        )
                    }
                })

        Mockito.`when`(
            mockAuthenticationActions.initiateSignOutAction(
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject()
            )
        ).thenReturn(
            object : Action {
                override suspend fun execute(
                    dispatcher: EventDispatcher,
                    environment: Environment
                ) {
                    dispatcher.send(
                        SignOutEvent(SignOutEvent.EventType.SignOutGlobally(signedInData))
                    )
                }
            })
    }

    internal fun setupAuthZActions() {
        Mockito.`when`(mockAuthorizationActions.configureAuthorizationAction())
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(AuthEvent(AuthEvent.EventType.ConfiguredAuthorization))
                    }
                })
    }

    internal fun setupSRPActions() {
        Mockito.`when`(mockSRPActions.initiateSRPAuthAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            SRPEvent(SRPEvent.EventType.RespondPasswordVerifier(mapOf()))
                        )
                    }
                })

        Mockito.`when`(mockSRPActions.verifyPasswordSRPAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            SRPEvent(SRPEvent.EventType.FinalizeSRPSignIn(mapOf()))
                        )
                        dispatcher.send(
                            AuthenticationEvent(
                                AuthenticationEvent.EventType.InitializedSignedIn(signedInData)
                            )
                        )
                    }
                })
    }

    internal fun setupSignOutActions() {
        Mockito.`when`(mockSignOutActions.localSignOutAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            SignOutEvent(
                                SignOutEvent.EventType.SignedOutSuccess(
                                    signedInData
                                )
                            )
                        )
                        dispatcher.send(
                            AuthenticationEvent(
                                AuthenticationEvent.EventType.InitializedSignedOut(
                                    SignedOutData(signedInData.username)
                                )
                            )
                        )
                    }
                })

        Mockito.`when`(mockSignOutActions.globalSignOutAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            SignOutEvent(SignOutEvent.EventType.RevokeToken(signedInData))
                        )
                    }
                })

        Mockito.`when`(mockSignOutActions.revokeTokenAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
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
                })
    }

    internal fun setupSignUpActions() {
        Mockito.`when`(mockSignUpActions.startSignUpAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        dispatcher.send(
                            SignUpEvent(
                                SignUpEvent.EventType.InitiateSignUpSuccess("username", null)
                            )
                        )
                    }
                })

//        Mockito.`when`(mockSignUpActions.confirmSignUpAction(MockitoHelper.anyObject()))
//            .thenReturn(
//                object : Action {
//                    override suspend fun execute(
//                        dispatcher: EventDispatcher,
//                        environment: Environment
//                    ) {
//                        dispatcher.send(
//                            SignUpEvent(SignUpEvent.EventType.ConfirmSignUpSuccess(null))
//                        )
//                    }
//                })
    }
}