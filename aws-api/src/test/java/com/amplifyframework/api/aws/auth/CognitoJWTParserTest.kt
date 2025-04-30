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

package com.amplifyframework.api.aws.auth

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CognitoJWTParserTest {
    private val usernameKey = "username"
    private val username = "test-user"

    private val cognitoGroupsKey = "cognito:groups"
    private val cognitoGroups = JSONArray(arrayOf("Admins"))

    private val claimKey = "iss"
    private val claimVal = "cognito"

    private val fakeJWTToken: FakeJWTToken = FakeJWTToken.builder()
        .putPayload(usernameKey, username)
        .putPayload(cognitoGroupsKey, cognitoGroups)
        .putPayload(claimKey, claimVal)
        .build()

    @Test
    fun `getPayload retrieves correct payload`() {
        val payload = CognitoJWTParser.getPayload(fakeJWTToken.asString())

        assertEquals(username, payload.get(usernameKey))
        assertEquals(cognitoGroups, payload.get(cognitoGroupsKey))
    }

    @Test
    fun `getClaim retrieves correct claim`() {
        val claim = CognitoJWTParser.getClaim(fakeJWTToken.asString(), claimKey)

        assertEquals(claimVal, claim)
    }
}
