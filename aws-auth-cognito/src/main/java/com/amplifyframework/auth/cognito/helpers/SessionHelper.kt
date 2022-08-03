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

object SessionHelper {
    /**
     * Returns expiration of this id token.
     * @return id token expiration claim as {@link java.time.Instant} in UTC.
     */
    internal fun getExpiration(token: String): Instant? {
        val claim = JWTParser.getClaim(token, "exp")
        return claim?.let {
            Instant.ofEpochSecond(claim.toLong())
        }
    }

    /**
     * Returns the username set in the access token.
     * @return Username.
     */
    fun getUsername(token: String): String? {
        return JWTParser.getClaim(token, "username")
    }

    /**
     * Returns the usersub set in the access token.
     * @return usersub
     */
    fun getUserSub(token: String): String? {
        return JWTParser.getClaim(token, "sub")
    }

    /**
     * Returns if the access and id tokens have not expired.
     * @return boolean to indicate if the access and id tokens have not expired.
     */
    fun isValid(userPoolTokens: CognitoUserPoolTokens): Boolean {
        val currentTimeStamp = Instant.now()
        return when {
            userPoolTokens.idToken == null -> false
            userPoolTokens.accessToken == null -> false
            else -> currentTimeStamp < getExpiration(userPoolTokens.idToken) && currentTimeStamp < getExpiration(
                userPoolTokens.accessToken
            )
        }
    }

    fun isValidSession(awsCredentials: AWSCredentials): Boolean {
        // TODO: verify session expiry
        val currentTimeStamp = Instant.now()
        return currentTimeStamp < awsCredentials.expiration?.let { Instant.ofEpochSecond(it) }
    }
}
