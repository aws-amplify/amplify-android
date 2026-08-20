/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.apollo.appsync.authorizers

import com.amplifyframework.apollo.appsync.AppSyncAuthorizer
import com.amplifyframework.apollo.appsync.AppSyncEndpoint
import com.amplifyframework.apollo.appsync.toJson
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.http.ByteStringHttpBody
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * [AppSyncAuthorizer] implementation that uses IAM policies for authorization. This authorizer delegates to the
 * [generateSignatureHeaders] function to generate a signature to add to each request.
 * The signature generation should use AWS SigV4 signing. There is an implementation of this signing logic in the
 * apollo-appsync-amplify library, or you can use the AWS SDK for Kotlin, or any other SigV4 implementation.
 * @param generateSignatureHeaders The delegate that performs the signing. It should return all the headers from the
 * signed request.
 */
class IamAuthorizer(private val generateSignatureHeaders: suspend (HttpRequest) -> Map<String, String>) :
    AppSyncAuthorizer {

    constructor(signatureProvider: SignatureProvider) : this({ request ->
        suspendCoroutine { continuation ->
            signatureProvider.generateSignatureHeaders(
                request,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    })

    // See IamRequestDecorator
    override suspend fun getHttpAuthorizationHeaders(request: HttpRequest) = generateSignatureHeaders(request)

    // See SubscriptionAuthorizer.forIam
    override suspend fun getWebsocketConnectionHeaders(endpoint: AppSyncEndpoint): Map<String, String> {
        val request = createHttpRequestForSigning(
            url = endpoint.realtime.toString() + "/connect",
            host = endpoint.serverUrl.host,
            json = "{}"
        )
        return generateSignatureHeaders(request)
    }

    // See SubscriptionAuthorizer.forIam
    override suspend fun getWebSocketSubscriptionPayload(
        endpoint: AppSyncEndpoint,
        request: ApolloRequest<*>
    ): Map<String, String> {
        val json = request.toJson()
        val httpRequest = createHttpRequestForSigning(
            url = endpoint.serverUrl.toString(),
            host = endpoint.serverUrl.host,
            json = json
        )
        return generateSignatureHeaders(httpRequest)
    }

    private fun createHttpRequestForSigning(url: String, host: String, json: String) =
        HttpRequest.Builder(HttpMethod.Post, url)
            .addHeader("accept", "application/json, text/javascript")
            .addHeader("content-type", "application/json; charset=UTF-8")
            .addHeader("host", host)
            .body(ByteStringHttpBody("application/json", json))
            .build()

    /**
     * Provide signature data using a callback-based API. This is primarily intended for Java consumption.
     */
    fun interface SignatureProvider {
        fun generateSignatureHeaders(
            request: HttpRequest,
            onSuccess: Consumer<Map<String, String>>,
            onError: Consumer<Throwable>
        )
    }
}
