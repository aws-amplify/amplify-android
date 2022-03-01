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

package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.events.FetchAwsCredentialsEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import java.util.*

interface FetchAwsCredentialsAction : Action

class InitFetchAWSCredentialsAction : FetchAwsCredentialsAction {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as AuthEnvironment)
        //TODO: use the identityID fetched from the credential store
        var mockedIdentityId = "MOCK_IDENTITY_ID"
        val getCredentialsForIdentityRequest = GetCredentialsForIdentityRequest.invoke {
            this.identityId = mockedIdentityId
        }
        try {
            val getCredentialsForIdentityResponse =
                env.cognitoIdentityClient.getCredentialsForIdentity(
                    getCredentialsForIdentityRequest
                )
            //TODO: Update the credential store with the identityID returned
            val event =
                FetchAwsCredentialsEvent(
                    FetchAwsCredentialsEvent.EventType.Fetched()
                )
            dispatcher.send(event)
        } catch (e: Exception) {
            val event =
                FetchAwsCredentialsEvent(
                    FetchAwsCredentialsEvent.EventType.ThrowError(e)
                )
            dispatcher.send(event)
        }
    }
}