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
import com.amplifyframework.api.aws.AuthorizationType
import com.amplifyframework.api.aws.GsonVariablesSerializer
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.core.model.AuthRule
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.ModelField
import com.amplifyframework.core.model.ModelOperation
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.foundation.credentials.AwsCredentials
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

/**
 * Tests [AppSyncAuthModeResolver] — which auth modes a request is eligible for, and in what order.
 *
 * Ordering itself belongs to `MultiAuthorizationTypeIterator` and is tested with it. What these tests
 * pin is the part this class owns: precedence between a per-request override, the model's `@auth` rules
 * and the configured default; that a mode with no configured authorizer is never proposed; and that each
 * mode arrives paired with the authorizer that will sign for it.
 */
class AppSyncAuthModeResolverTest {

    // ── Single auth ─────────────────────────────────────────────────────

    @Test
    fun `single auth resolves to its one mode`() {
        val resolver = AppSyncAuthModeResolver(
            AppSyncAuthorization.Single(AppSyncClientAuthorizer.ApiKey("da2-fakekey"))
        )

        resolver.modesFor(rawRequest()) shouldContainExactly listOf(AppSyncAuthMode.API_KEY)
    }

    @Test
    fun `single auth ignores auth rules, because there is only one authorizer to use`() {
        val resolver = AppSyncAuthModeResolver(
            AppSyncAuthorization.Single(AppSyncClientAuthorizer.ApiKey("da2-fakekey"))
        )

        resolver.modesFor(modelRequest(ownerRule(), publicRule())) shouldContainExactly
            listOf(AppSyncAuthMode.API_KEY)
    }

    // ── Multi auth from @auth rules ─────────────────────────────────────

    @Test
    fun `multi auth orders modes by auth rule priority`() {
        // OWNER(2) before PRIVATE(4) before PUBLIC(5), per AuthStrategy priorities.
        val resolver = AppSyncAuthModeResolver(multi())

        resolver.modesFor(modelRequest(publicRule(), privateRule(), ownerRule())) shouldContainExactly
            listOf(AppSyncAuthMode.USER_POOLS, AppSyncAuthMode.IAM, AppSyncAuthMode.API_KEY)
    }

    @Test
    fun `multi auth drops modes that have no configured authorizer`() {
        // The model allows IAM, but this client has no IAM authorizer, so it must not be proposed.
        val resolver = AppSyncAuthModeResolver(
            AppSyncAuthorization.Multi(
                defaultAuthMode = AppSyncAuthMode.API_KEY,
                authorizers = listOf(AppSyncClientAuthorizer.ApiKey("da2-fakekey"))
            )
        )

        val modes = resolver.modesFor(modelRequest(privateRule(), publicRule()))

        modes shouldNotContain AppSyncAuthMode.IAM
        modes shouldContainExactly listOf(AppSyncAuthMode.API_KEY)
    }

    @Test
    fun `multi auth falls back to the default when no auth rule matches a configured authorizer`() {
        val resolver = AppSyncAuthModeResolver(
            AppSyncAuthorization.Multi(
                defaultAuthMode = AppSyncAuthMode.LAMBDA,
                authorizers = listOf(AppSyncClientAuthorizer.Lambda { "token" })
            )
        )

        resolver.modesFor(modelRequest(ownerRule())) shouldContainExactly listOf(AppSyncAuthMode.LAMBDA)
    }

    @Test
    fun `a raw request carries no auth rules, so multi auth falls back to the default`() {
        val resolver = AppSyncAuthModeResolver(multi())

        resolver.modesFor(rawRequest()) shouldContainExactly listOf(AppSyncAuthMode.API_KEY)
    }

    @Test
    fun `rules that differ only in their owner field yield one mode`() {
        // Both owner rules resolve to User Pools. MultiAuthorizationTypeIterator sorts rules by strategy
        // then provider, so these two compare equal and it discards one before yielding anything.
        val resolver = AppSyncAuthModeResolver(multi())

        resolver.modesFor(modelRequest(ownerRule(), ownerRule(ownerField = "editor"))) shouldContainExactly
            listOf(AppSyncAuthMode.USER_POOLS)
    }

