package com.amplifyframework.auth.cognito.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.CustomSignInActions
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent

class SignInCustomActions: CustomSignInActions {
    override fun initiateCustomSignInAuthAction(event: CustomSignInEvent.EventType.InitiateCustomSignIn): Action {

        /**
         * InitiateAuth api call and in the response we will get a challenge  which could be one of these enums:
         * https://docs.amplify.aws/sdk/auth/custom-auth-flow/q/platform/android/#custom-authentication-in-amplify
         * Based on that we will proceed.
         * */
        TODO("Not yet implemented")
    }

    override fun respondToChallengeAction(event: CustomSignInEvent.EventType.FinalizeSignIn): Action {
        TODO("Not yet implemented")
    }
}