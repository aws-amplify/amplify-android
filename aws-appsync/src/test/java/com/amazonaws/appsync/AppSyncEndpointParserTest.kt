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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * Tests [AppSyncEndpointParser].
 *
 * The partition matrix is the point of this class. The regex this parser replaced assumed a
 * `.amazonaws.com` suffix, so it produced a realtime host that does not resolve for China endpoints,
 * which end in `.amazonaws.com.cn`.
 */
class AppSyncEndpointParserTest {

    private fun parsed(endpoint: String) = (AppSyncEndpointParser.parse(endpoint) as Result.Success).data

    // ── Partition matrix (§5.4) ─────────────────────────────────────────

    @Test
    fun `commercial partition`() {
        val endpoint = parsed("https://abc123.appsync-api.us-east-1.amazonaws.com/graphql")

        endpoint.region shouldBe "us-east-1"
        endpoint.host shouldBe "abc123.appsync-api.us-east-1.amazonaws.com"
        endpoint.realtimeHost shouldBe "abc123.appsync-realtime-api.us-east-1.amazonaws.com"
        endpoint.dnsSuffix shouldBe "amazonaws.com"
    }

    @Test
    fun `china cn-north-1 keeps the com-cn suffix in the realtime host`() {
        val endpoint = parsed("https://abc123.appsync-api.cn-north-1.amazonaws.com.cn/graphql")

        endpoint.region shouldBe "cn-north-1"
        endpoint.realtimeHost shouldBe "abc123.appsync-realtime-api.cn-north-1.amazonaws.com.cn"
        endpoint.dnsSuffix shouldBe "amazonaws.com.cn"
    }

    @Test
    fun `china cn-northwest-1 keeps the com-cn suffix in the realtime host`() {
        val endpoint = parsed("https://abc123.appsync-api.cn-northwest-1.amazonaws.com.cn/graphql")

        endpoint.region shouldBe "cn-northwest-1"
        endpoint.realtimeHost shouldBe "abc123.appsync-realtime-api.cn-northwest-1.amazonaws.com.cn"
        endpoint.dnsSuffix shouldBe "amazonaws.com.cn"
    }

    @Test
    fun `govcloud`() {
        val endpoint = parsed("https://abc123.appsync-api.us-gov-west-1.amazonaws.com/graphql")

        endpoint.region shouldBe "us-gov-west-1"
        endpoint.realtimeHost shouldBe "abc123.appsync-realtime-api.us-gov-west-1.amazonaws.com"
        endpoint.dnsSuffix shouldBe "amazonaws.com"
    }

    @Test
    fun `a region launched after this ships still parses`() {
        // Structural, not an allowlist — this must not need updating per region launch.
        parsed("https://abc123.appsync-api.ap-southeast-7.amazonaws.com/graphql").region shouldBe
            "ap-southeast-7"
    }

    // ── URL shapes ──────────────────────────────────────────────────────

    @Test
    fun `tolerates a missing scheme, a port, a query string and a fragment`() {
        listOf(
            "abc123.appsync-api.eu-west-2.amazonaws.com/graphql",
            "https://abc123.appsync-api.eu-west-2.amazonaws.com:443/graphql",
            "https://abc123.appsync-api.eu-west-2.amazonaws.com/graphql?trace=1",
            "https://abc123.appsync-api.eu-west-2.amazonaws.com/graphql#frag"
        ).forEach { parsed(it).region shouldBe "eu-west-2" }
    }

    @Test
    fun `host casing is normalised`() {
        parsed("https://ABC123.AppSync-Api.US-EAST-1.amazonaws.com/graphql").region shouldBe "us-east-1"
    }

    // ── Failures ────────────────────────────────────────────────────────

    @Test
    fun `a custom domain fails, because it carries no region`() {
        val result = AppSyncEndpointParser.parse("https://api.example.com/graphql")

        result.shouldBeInstanceOf<Result.Failure<AppSyncEndpointResolutionException>>()
        result.error.shouldBeInstanceOf<AppSyncConfigurationException>()
        result.error.message shouldContain "appsync-api"
    }

    @Test
    fun `a non-region label where the region belongs fails`() {
        val result = AppSyncEndpointParser.parse("https://abc123.appsync-api.notaregion.amazonaws.com/graphql")

        result.shouldBeInstanceOf<Result.Failure<AppSyncEndpointResolutionException>>()
        result.error.message shouldContain "not a valid AWS region"
    }

    @Test
    fun `an endpoint with no apiId label before appsync-api fails`() {
        AppSyncEndpointParser.parse("https://appsync-api.us-east-1.amazonaws.com/graphql")
            .shouldBeInstanceOf<Result.Failure<AppSyncEndpointResolutionException>>()
    }

    @Test
    fun `an endpoint with nothing after the region fails`() {
        AppSyncEndpointParser.parse("https://abc123.appsync-api.us-east-1/graphql")
            .shouldBeInstanceOf<Result.Failure<AppSyncEndpointResolutionException>>()
    }

    @Test
    fun `an empty endpoint fails rather than throwing`() {
        AppSyncEndpointParser.parse("")
            .shouldBeInstanceOf<Result.Failure<AppSyncEndpointResolutionException>>()
    }
}
