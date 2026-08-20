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

package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import java.time.Instant
import java.time.temporal.ChronoUnit

internal object SessionHelper {
    /**
     * Returns true if the access and id tokens have not expired.
     * @return boolean to indicate if the access and id tokens are expired.
     */
    fun isValidTokens(userPoolTokens: CognitoUserPoolTokens): Boolean {
        val currentTimeStamp = Instant.now()
        return when {
            userPoolTokens.idToken == null -> false
            userPoolTokens.accessToken == null -> false
            else ->
                currentTimeStamp < userPoolTokens.idToken.expiration &&
                    currentTimeStamp < userPoolTokens.accessToken.expiration
        }
    }

    /**
     * Returns true if the AWS credentials have not expired.
     * @return boolean to indicate if the AWS credentials are expired.
     */
    fun isValidSession(awsCredentials: AWSCredentials): Boolean {
        val currentTimeStamp = Instant.now()
        val credentialsExpirationInSecond = awsCredentials.expiration?.let { Instant.ofEpochSecond(it) }

        // Check if current timestamp is BEFORE expiration && next year is AFTER expiration
        // The latter check is to fix v1 > v2 migration issues as found in:
        // https://github.com/aws-amplify/amplify-android/issues/2789
        return (
            currentTimeStamp < credentialsExpirationInSecond &&
                currentTimeStamp.plus(365, ChronoUnit.DAYS) > credentialsExpirationInSecond
            )
    }
}

internal fun CognitoUserPoolTokens.isValid() = SessionHelper.isValidTokens(this)
