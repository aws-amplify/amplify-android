/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthResponse
import com.amplifyframework.auth.cognito.testUtil.authenticationResult
import com.amplifyframework.statemachine.codegen.data.ChallengeParameter
import io.kotest.matchers.shouldBe
import org.junit.Test

class AuthHelperTest {
    // JWT with no username claims
    private val tokenWithoutUsernames = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6I" +
        "kpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30"

    // JWT with both username and cognito:username claims
    private val tokenWithUsernames =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG" +
            "9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwidXNlcm5hbWUiOiJqd3RVc2VyIiwiY29nbml0bzp1c2VybmFtZSI6ImNv" +
            "Z25pdG9Vc2VyIn0.TDAPYzxW2Vxp0pfhGyUDkwN8d0x55cWBzNqluQc1P-0"

    @Test
    fun `getActiveUsername returns fallback username for empty initiateAuthResponse`() {
        val response = initiateAuthResponse()
        val username = AuthHelper.getActiveUsername("fallback", response)
        username shouldBe "fallback"
    }

    @Test
    fun `getActiveUsername returns fallback username for response with no username claims`() {
        val response = initiateAuthResponse(
            idToken = tokenWithoutUsernames,
            accessToken = tokenWithoutUsernames
        )
        val username = AuthHelper.getActiveUsername("fallback", response)
        username shouldBe "fallback"
    }

    @Test
    fun `getActiveUsername prefers username from idToken`() {
        val response = initiateAuthResponse(
            idToken = tokenWithUsernames,
            accessToken = tokenWithUsernames,
            usernameParameter = "challengeUsername",
            userIdForSrpParameter = "userIdForSrp"
        )
        val username = AuthHelper.getActiveUsername("fallback", response)
        username shouldBe "cognitoUser"
    }

    @Test
    fun `getActiveUsername returns username from accessToken if idToken is missing`() {
        val response = initiateAuthResponse(
            accessToken = tokenWithUsernames,
            usernameParameter = "challengeUsername",
            userIdForSrpParameter = "userIdForSrp"
        )
        val username = AuthHelper.getActiveUsername("fallback", response)
        username shouldBe "jwtUser"
    }

    @Test
    fun `getActiveUsername returns username from username parameter if both tokens are missing`() {
        val response = initiateAuthResponse(
            usernameParameter = "challengeUsername",
            userIdForSrpParameter = "userIdForSrp"
        )
        val username = AuthHelper.getActiveUsername("fallback", response)
        username shouldBe "challengeUsername"
    }

    @Test
    fun `getActiveUsername returns username from username srp id`() {
        val response = initiateAuthResponse(
            userIdForSrpParameter = "userIdForSrp"
        )
        val username = AuthHelper.getActiveUsername("fallback", response)
        username shouldBe "userIdForSrp"
    }

    private fun initiateAuthResponse(
        idToken: String? = null,
        accessToken: String? = null,
        usernameParameter: String? = null,
        userIdForSrpParameter: String? = null
    ) = InitiateAuthResponse {
        if (idToken != null || accessToken != null) {
            this.authenticationResult = authenticationResult(
                idToken = idToken,
                accessToken = accessToken
            )
        }
        if (usernameParameter != null || userIdForSrpParameter != null) {
            this.challengeParameters = buildMap {
                usernameParameter?.let { put(ChallengeParameter.Username.key, it) }
                userIdForSrpParameter?.let { put(ChallengeParameter.UserIdForSrp.key, it) }
            }
        }
    }
}
