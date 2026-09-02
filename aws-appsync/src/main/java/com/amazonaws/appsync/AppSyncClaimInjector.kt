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
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.core.model.AuthRule
import com.amplifyframework.core.model.AuthStrategy
import com.amplifyframework.core.model.ModelOperation

/**
 * Adds the owner variable that owner-restricted subscriptions require.
 *
 * A model whose `@auth` rules restrict reads to an owner produces a subscription that AppSync expects
 * to carry that owner as a variable. The value comes from a claim in the caller's own token, so only
 * the client can supply it.
 *
 * Applies to subscriptions only. AppSync resolves the owner server-side for queries and mutations, so
 * they need nothing added.
 *
 * A private reimplementation of the plugin's `AuthRuleRequestDecorator`. It differs in one way worth
 * knowing: the plugin looks up a token from a registry of auth providers, whereas here the authorizer
 * already carries the token supplier, so the token is simply requested from it.
 */
internal class AppSyncClaimInjector {

    /**
     * Returns [request] with the owner variable added, or unchanged when the rules do not call for one.
     *
     * Safe to call for any request: the rules decide. A request that is not an [AppSyncGraphQLRequest]
     * carries no schema and is returned untouched.
     *
     * @param authMode The mode the subscription is being registered under. Which rules apply depends
     *   on it — a rule that permits public reads under an API key means no owner is needed.
     * @param authorizer The authorizer for [authMode], used to obtain the token the claims come from.
     */
    suspend fun <T> inject(
        request: GraphQLRequest<T>,
        authMode: AppSyncAuthMode,
        authorizer: AppSyncClientAuthorizer
    ): GraphQLRequest<T> {
        val appSyncRequest = request as? AppSyncGraphQLRequest<T> ?: return request
        val schema = appSyncRequest.modelSchema ?: return request

        var ownerRule: AuthRule? = null
        val groupsByClaim = mutableMapOf<String, MutableSet<String>>()

        for (rule in schema.authRules) {
            when {
                // This rule already permits the read without an owner, so nothing needs adding and the
                // remaining rules cannot change that.
                rule.permitsPublicRead(authMode) -> return request

                rule.isOwnerReadRule() -> {
                    if (ownerRule != null) {
                        // AppSync generates a separate variable per owner rule, so there is no single
                        // value to send. The plugin refuses this case too.
                        throw AppSyncAuthorizationClaimException(
                            message = "The model has more than one owner @auth rule restricting reads, " +
                                "so there is no single owner to send.",
                            recoverySuggestion = "Restrict the model to one owner rule covering READ."
                        )
                    }
                    ownerRule = rule
                }

                rule.isStaticGroupReadRule() ->
                    groupsByClaim.getOrPut(rule.groupClaimOrDefault) { mutableSetOf() }
                        .addAll(rule.groups)
            }
        }

        val owner = ownerRule ?: return request

        // Group membership takes precedence: a user who can already read via a group does not need the
        // owner filter, and adding it would narrow what they receive.
        val token = authorizer.tokenFor(authMode)
        val payload = AppSyncJwt.payload(token)
        if (groupsByClaim.any { (claim, permitted) ->
                AppSyncJwt.groupsClaim(payload, claim).any(permitted::contains)
            }
        ) {
            return request
        }

        val value = AppSyncJwt.stringClaim(payload, owner.identityClaimOrDefault)

        return try {
            appSyncRequest.newBuilder()
                .variable(owner.ownerFieldOrDefault, "String!", value)
                .build()
        } catch (error: Exception) {
            throw AppSyncValidationException(
                message = "The owner variable could not be added to the subscription request.",
                cause = error
            )
        }
    }

    /**
     * Obtains the token whose claims identify the owner.
     *
     * Only User Pools and OIDC carry claims. An API key or IAM credential identifies no user, so an
     * owner-restricted subscription cannot be authorized with one at all.
     */
    private suspend fun AppSyncClientAuthorizer.tokenFor(authMode: AppSyncAuthMode): String = when (this) {
        is AppSyncClientAuthorizer.UserPools -> fetchToken.fetch("User Pools token")
        is AppSyncClientAuthorizer.Oidc -> fetchToken.fetch("OIDC token")
        else -> throw AppSyncProviderNotConfiguredException(
            message = "The model restricts reads to an owner, which requires a token carrying claims, " +
                "but the subscription is authorized with $authMode.",
            recoverySuggestion = "Subscribe using Cognito User Pools or OpenID Connect."
        )
    }

    /**
     * Invokes a token supplier, translating any failure into a typed exception. A supplier is customer
     * code and can throw anything; letting that escape untyped would stop the caller recognising it as
     * an auth failure and trying another mode.
     */
    private suspend fun (suspend () -> String).fetch(description: String): String = try {
        this()
    } catch (error: Exception) {
        throw AppSyncTokenFetchException(
            message = "Fetching the $description failed.",
            cause = error
        )
    }

    private fun AuthRule.permitsPublicRead(authMode: AppSyncAuthMode): Boolean = authStrategy == AuthStrategy.PUBLIC &&
        authProvider == AuthStrategy.Provider.API_KEY &&
        authMode == AppSyncAuthMode.API_KEY &&
        coversRead()

    private fun AuthRule.isOwnerReadRule(): Boolean =
        authStrategy == AuthStrategy.OWNER && operationsOrDefault.contains(ModelOperation.READ)

    private fun AuthRule.isStaticGroupReadRule(): Boolean = authStrategy == AuthStrategy.GROUPS &&
        groups.isNotEmpty() &&
        operationsOrDefault.contains(ModelOperation.READ)

    private fun AuthRule.coversRead(): Boolean =
        operationsOrDefault.any { it == ModelOperation.LISTEN || it == ModelOperation.READ }
}