    @Test
    fun `rules with different strategies but the same provider yield one mode`() {
        // OWNER and PRIVATE hold different strategy priorities, so the iterator keeps both rules and
        // yields their shared User Pools provider twice. Nothing is gained by sending the same identity
        // again, so the resolver has to collapse the repeat itself.
        val resolver = AppSyncAuthModeResolver(multi())

        val privateUserPools = AuthRule.builder()
            .authStrategy(AuthStrategy.PRIVATE)
            .authProvider(AuthStrategy.PRIVATE.defaultAuthProvider)
            .operations(ALL_OPERATIONS)
            .build()

        resolver.modesFor(modelRequest(ownerRule(), privateUserPools)) shouldContainExactly
            listOf(AppSyncAuthMode.USER_POOLS)
    }

    @Test
    fun `a field-level auth rule makes its mode eligible`() {
        // Rules can sit on a field as well as on the model, and a field rule can allow a mode the model
        // rules do not — which changes both the candidate list and the order the modes are tried in.
        val resolver = AppSyncAuthModeResolver(multi())

        val modes = resolver.modesFor(
            modelRequest(
                publicRule(),
                fields = mapOf(
                    "title" to ModelField.builder()
                        .name("title")
                        .authRules(listOf(ownerRule()))
                        .build()
                )
            )
        )

        modes shouldContainExactly listOf(AppSyncAuthMode.USER_POOLS, AppSyncAuthMode.API_KEY)
    }

    @Test
    fun `each mode is paired with the authorizer that will sign for it`() {
        // The pairing is the point: resolving them together is what stops a caller being handed a mode
        // it has no authorizer for.
        val authorization = multi()
        val resolver = AppSyncAuthModeResolver(authorization)

        val candidates = resolver.resolve(modelRequest(ownerRule(), publicRule()))

        candidates.map { it.authorizer } shouldContainExactly listOf(
            authorization.authorizers.first { it.authMode == AppSyncAuthMode.USER_POOLS },
            authorization.authorizers.first { it.authMode == AppSyncAuthMode.API_KEY }
        )
    }

    @Test
    fun `rules that do not cover the operation are excluded`() {
        // A rule restricted to CREATE must not make its mode eligible for a READ.
        val resolver = AppSyncAuthModeResolver(multi())

        val modes = resolver.modesFor(
            modelRequest(
                privateRule(operations = listOf(ModelOperation.CREATE)),
                publicRule(),
                operation = ModelOperation.READ
            )
        )

        modes shouldNotContain AppSyncAuthMode.IAM
        modes shouldContainExactly listOf(AppSyncAuthMode.API_KEY)
    }

    // ── Per-request override ────────────────────────────────────────────

    @Test
    fun `a per-request override wins over the auth rules`() {
        val resolver = AppSyncAuthModeResolver(multi())

        val modes = resolver.modesFor(
            modelRequest(ownerRule(), publicRule(), authorizationType = AuthorizationType.AWS_IAM)
        )

        // Not just first — the only candidate. The caller was explicit, so falling back to another
        // identity would contradict them.
        modes shouldContainExactly listOf(AppSyncAuthMode.IAM)
    }

    @Test
    fun `a per-request override wins over the default in single auth`() {
        val resolver = AppSyncAuthModeResolver(
            AppSyncAuthorization.Multi(
                defaultAuthMode = AppSyncAuthMode.API_KEY,
                authorizers = listOf(
                    AppSyncClientAuthorizer.ApiKey("da2-fakekey"),
                    AppSyncClientAuthorizer.UserPools { "token" }
                )
            )
        )

        resolver.modesFor(
            modelRequest(authorizationType = AuthorizationType.AMAZON_COGNITO_USER_POOLS)
        ) shouldContainExactly listOf(AppSyncAuthMode.USER_POOLS)
    }

    @Test
    fun `an override naming an unconfigured mode is ignored rather than used`() {
        // Honouring it would mean sending an unauthorized request; falling back is the safe reading.
        val resolver = AppSyncAuthModeResolver(
            AppSyncAuthorization.Multi(
                defaultAuthMode = AppSyncAuthMode.API_KEY,
                authorizers = listOf(AppSyncClientAuthorizer.ApiKey("da2-fakekey"))
            )
        )

        resolver.modesFor(
            modelRequest(authorizationType = AuthorizationType.AWS_LAMBDA)
        ) shouldContainExactly listOf(AppSyncAuthMode.API_KEY)
    }

