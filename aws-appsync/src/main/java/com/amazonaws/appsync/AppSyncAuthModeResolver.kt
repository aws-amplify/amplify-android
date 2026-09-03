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
import com.amplifyframework.api.aws.MultiAuthModeStrategy
import com.amplifyframework.api.graphql.GraphQLRequest

/**
 * Decides which auth modes a request may be attempted with, and in what order.
 *
 * Rule inspection and priority ordering are delegated to [MultiAuthModeStrategy] and its
 * `MultiAuthorizationTypeIterator`, which sort by auth strategy then provider precedence and
 * de-duplicate. This class bridges their [AuthorizationType] output to [AppSyncAuthMode] and drops any
 * mode with no configured authorizer.
 *
 * Precedence, highest first:
 * 1. A per-request override on the request.
 * 2. The model's `@auth` rules, in priority order.
 * 3. The configured default.
 */
internal class AppSyncAuthModeResolver(private val authorization: AppSyncAuthorization) {

    /**
     * Returns the auth modes to try, in order. Never empty: it falls back to the configured default,
     * so a caller always has something to attempt.
     */
    fun resolve(request: GraphQLRequest<*>): List<AppSyncAuthMode> {
        val default = authorization.resolveDefaultAuthMode()

        // A per-request override wins outright — the caller has been explicit, so falling back to
        // other modes would contradict them.
        requestOverride(request)?.let { return listOf(it) }

        // Single-auth has exactly one authorizer, so there is nothing to order.
        if (authorization is AppSyncAuthorization.Single) return listOf(default)

        val fromAuthRules = authRuleModes(request).filter { authorization.authorizerFor(it) != null }

        return fromAuthRules.ifEmpty { listOf(default) }
    }

    private fun requestOverride(request: GraphQLRequest<*>): AppSyncAuthMode? {
        val override = (request as? AppSyncGraphQLRequest<*>)?.authorizationType?.toAuthMode() ?: return null
        // An override naming a mode with no authorizer is a configuration error, not a reason to
        // silently use a different identity than the caller asked for.
        return override.takeIf { authorization.authorizerFor(it) != null }
    }

    /**
     * The modes the model's `@auth` rules allow, in priority order. Empty when the request carries no
     * schema — a raw request has no rules to inspect.
     */
    private fun authRuleModes(request: GraphQLRequest<*>): List<AppSyncAuthMode> {
        val appSyncRequest = request as? AppSyncGraphQLRequest<*> ?: return emptyList()
        val schema = appSyncRequest.modelSchema ?: return emptyList()
        val operation = appSyncRequest.authRuleOperation ?: return emptyList()

        val iterator = MultiAuthModeStrategy.getInstance().authTypesFor(schema, operation)

        return buildList {
            while (iterator.hasNext()) {
                iterator.next().toAuthMode()?.let(::add)
            }
        }.distinct()
    }
}

/**
 * Bridges [AuthorizationType] to the client's [AppSyncAuthMode].
 *
 * Returns null for [AuthorizationType.NONE], which has no client equivalent: the client always
 * authorizes a request, so an unauthenticated mode is not a candidate it can attempt.
 */
internal fun AuthorizationType.toAuthMode(): AppSyncAuthMode? = when (this) {
    AuthorizationType.API_KEY -> AppSyncAuthMode.API_KEY
    AuthorizationType.AWS_IAM -> AppSyncAuthMode.IAM
    AuthorizationType.OPENID_CONNECT -> AppSyncAuthMode.OIDC
    AuthorizationType.AMAZON_COGNITO_USER_POOLS -> AppSyncAuthMode.USER_POOLS
    AuthorizationType.AWS_LAMBDA -> AppSyncAuthMode.LAMBDA
    AuthorizationType.NONE -> null
}
