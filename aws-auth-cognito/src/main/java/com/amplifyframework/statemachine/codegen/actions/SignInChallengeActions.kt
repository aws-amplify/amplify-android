package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent

interface SignInChallengeActions {
    fun verifySignInChallenge(event: SignInChallengeEvent.EventType.VerifyChallengeAnswer): Action
}