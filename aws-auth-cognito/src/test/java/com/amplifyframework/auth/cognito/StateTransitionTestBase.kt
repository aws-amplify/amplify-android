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

import com.amplifyframework.auth.cognito.actions.DeleteUserActions
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthActions
import com.amplifyframework.statemachine.codegen.actions.AuthenticationActions
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions
import com.amplifyframework.statemachine.codegen.actions.FetchAuthSessionActions
import com.amplifyframework.statemachine.codegen.actions.SRPActions
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.actions.SignOutActions
import com.amplifyframework.statemachine.codegen.actions.StoreActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
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
    internal lateinit var credentialStoreActions: StoreActions

    @Mock
    internal lateinit var credentialStore: AWSCognitoAuthCredentialStore

    @Mock
    internal lateinit var legacyCredentialStore: AWSCognitoLegacyCredentialStore

    @Mock
    internal lateinit var cognitoAuthService: AWSCognitoAuthServiceBehavior

    @Mock
    internal lateinit var mockAuthActions: AuthActions

    @Mock
    internal lateinit var mockAuthenticationActions: AuthenticationActions

    @Mock
    internal lateinit var mockAuthorizationActions: AuthorizationActions

    @Mock
    internal lateinit var mockSignInActions: SignInActions

    @Mock
    internal lateinit var mockSRPActions: SRPActions

    @Mock
    internal lateinit var mockSignInChallengeActions: SignInChallengeActions

    @Mock
    internal lateinit var mockSignOutActions: SignOutActions

    @Mock
    internal lateinit var mockFetchAuthSessionActions: FetchAuthSessionActions

    @Mock
    internal lateinit var mockDeleteUserActions: DeleteUserActions

    internal fun setupCredentialStoreActions() {
        Mockito.`when`(credentialStoreActions.migrateLegacyCredentialStoreAction())
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        CredentialStoreEvent(CredentialStoreEvent.EventType.LoadCredentialStore())
                    )
                }
            )

//        Mockito.`when`(credentialStoreActions.clearCredentialStoreAction())
//            .thenReturn(
//                Action { dispatcher, _ ->
//                    dispatcher.send(
//                        CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
//                    )
//                }
//            )

        Mockito.`when`(credentialStoreActions.loadCredentialStoreAction())
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
                    )
                }
            )

        Mockito.`when`(credentialStoreActions.storeCredentialsAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        CredentialStoreEvent(CredentialStoreEvent.EventType.CompletedOperation(credentials))
                    )
                }
            )

        Mockito.`when`(credentialStoreActions.moveToIdleStateAction())
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        CredentialStoreEvent(CredentialStoreEvent.EventType.MoveToIdleState())
                    )
                }
            )
    }

    internal fun setupAuthActions() {
        Mockito.`when`(mockAuthActions.initializeAuthConfigurationAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthEvent(
                            AuthEvent.EventType.FetchCachedCredentials(configuration)
                        )
                    )
                }
            )

        Mockito.`when`(
            mockAuthActions.initializeAuthenticationConfigurationAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.Configure(configuration, credentials)
                        )
                    )
                }
            )

        Mockito.`when`(
            mockAuthActions.validateCredentialsAndConfiguration(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthEvent(AuthEvent.EventType.ConfigureAuthentication(configuration, credentials))
                    )
                }
            )

        Mockito.`when`(
            mockAuthActions.initializeAuthorizationConfigurationAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthorizationEvent(AuthorizationEvent.EventType.Configure)
                    )
                }
            )
    }

    internal fun setupAuthNActions() {
        Mockito.`when`(
            mockAuthenticationActions.initiateSignOutAction(
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject()
            )
        ).thenReturn(
            Action { dispatcher, _ ->
                dispatcher.send(
                    SignOutEvent(SignOutEvent.EventType.SignOutGlobally(signedInData))
                )
            }
        )
    }

    internal fun setupAuthZActions() {
        Mockito.`when`(mockAuthorizationActions.configureAuthorizationAction())
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(AuthEvent(AuthEvent.EventType.ConfiguredAuthorization))
                }
            )

        Mockito.`when`(
            mockAuthorizationActions.initializeFetchAuthSession(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.FetchIdentity(credentials)
                        )
                    )
                }
            )

//        Mockito.`when`(
//            mockAuthorizationActions.refreshAuthSessionAction(credentials)
//        )
//            .thenReturn(
//                Action { dispatcher, _ ->
//                    dispatcher.send(
//                        FetchAuthSessionEvent(
//                            FetchAuthSessionEvent.EventType.FetchIdentity(credentials)
//                        )
//                    )
//                }
//            )

//        Mockito.`when`(
//            mockAuthorizationActions.resetAuthorizationAction()
//        )
//            .thenReturn(
//                Action { dispatcher, _ ->
//                    dispatcher.send(
//                        AuthorizationEvent(AuthorizationEvent.EventType.Configure)
//                    )
//                }
//            )
    }

    internal fun setupSignInActions() {
        Mockito.`when`(mockSignInActions.startSRPAuthAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(SRPEvent(SRPEvent.EventType.InitiateSRP("username", "password")))
                }
            )
    }

    internal fun setupSRPActions() {
        Mockito.`when`(signedInData.cognitoUserPoolTokens).thenReturn(CognitoUserPoolTokens("", "", "", 0))

        Mockito.`when`(mockSRPActions.initiateSRPAuthAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(SRPEvent(SRPEvent.EventType.RespondPasswordVerifier(mapOf())))
                }
            )

        Mockito.`when`(mockSRPActions.verifyPasswordSRPAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthenticationEvent(AuthenticationEvent.EventType.SignInCompleted(signedInData))
                    )
                }
            )
    }

    internal fun setupSignOutActions() {
        Mockito.`when`(mockSignOutActions.localSignOutAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        SignOutEvent(SignOutEvent.EventType.SignedOutSuccess(SignedOutData(signedInData.username)))
                    )
                    dispatcher.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.InitializedSignedOut(
                                SignedOutData(signedInData.username)
                            )
                        )
                    )
                }
            )

        Mockito.`when`(mockSignOutActions.globalSignOutAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        SignOutEvent(SignOutEvent.EventType.RevokeToken(signedInData))
                    )
                }
            )

        Mockito.`when`(mockSignOutActions.revokeTokenAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        SignOutEvent(SignOutEvent.EventType.SignOutLocally(signedInData))
                    )
                }
            )
    }

    internal fun setupFetchAuthActions() {
        Mockito.`when`(
            mockFetchAuthSessionActions.fetchIdentityAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.FetchAwsCredentials(credentials)
                        )
                    )
                }
            )

        Mockito.`when`(
            mockFetchAuthSessionActions.fetchAWSCredentialsAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.Fetched(credentials)
                        )
                    )
                }
            )

        Mockito.`when`(
            mockFetchAuthSessionActions.notifySessionEstablishedAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthorizationEvent(
                            AuthorizationEvent.EventType.Fetched(credentials)
                        )
                    )
                }
            )
    }

    internal fun setupDeleteAction() {
        Mockito.`when`(
            mockDeleteUserActions.initDeleteUserAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthenticationEvent(AuthenticationEvent.EventType.SignOutRequested(true))
                    )
                }
            )
    }
}
