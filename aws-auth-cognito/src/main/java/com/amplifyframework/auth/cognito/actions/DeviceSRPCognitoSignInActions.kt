package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmDeviceRequest
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.DeviceSRPSignInActions
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.DeviceSRPSignInEvent

class DeviceSRPCognitoSignInActions: DeviceSRPSignInActions {
    override fun respondDevicePasswordVerifier(event: DeviceSRPSignInEvent.EventType.RespondDevicePasswordVerifier): Action =
        Action<AuthEnvironment>("RespondToDevicePasswordVerifier") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val confirmDeviceResponse = cognitoAuthService.cognitoIdentityProviderClient?.confirmDevice(
                    ConfirmDeviceRequest.invoke {
                        accessToken = "STUB"
                        deviceKey = "STUB"
                    }
                )
                if (confirmDeviceResponse != null) {
                    DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.FinalizeSignIn())
                } else {
                    throw AuthException(
                        "This sign in method is not supported",
                        "Please consult our docs for supported sign in methods"
                    )
                }
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
        //TODO: Cleaning up Device Storage
        val evt = DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.RestoreToNotInitialized())
        dispatcher.send(evt)
    }
}