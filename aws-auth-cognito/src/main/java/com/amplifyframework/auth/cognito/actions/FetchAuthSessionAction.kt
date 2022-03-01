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

import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityResponse
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.events.FetchAwsCredentialsEvent
import com.amplifyframework.auth.cognito.events.FetchIdentityEvent
import com.amplifyframework.auth.cognito.events.FetchUserPoolTokensEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import java.util.*

interface FetchAuthSessionAction : Action

class ConfigureUserPoolTokensAction : FetchAuthSessionAction {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as AuthEnvironment)
        //TODO : Fetch the userPoolToken from the credential store
        val TWO_MINUTES = 2 * 60 * 1000
        val TEN_MINUTES = 2 * 60 * 1000
        var userPoolToken = "MOCK_TOKEN"
        val userPoolTokenExpiryTime = System.currentTimeMillis() + TEN_MINUTES
        val expiryTimeOfToken = System.currentTimeMillis() + TWO_MINUTES
        //TODO: Check the validity of the user pool token
        if (userPoolTokenExpiryTime > expiryTimeOfToken) {
            //User Pool Token is still valid
            val event =
                FetchUserPoolTokensEvent(
                    FetchUserPoolTokensEvent.EventType.Fetched()
                )
            dispatcher.send(event)
        } else {
            //Token has expired and we need a refresh
            val event = FetchUserPoolTokensEvent(
                FetchUserPoolTokensEvent.EventType.Refresh()
            )
            dispatcher.send(event)
        }

    }
}

class ConfigureIdentityAction : FetchAuthSessionAction {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as AuthEnvironment)
        //TODO : Fetch identity ID from Credential store and initialize it here
        var idenityID = null
        if (idenityID != null) {
            val event = FetchIdentityEvent(
                FetchIdentityEvent.EventType.Fetched()
            )
            dispatcher.send(event)
        } else {
            val event = FetchIdentityEvent(
                FetchIdentityEvent.EventType.Fetch()
            )
            dispatcher.send(event)
        }
    }
}

class ConfigureAWSCredentialsAction : FetchAuthSessionAction {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as AuthEnvironment)
        //TODO : Fetch AWS Credentials from Credential store and check the validity of the IdentityID
        val TEN_MINUTES = 2 * 60 * 1000
        val getCredentialsForIdentityResponse: GetCredentialsForIdentityResponse =
            GetCredentialsForIdentityResponse.invoke {
                credentials = Credentials.invoke {
                    accessKeyId = ""
                    expiration = Instant.now()
                    secretKey = ""
                    sessionToken = ""
                }
                identityId = "MOCK_IDENTITY_ID"
            }

        val userPoolTokenExpiryTime = System.currentTimeMillis() + TEN_MINUTES
        //TODO: Check the validity of the user pool token by checking if it expires 10 minutes from now
        if (getCredentialsForIdentityResponse.credentials?.expiration as Instant > Instant.now()) {
            val event =
                FetchAwsCredentialsEvent(
                    FetchAwsCredentialsEvent.EventType.Fetched()
                )
            dispatcher.send(event)
        } else {
            //if invalid
            val event =
                FetchAwsCredentialsEvent(
                    FetchAwsCredentialsEvent.EventType.Fetch()
                )
            dispatcher.send(event)
        }
    }
}