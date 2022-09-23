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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.actions.DeleteUserActions
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthActions
import com.amplifyframework.statemachine.codegen.actions.AuthenticationActions
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions
import com.amplifyframework.statemachine.codegen.actions.CustomSignInActions
import com.amplifyframework.statemachine.codegen.actions.DeviceSRPSignInActions
import com.amplifyframework.statemachine.codegen.actions.FetchAuthSessionActions
import com.amplifyframework.statemachine.codegen.actions.HostedUIActions
import com.amplifyframework.statemachine.codegen.actions.SRPActions
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.actions.SignOutActions
import com.amplifyframework.statemachine.codegen.actions.StoreActions
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.LoginsMapProvider
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.RefreshSessionEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import java.util.Date
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
    internal lateinit var signInData: SignInData

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
    internal lateinit var mockDeviceSRPSignInActions: DeviceSRPSignInActions

    @Mock
    internal lateinit var mockSRPActions: SRPActions

    @Mock
    internal lateinit var mockSignInChallengeActions: SignInChallengeActions

    @Mock
    internal lateinit var mockSignInCustomActions: CustomSignInActions

    @Mock
    internal lateinit var mockHostedUIActions: HostedUIActions

    @Mock
    internal lateinit var mockSignOutActions: SignOutActions

    @Mock
    internal lateinit var mockFetchAuthSessionActions: FetchAuthSessionActions

    @Mock
    internal lateinit var mockDeleteUserActions: DeleteUserActions

    private val dummyCredential = AmplifyCredential.UserAndIdentityPool(
        SignedInData(
            "userId",
            "username",
            Date(0),
            SignInMethod.SRP,
            DeviceMetadata.Empty,
            CognitoUserPoolTokens("idToken", "accessToken", "refreshToken", 123123L),
        ),
        "identityPool",
        AWSCredentials(
            "accessKeyId",
            "secretAccessKey",
            "sessionToken",
            123123L
        )
    )

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

    internal fun setupSignInActionWithCustomAuth() {
        Mockito.`when`(mockAuthenticationActions.initiateSignInAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        SignInEvent(
                            SignInEvent.EventType.InitiateSignInWithCustom(
                                "username",
                                "password",
                                mapOf()
                            )
                        )
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
            mockAuthorizationActions.initializeFetchUnAuthSession()
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.FetchIdentity(LoginsMapProvider.UnAuthLogins())
                        )
                    )
                }
            )

        Mockito.`when`(
            mockAuthorizationActions.initializeFetchAuthSession(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.FetchIdentity(
                                LoginsMapProvider.CognitoUserPoolLogins(",", "", "")
                            )
                        )
                    )
                }
            )

        Mockito.`when`(
            mockAuthorizationActions.initiateRefreshSessionAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        RefreshSessionEvent(
                            RefreshSessionEvent.EventType.RefreshUserPoolTokens(dummyCredential.signedInData)
                        )
                    )
                }
            )
    }

    internal fun setupSignInActions() {
        Mockito.`when`(mockSignInActions.startSRPAuthAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(SRPEvent(SRPEvent.EventType.InitiateSRP("username", "password")))
                }
            )

        Mockito.`when`(mockSignInActions.startCustomAuthAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        CustomSignInEvent(
                            CustomSignInEvent.EventType.InitiateCustomSignIn(
                                "username",
                                "password"
                            )
                        )
                    )
                }
            )
        Mockito.`when`(mockSignInActions.initResolveChallenge(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        SignInChallengeEvent(
                            SignInChallengeEvent.EventType.WaitForAnswer(
                                AuthChallenge(
                                    ChallengeNameType.CustomChallenge.toString(),
                                    "Test",
                                    "session_mock_value",
                                    mapOf()
                                )
                            )
                        )
                    )
                }
            )
    }

    fun setupCustomAuthActions() {
        Mockito.`when`(mockSignInCustomActions.initiateCustomSignInAuthAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    val authChallenge = AuthChallenge(
                        ChallengeNameType.CustomChallenge.toString(),
                        "Test",
                        "session_mock_value",
                        mapOf()
                    )
                    dispatcher.send(
                        SignInEvent(SignInEvent.EventType.ReceivedChallenge(authChallenge))
                    )
                }
            )

        Mockito.`when`(
            mockSignInChallengeActions.verifyChallengeAuthAction(
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject()
            )
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(SignInChallengeEvent(SignInChallengeEvent.EventType.Verified()))
                    dispatcher.send(CustomSignInEvent(CustomSignInEvent.EventType.FinalizeSignIn()))
                    dispatcher.send(
                        AuthenticationEvent(AuthenticationEvent.EventType.SignInCompleted(signedInData))
                    )
                }
            )
    }

    internal fun setupSRPActions() {
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
            mockFetchAuthSessionActions.refreshUserPoolTokensAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        RefreshSessionEvent(
                            RefreshSessionEvent.EventType.RefreshAuthSession(
                                dummyCredential.signedInData,
                                LoginsMapProvider.UnAuthLogins()
                            )
                        )
                    )
                }
            )

        Mockito.`when`(
            mockFetchAuthSessionActions.refreshAuthSessionAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.FetchIdentity(LoginsMapProvider.UnAuthLogins())
                        )
                    )
                }
            )

        Mockito.`when`(
            mockFetchAuthSessionActions.fetchIdentityAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.FetchAwsCredentials(
                                "identityId",
                                LoginsMapProvider.UnAuthLogins()
                            )
                        )
                    )
                }
            )

        Mockito.`when`(
            mockFetchAuthSessionActions.fetchAWSCredentialsAction("identityId", LoginsMapProvider.UnAuthLogins())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(
                            FetchAuthSessionEvent.EventType.Fetched("identityId", dummyCredential.credentials)
                        )
                    )
                }
            )

        Mockito.`when`(
            mockFetchAuthSessionActions.notifySessionEstablishedAction("identityId", dummyCredential.credentials)
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthorizationEvent(
                            AuthorizationEvent.EventType.Fetched("identityId", dummyCredential.credentials)
                        )
                    )
                }
            )

        Mockito.`when`(
            mockFetchAuthSessionActions.notifySessionRefreshedAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthorizationEvent(AuthorizationEvent.EventType.Refreshed(dummyCredential))
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
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignOutRequested(SignOutData(true))
                        )
                    )
                }
            )
    }
}
