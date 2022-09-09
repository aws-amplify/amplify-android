package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.DeviceSRPSignInActions
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.DeviceSRPSignInEvent

object DeviceSRPCognitoSignInActions : DeviceSRPSignInActions {

    private val KEY_PASSWORD_CLAIM_SECRET_BLOCK = "PASSWORD_CLAIM_SECRET_BLOCK"
    private val KEY_PASSWORD_CLAIM_SIGNATURE = "PASSWORD_CLAIM_SIGNATURE"
    private val KEY_TIMESTAMP = "TIMESTAMP"
    private val KEY_SALT = "SALT"
    private val KEY_SECRET_BLOCK = "SECRET_BLOCK"
    private val KEY_SRP_A = "SRP_A"
    private val KEY_SRP_B = "SRP_B"
    private val KEY_USERNAME = "USERNAME"
    private val KEY_DEVICE_KEY = "DEVICE_KEY"
    override fun respondDeviceSRP(event: DeviceSRPSignInEvent.EventType.RespondDeviceSRPChallenge): Action =
        Action<AuthEnvironment>("RespondToDevicePasswordVerifier") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                event.challengeParameters?.let { params ->
                    val username = params.getValue(KEY_USERNAME)
                    val respondToAuthChallenge =
                        cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge(
                            RespondToAuthChallengeRequest.invoke {
                                challengeName = ChallengeNameType.DeviceSrpAuth
                                clientId = configuration.userPool?.appClient
                                mapOf(
                                    KEY_USERNAME to username,
                                    KEY_DEVICE_KEY to "STUB", //TODO: get this from the device credential store
                                    KEY_SRP_A to srpHelper.getPublicA()
                                ).also { challengeResponses = it }
                            }
                        )
                    if (respondToAuthChallenge != null) {
                        SignInChallengeHelper.evaluateNextStep(
                            username = username,
                            signInMethod = SignInMethod.SRP,
                            authenticationResult = respondToAuthChallenge.authenticationResult,
                            challengeNameType = respondToAuthChallenge.challengeName,
                            challengeParameters = respondToAuthChallenge.challengeParameters,
                            session = respondToAuthChallenge.session,
                        )
                    } else {
                        throw AuthException(
                            "This sign in method is not supported",
                            "Please consult our docs for supported sign in methods"
                        )
                    }
                } ?: throw AuthException(
                    "There was problem while signing you in",
                    AuthException.TODO_RECOVERY_SUGGESTION
                )
            } catch (e: Exception) {
                val errorEvent = DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.CancelSRPSignIn())
                logger?.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)
                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun respondDevicePasswordVerifier(event: DeviceSRPSignInEvent.EventType.RespondDevicePasswordVerifier): Action =
        Action<AuthEnvironment>("RespondToDevicePasswordVerifier") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                event.challengeParameters?.let { params ->
                    val salt = params.getValue(KEY_SALT)
                    val secretBlock = params.getValue(KEY_SECRET_BLOCK)
                    val srpB = params.getValue(KEY_SRP_B)
                    val username = params.getValue(KEY_USERNAME)

                    val respondToAuthChallenge =
                        cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge(
                            RespondToAuthChallengeRequest.invoke {
                                challengeName = ChallengeNameType.DevicePasswordVerifier
                                clientId = configuration.userPool?.appClient
                                challengeResponses = mapOf(
                                    KEY_USERNAME to username,
                                    KEY_PASSWORD_CLAIM_SECRET_BLOCK to secretBlock,
                                    KEY_TIMESTAMP to srpHelper.dateString,
                                    KEY_PASSWORD_CLAIM_SIGNATURE to srpHelper.getSignature(salt, srpB, secretBlock),
                                    KEY_DEVICE_KEY to "STUB", //TODO: get this from the device credential store
                                ).also { challengeResponses = it }
                            }
                        )

                    if (respondToAuthChallenge != null) {
                        DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.FinalizeSignIn())
                        SignInChallengeHelper.evaluateNextStep(
                            username = username,
                            signInMethod = SignInMethod.SRP,
                            authenticationResult = respondToAuthChallenge.authenticationResult,
                            challengeNameType = respondToAuthChallenge.challengeName,
                            challengeParameters = respondToAuthChallenge.challengeParameters,
                            session = respondToAuthChallenge.session,
                        )
                    } else {
                        throw AuthException(
                            "This sign in method is not supported",
                            "Please consult our docs for supported sign in methods"
                        )
                    }
                } ?: throw AuthException(
                    "There was problem while signing you in",
                    AuthException.TODO_RECOVERY_SUGGESTION
                )
            } catch (e: Exception) {
                val errorEvent = DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.CancelSRPSignIn())
                logger?.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)
                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun cancellingSignIn(event: DeviceSRPSignInEvent.EventType.CancelSRPSignIn): Action =
        Action<AuthEnvironment>("CancelSignIn") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            //TODO: Cleaning up Device Storage once implemented
            val evt = DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.RestoreToNotInitialized())
            dispatcher.send(evt)
        }
}