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
package com.amazonaws.appsync

import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.core.model.AuthRule
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.ModelOperation
import com.amplifyframework.core.model.ModelSchema
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import org.junit.Test

/**
 * Tests [AppSyncClaimInjector].
 *
 * The rule logic decides whether a subscription gets an owner variable at all, and getting it wrong is
 * silent in both directions: injecting when unnecessary narrows what a subscriber receives, while
 * failing to inject makes AppSync reject the subscription. Each branch has a test.
 */
class AppSyncClaimInjectorTest {

    private val injector = AppSyncClaimInjector()
    private val variableSlot = slot<String>()
    private val valueSlot = slot<Any>()

    private val userPools = AppSyncClientAuthorizer.UserPools { jwt(mapOf("username" to "alice")) }

    // ── When no owner variable is needed ────────────────────────────────

    @Test
    fun `a raw request is returned untouched, having no schema to inspect`() = runTest {
        val raw: GraphQLRequest<String> = SimpleGraphQLRequest(
            "subscription { onCreateTodo { id } }",
            emptyMap(),
            String::class.java,
            GsonVariablesSerializer()
        )

        injector.inject(raw, AppSyncAuthMode.USER_POOLS, userPools) shouldBe raw
    }

    @Test
    fun `a model with no owner rule is returned untouched`() = runTest {
        val request = requestWith(privateRule())

        injector.inject(request, AppSyncAuthMode.USER_POOLS, userPools) shouldBe request
        verify(exactly = 0) { request.newBuilder() }
    }

    @Test
    fun `a public read rule under an api key needs no owner`() = runTest {
        // The rule already permits the read, so adding an owner filter would narrow it for no reason.
        val request = requestWith(publicRule(), ownerRule())

        injector.inject(request, AppSyncAuthMode.API_KEY, AppSyncClientAuthorizer.ApiKey("da2-fake")) shouldBe
            request
    }

    @Test
    fun `a public read rule does not apply when subscribing with a different mode`() = runTest {
        // The public rule only excuses the owner when the request is actually using the API key.
        val request = requestWith(publicRule(), ownerRule())

        injector.inject(request, AppSyncAuthMode.USER_POOLS, userPools)

        variableSlot.captured shouldBe "owner"
    }

    @Test
    fun `group membership takes precedence over the owner filter`() = runTest {
        // A user who can already read via a group should receive everything that group can see.
        val inGroup = AppSyncClientAuthorizer.UserPools {
            jwt(mapOf("username" to "alice", "cognito:groups" to listOf("Admins")))
        }
        val request = requestWith(ownerRule(), groupRule("Admins"))

        injector.inject(request, AppSyncAuthMode.USER_POOLS, inGroup) shouldBe request
    }

    @Test
    fun `a user outside every read-restricting group still gets the owner filter`() = runTest {
        val notInGroup = AppSyncClientAuthorizer.UserPools {
            jwt(mapOf("username" to "alice", "cognito:groups" to listOf("Readers")))
        }
        val request = requestWith(ownerRule(), groupRule("Admins"))

        injector.inject(request, AppSyncAuthMode.USER_POOLS, notInGroup)

        variableSlot.captured shouldBe "owner"
    }

    // ── When it is needed ───────────────────────────────────────────────

    @Test
    fun `the owner variable is added from the identity claim`() = runTest {
        val request = requestWith(ownerRule())

        injector.inject(request, AppSyncAuthMode.USER_POOLS, userPools)

        variableSlot.captured shouldBe "owner"
        valueSlot.captured shouldBe "alice"
    }

    @Test
    fun `a custom owner field and identity claim are honoured`() = runTest {
        val custom = AppSyncClientAuthorizer.UserPools { jwt(mapOf("email" to "alice@example.com")) }
        val request = requestWith(ownerRule(ownerField = "editor", identityClaim = "email"))

        injector.inject(request, AppSyncAuthMode.USER_POOLS, custom)

        variableSlot.captured shouldBe "editor"
        valueSlot.captured shouldBe "alice@example.com"
    }

    @Test
    fun `an OIDC token is read the same way`() = runTest {
        val oidc = AppSyncClientAuthorizer.Oidc { jwt(mapOf("username" to "bob")) }
        val request = requestWith(ownerRule())

        injector.inject(request, AppSyncAuthMode.OIDC, oidc)

        valueSlot.captured shouldBe "bob"
    }

