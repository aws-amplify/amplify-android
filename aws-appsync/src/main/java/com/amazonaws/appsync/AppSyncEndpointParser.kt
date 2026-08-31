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

import com.amplifyframework.foundation.result.Result

/**
 * An AppSync GraphQL endpoint, decomposed into the parts the client needs.
 *
 * The realtime host is derived rather than assumed, because it differs by partition: in the standard
 * partitions the `appsync-api` label becomes `appsync-realtime-api`, but China endpoints end in
 * `.amazonaws.com.cn` rather than `.amazonaws.com`, so a parser that assumes a `.amazonaws.com`
 * suffix produces a host that does not resolve.
 *
 * @param region The AWS region the endpoint is in.
 * @param host The HTTP host, used for queries and mutations.
 * @param realtimeHost The WebSocket host, used for subscriptions.
 * @param dnsSuffix The partition's DNS suffix, e.g. `amazonaws.com` or `amazonaws.com.cn`.
 */
internal data class AppSyncEndpoint(
    val region: String,
    val host: String,
    val realtimeHost: String,
    val dnsSuffix: String
)

/**
 * Parses AppSync GraphQL endpoint URLs into an [AppSyncEndpoint].
 *
 * Replaces the unanchored region regex this client started with. That regex did extract the right
 * region for China endpoints by accident, since it was unanchored — but it gave no realtime host and
 * no partition, both of which subscriptions and SigV4 signing need.
 *
 * Handles the standard AppSync form:
 * ```
 * https://{apiId}.appsync-api.{region}.{dnsSuffix}/graphql
 * ```
 * across the commercial, China and GovCloud partitions. A custom domain does not carry a region in
 * its host, so [parse] fails for one and the caller must supply the region explicitly.
 */
internal object AppSyncEndpointParser {

    private const val API_LABEL = "appsync-api"
    private const val REALTIME_LABEL = "appsync-realtime-api"

    /**
     * Parses [endpoint].
     *
     * @return the parsed endpoint, or a failure describing why it could not be parsed.
     */
    fun parse(endpoint: String): Result<AppSyncEndpoint, AppSyncEndpointResolutionException> {
        val host = hostOf(endpoint)
            ?: return failure("Could not read a host from the endpoint URL '$endpoint'.")

        // {apiId}.appsync-api.{region}.{dnsSuffix...}
        val labels = host.split('.')
        val apiLabelIndex = labels.indexOf(API_LABEL)

        if (apiLabelIndex < 1) {
            return failure(
                "The endpoint host '$host' is not a standard AppSync endpoint: expected a " +
                    "'$API_LABEL' label, as in {apiId}.$API_LABEL.{region}.amazonaws.com."
            )
        }

        val regionIndex = apiLabelIndex + 1
        if (regionIndex >= labels.lastIndex) {
            return failure("The endpoint host '$host' has no region label after '$API_LABEL'.")
        }

        val region = labels[regionIndex]
        if (!isRegionLike(region)) {
            return failure("'$region' in the endpoint host '$host' is not a valid AWS region.")
        }

        val dnsSuffix = labels.drop(regionIndex + 1).joinToString(".")
        if (dnsSuffix.isEmpty()) {
            return failure("The endpoint host '$host' has no DNS suffix after the region.")
        }

        // Swap only the api label, preserving apiId, region and the partition's suffix. This is what
        // keeps China (amazonaws.com.cn) and GovCloud working without special-casing either.
        val realtimeLabels = labels.toMutableList()
        realtimeLabels[apiLabelIndex] = REALTIME_LABEL

        return Result.Success(
            AppSyncEndpoint(
                region = region,
                host = host,
                realtimeHost = realtimeLabels.joinToString("."),
                dnsSuffix = dnsSuffix
            )
        )
    }

    /**
     * Resolves just the region from [endpoint], for callers that only need that.
     *
     * @return the region, or null if the endpoint is not a standard AppSync URL.
     */
    fun regionOrNull(endpoint: String): String? = (parse(endpoint) as? Result.Success)?.data?.region

    private fun hostOf(endpoint: String): String? {
        val withoutScheme = endpoint.substringAfter("://", missingDelimiterValue = endpoint)
        val host = withoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':') // strip any port
            .lowercase()
        return host.ifEmpty { null }
    }

    // Region labels are of the form {partition}-{area}-{number}, e.g. us-east-1, cn-north-1,
    // us-gov-west-1, ap-southeast-4. Deliberately structural rather than an allowlist, so a region
    // launched after this ships still parses.
    private fun isRegionLike(candidate: String): Boolean = Regex("""^[a-z]{2,}(-[a-z]+)+-\d+$""").matches(candidate)

    private fun failure(message: String) = Result.Failure(AppSyncEndpointResolutionException(message))
}
