/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.auth.cognito.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SetupTOTPActions
import com.amplifyframework.statemachine.codegen.events.SetupTOTPEvent

internal object SetupTOTPCognitoActions: SetupTOTPActions {
    override fun initiateTOTPSetup(eventType: SetupTOTPEvent.EventType.SetupTOTP): Action {
        TODO("Not yet implemented")
    }

    override fun verifyChallengeAnswer(eventType: SetupTOTPEvent.EventType.VerifyChallengeAnswer): Action {
        TODO("Not yet implemented")
    }

    override fun respondToAuthChallenge(eventType: SetupTOTPEvent.EventType.RespondToAuthChallenge): Action {
        TODO("Not yet implemented")
    }

}