    @Test
    fun `an identity claim of cognito colon username reads the username claim`() = runTest {
        // AuthRule maps that value to "username" for backwards compatibility with an older CLI, so a
        // token carrying "username" satisfies a rule that names "cognito:username".
        val request = requestWith(ownerRule(identityClaim = "cognito:username"))

        injector.inject(request, AppSyncAuthMode.USER_POOLS, userPools)

        valueSlot.captured shouldBe "alice"
    }

    // ── Failures ────────────────────────────────────────────────────────

    @Test
    fun `an auth mode with no claims cannot satisfy an owner rule`() = runTest {
        // An API key identifies no user, so there is no owner to send.
        val error = shouldThrow<AppSyncProviderNotConfiguredException> {
            injector.inject(requestWith(ownerRule()), AppSyncAuthMode.API_KEY, AppSyncClientAuthorizer.ApiKey("k"))
        }

        error.message shouldContain "API_KEY"
    }

    @Test
    fun `a missing identity claim is an authorization claim failure, not a parsing failure`() = runTest {
        // The token was readable; it just does not carry what the rule requires.
        val noClaim = AppSyncClientAuthorizer.UserPools { jwt(mapOf("sub" to "123")) }

        shouldThrow<AppSyncAuthorizationClaimException> {
            injector.inject(requestWith(ownerRule()), AppSyncAuthMode.USER_POOLS, noClaim)
        }.message shouldContain "username"
    }

    @Test
    fun `a malformed token is a parsing failure`() = runTest {
        val bad = AppSyncClientAuthorizer.UserPools { "not-a-jwt" }

        shouldThrow<AppSyncTokenParsingException> {
            injector.inject(requestWith(ownerRule()), AppSyncAuthMode.USER_POOLS, bad)
        }
    }

    @Test
    fun `more than one owner read rule is refused rather than guessed at`() = runTest {
        // AppSync generates a variable per owner rule, so there is no single value to send.
        val request = requestWith(ownerRule(), ownerRule(ownerField = "editor"))

        shouldThrow<AppSyncAuthorizationClaimException> {
            injector.inject(request, AppSyncAuthMode.USER_POOLS, userPools)
        }.message shouldContain "more than one owner"
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /** An unsigned JWT whose payload carries [claims]. The signature is never checked. */
    private fun jwt(claims: Map<String, Any>): String {
        val payload = buildString {
            append("{")
            append(
                claims.entries.joinToString(",") { (key, value) ->
                    val rendered = when (value) {
                        is List<*> -> value.joinToString(",", "[", "]") { "\"$it\"" }
                        else -> "\"$value\""
                    }
                    "\"$key\":$rendered"
                }
            )
            append("}")
        }
        val encoded = payload.encodeUtf8().base64Url().trimEnd('=')
        return "header.$encoded.signature"
    }

    private fun requestWith(vararg rules: AuthRule): AppSyncGraphQLRequest<String> {
        val rebuilt: AppSyncGraphQLRequest<String> = mockk(relaxed = true)
        val builder: AppSyncGraphQLRequest.Builder = mockk(relaxed = true)
        every { builder.variable(capture(variableSlot), any(), capture(valueSlot)) } returns builder
        every { builder.build<String>() } returns rebuilt

        return mockk {
            every { content } returns """{"query":"subscription { onCreateTodo { id } }"}"""
            every { modelSchema } returns ModelSchema.builder()
                .name("Todo")
                .authRules(rules.toList())
                .build()
            every { newBuilder() } returns builder
        }
    }

    private fun ownerRule(ownerField: String = "owner", identityClaim: String = "username") = AuthRule.builder()
        .authStrategy(AuthStrategy.OWNER)
        .authProvider(AuthStrategy.OWNER.defaultAuthProvider)
        .identityClaim(identityClaim)
        .ownerField(ownerField)
        .operations(listOf(ModelOperation.READ))
        .build()

    private fun groupRule(vararg groups: String) = AuthRule.builder()
        .authStrategy(AuthStrategy.GROUPS)
        .authProvider(AuthStrategy.GROUPS.defaultAuthProvider)
        .identityClaim("cognito:groups")
        .groups(groups.toList())
        .operations(listOf(ModelOperation.READ))
        .build()

    private fun publicRule() = AuthRule.builder()
        .authStrategy(AuthStrategy.PUBLIC)
        .authProvider(AuthStrategy.Provider.API_KEY)
        .operations(listOf(ModelOperation.READ))
        .build()

    private fun privateRule() = AuthRule.builder()
        .authStrategy(AuthStrategy.PRIVATE)
        .authProvider(AuthStrategy.Provider.IAM)
        .operations(listOf(ModelOperation.READ))
        .build()
}
