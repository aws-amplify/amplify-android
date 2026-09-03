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
 * pin is the part this class owns: precedence between a per-request override,
 * the model's `@auth` rules and the configured default, and that a mode with no configured authorizer
 * is never proposed.
 */
class AppSyncAuthModeResolverTest {

    // ── Single auth ─────────────────────────────────────────────────────

    @Test
    fun `single auth resolves to its one mode`() {
        val resolver = AppSyncAuthModeResolver(
            AppSyncAuthorization.Single(AppSyncClientAuthorizer.ApiKey("da2-fakekey"))
        )

        resolver.resolve(rawRequest()) shouldContainExactly listOf(AppSyncAuthMode.API_KEY)
    }

    @Test
    fun `single auth ignores auth rules, because there is only one authorizer to use`() {
        val resolver = AppSyncAuthModeResolver(
            AppSyncAuthorization.Single(AppSyncClientAuthorizer.ApiKey("da2-fakekey"))
        )

        resolver.resolve(modelRequest(ownerRule(), publicRule())) shouldContainExactly
            listOf(AppSyncAuthMode.API_KEY)
    }

    // ── Multi auth from @auth rules ─────────────────────────────────────

    @Test
    fun `multi auth orders modes by auth rule priority`() {
        // OWNER(2) before PRIVATE(4) before PUBLIC(5), per AuthStrategy priorities.
        val resolver = AppSyncAuthModeResolver(multi())

        resolver.resolve(modelRequest(publicRule(), privateRule(), ownerRule())) shouldContainExactly
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

        val modes = resolver.resolve(modelRequest(privateRule(), publicRule()))

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

        resolver.resolve(modelRequest(ownerRule())) shouldContainExactly listOf(AppSyncAuthMode.LAMBDA)
    }

    @Test
    fun `a raw request has no schema, so multi auth falls back to the default`() {
        val resolver = AppSyncAuthModeResolver(multi())

        resolver.resolve(rawRequest()) shouldContainExactly listOf(AppSyncAuthMode.API_KEY)
    }

    @Test
    fun `duplicate providers across rules yield one mode each`() {
        // Two owner rules both resolve to User Pools; retrying the same mode twice is pointless.
        val resolver = AppSyncAuthModeResolver(multi())

        resolver.resolve(modelRequest(ownerRule(), ownerRule(ownerField = "editor"))) shouldContainExactly
            listOf(AppSyncAuthMode.USER_POOLS)
    }

    @Test
    fun `rules that do not cover the operation are excluded`() {
        // A rule restricted to CREATE must not make its mode eligible for a READ.
        val resolver = AppSyncAuthModeResolver(multi())

        val modes = resolver.resolve(
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

        val modes = resolver.resolve(
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

        resolver.resolve(
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

        resolver.resolve(
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
        operation: ModelOperation = ModelOperation.READ
    ): AppSyncGraphQLRequest<String> = mockk {
        every { modelSchema } returns ModelSchema.builder()
            .name("Todo")
            .authRules(authRules.toList())
            .build()
        every { authRuleOperation } returns operation
        every { this@mockk.authorizationType } returns authorizationType
    }

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
