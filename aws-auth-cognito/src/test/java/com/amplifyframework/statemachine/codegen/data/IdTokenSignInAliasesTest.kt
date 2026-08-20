/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.statemachine.codegen.data

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.Base64
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdTokenSignInAliasesTest {

    private fun idTokenWith(claims: Map<String, String>): IdToken {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(JSONObject(claims).toString().toByteArray())
        return IdToken("header.$payload.signature")
    }

    @Test
    fun `returns email phone and preferred username`() {
        val token = idTokenWith(
            mapOf(
                "email" to "user@example.com",
                "phone_number" to "+15550100",
                "preferred_username" to "alias-name"
            )
        )

        token.signInAliases shouldContainExactly listOf("user@example.com", "+15550100", "alias-name")
    }

    @Test
    fun `omits absent claims`() {
        val token = idTokenWith(mapOf("email" to "user@example.com"))

        token.signInAliases shouldContainExactly listOf("user@example.com")
    }

    @Test
    fun `returns empty list when no alias claims are present`() {
        idTokenWith(mapOf("sub" to "abc")).signInAliases shouldBe emptyList()
    }

    @Test
    fun `returns empty list for an unparseable token instead of throwing`() {
        IdToken("not-a-jwt").signInAliases shouldBe emptyList()
    }
}
