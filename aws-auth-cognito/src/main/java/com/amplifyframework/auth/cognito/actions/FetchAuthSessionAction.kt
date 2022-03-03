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

        val userPoolTokens = env.awsCognitoAuthCredentialStore.retrieveCredential()?.cognitoUserPoolTokens
        if (userPoolTokens == null) {
            //Failure to fetch, Refresh
            val event = FetchUserPoolTokensEvent(
                    FetchUserPoolTokensEvent.EventType.Refresh()
            )
            dispatcher.send(event)
        }
        val userPoolTokenExpiryTime = userPoolTokens?.tokenExpiration as Int
        val rightNow = Calendar.getInstance()
        val offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET)
        //The token must be valid for 2 minutes from now
        if (userPoolTokenExpiryTime > ((rightNow.timeInMillis + offset) + 2 * 60 * 1000)) {
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
        val identityID = env.awsCognitoAuthCredentialStore.retrieveCredential()?.identityId
        if (identityID != null) {
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
        val credentials = env.awsCognitoAuthCredentialStore.retrieveCredential()
        val awsCredentials = credentials?.awsCredentials
        val rightNow = Calendar.getInstance()
        val offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET)
        //AWS Credentials should be valid for up to 10 minutes from now
        if (awsCredentials?.expiration != null && awsCredentials.expiration > (rightNow.timeInMillis + offset) + 10 * 60 * 1000) {
            val event =
                    FetchAwsCredentialsEvent(
                            FetchAwsCredentialsEvent.EventType.Fetched())
            dispatcher.send(event)
        } else {
            val event = FetchAwsCredentialsEvent(
                    FetchAwsCredentialsEvent.EventType.Fetch(
                            credentials?.identityId as String))
            dispatcher.send(event)
        }
    }
}