    // ── AuthorizationType mapping ───────────────────────────────────────

    @Test
    fun `every AuthorizationType except NONE maps to an auth mode`() {
        AuthorizationType.API_KEY.toAuthMode() shouldBe AppSyncAuthMode.API_KEY
        AuthorizationType.AWS_IAM.toAuthMode() shouldBe AppSyncAuthMode.IAM
        AuthorizationType.OPENID_CONNECT.toAuthMode() shouldBe AppSyncAuthMode.OIDC
        AuthorizationType.AMAZON_COGNITO_USER_POOLS.toAuthMode() shouldBe AppSyncAuthMode.USER_POOLS
        AuthorizationType.AWS_LAMBDA.toAuthMode() shouldBe AppSyncAuthMode.LAMBDA
        // NONE has no client equivalent: the client always authorizes a request.
        AuthorizationType.NONE.toAuthMode() shouldBe null
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun multi() = AppSyncAuthorization.Multi(
        defaultAuthMode = AppSyncAuthMode.API_KEY,
        authorizers = listOf(
            AppSyncClientAuthorizer.ApiKey("da2-fakekey"),
            AppSyncClientAuthorizer.UserPools { "user-pools-token" },
            AppSyncClientAuthorizer.Iam(StubCredentialsProvider),
            AppSyncClientAuthorizer.Oidc { "oidc-token" }
        )
    )

    private fun rawRequest(): GraphQLRequest<String> = SimpleGraphQLRequest(
        "query { getTodo { id } }",
        emptyMap(),
        String::class.java,
        GsonVariablesSerializer()
    )

    /**
     * The resolver reads exactly three things off a request: its model schema, its auth-rule
     * operation, and any per-request override. Building a real [AppSyncGraphQLRequest] would also run
     * selection-set generation, which needs a code-generated model class and has nothing to do with
     * auth resolution — so these three are mocked directly.
     */
    private fun modelRequest(
        vararg authRules: AuthRule,
        authorizationType: AuthorizationType? = null,
        operation: ModelOperation = ModelOperation.READ,
        fields: Map<String, ModelField> = emptyMap()
    ): AppSyncGraphQLRequest<String> = mockk {
        every { modelSchema } returns ModelSchema.builder()
            .name("Todo")
            .authRules(authRules.toList())
            .fields(fields)
            .build()
        every { authRuleOperation } returns operation
        every { this@mockk.authorizationType } returns authorizationType
    }

    /** Most of these tests care only about the modes and their order, not the paired authorizers. */
    private fun AppSyncAuthModeResolver.modesFor(request: GraphQLRequest<*>) = resolve(request).map { it.authMode }

    private fun ownerRule(ownerField: String = "owner") = AuthRule.builder()
        .authStrategy(AuthStrategy.OWNER)
        .authProvider(AuthStrategy.OWNER.defaultAuthProvider)
        .identityClaim("cognito:username")
        .ownerField(ownerField)
        .operations(ALL_OPERATIONS)
        .build()

    private fun privateRule(operations: List<ModelOperation> = ALL_OPERATIONS) = AuthRule.builder()
        .authStrategy(AuthStrategy.PRIVATE)
        .authProvider(AuthStrategy.Provider.IAM)
        .operations(operations)
        .build()

    private fun publicRule() = AuthRule.builder()
        .authStrategy(AuthStrategy.PUBLIC)
        .authProvider(AuthStrategy.Provider.API_KEY)
        .operations(ALL_OPERATIONS)
        .build()

    private companion object {
        val ALL_OPERATIONS = listOf(
            ModelOperation.CREATE,
            ModelOperation.UPDATE,
            ModelOperation.DELETE,
            ModelOperation.READ
        )

        val StubCredentialsProvider = AwsCredentialsProvider {
            AwsCredentials.Static(accessKeyId = "AKIAIOSFODNN7EXAMPLE", secretAccessKey = "secret")
        }
    }
}
