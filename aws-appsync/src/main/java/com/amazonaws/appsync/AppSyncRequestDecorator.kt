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

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignedBodyHeader
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.http.DeferredHeaders
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.net.url.UrlEncoding
import com.amplifyframework.foundation.credentials.toSmithyProvider
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer

/**
 * Applies an [AppSyncClientAuthorizer]'s credentials to an outbound HTTP request.
 *
 * Every credential source is itself a suspend function, so all five auth modes are covered by a single
 * `when` rather than one class per mode.
 *
 * @param region The signing region, resolved by [AppSyncEndpointParser] or set explicitly.
 */
internal class AppSyncRequestDecorator(private val region: String) {

    /**
     * Returns [request] with the authorizer's credentials attached.
     *
     * @throws AppSyncTokenFetchException if a token or API key supplier fails.
     * @throws AppSyncSigningException if SigV4 signing fails.
     */
    suspend fun decorate(request: Request, authorizer: AppSyncClientAuthorizer): Request = when (authorizer) {
        is AppSyncClientAuthorizer.ApiKey ->
            request.withHeader(API_KEY_HEADER, authorizer.fetchApiKey.fetch("API key"))

        is AppSyncClientAuthorizer.UserPools ->
            request.withHeader(AUTHORIZATION_HEADER, authorizer.fetchToken.fetch("User Pools token"))

        is AppSyncClientAuthorizer.Oidc ->
            request.withHeader(AUTHORIZATION_HEADER, authorizer.fetchToken.fetch("OIDC token"))

        is AppSyncClientAuthorizer.Lambda ->
            request.withHeader(AUTHORIZATION_HEADER, authorizer.fetchToken.fetch("Lambda authorization token"))

        is AppSyncClientAuthorizer.Iam -> request.signed(authorizer)
    }

    private fun Request.withHeader(name: String, value: String) = newBuilder().header(name, value).build()

    /**
     * Invokes a credential supplier, translating any failure into a typed exception. A supplier is
     * customer code, so it can throw anything.
     *
     * Cancellation is rethrown rather than translated: on the JVM `CancellationException` is an
     * `Exception`, so without this a cancelled caller would be reported as a failed token fetch.
     */
    private suspend fun (suspend () -> String).fetch(description: String): String = try {
        this()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        throw AppSyncTokenFetchException(
            message = "Fetching the $description failed.",
            cause = error
        )
    }

    /**
     * Signs the request with SigV4.
     *
     * Calls the suspending smithy signer directly. This path suspends all the way down, so the signer
     * needs no blocking wrapper.
     *
     * `DefaultAwsSigner` is `@InternalApi` in smithy-kotlin, so the opt-in is unavoidable: smithy
     * exposes no public signer entry point.
     */
    @OptIn(InternalApi::class)
    private suspend fun Request.signed(authorizer: AppSyncClientAuthorizer.Iam): Request {
        val bodyBytes = body.toByteArray()

        val signable = HttpRequest(
            method = HttpMethod.parse(method),
            url = Url.parse(url.toUri().toString(), UrlEncoding.All),
            headers = Headers {
                this@signed.headers.names().forEach { name ->
                    appendAll(name, this@signed.headers.values(name))
                }
                // The signature covers Host, so it must be present before signing.
                set(HOST_HEADER, this@signed.url.host)
            },
            body = HttpBody.fromBytes(bodyBytes),
            trailingHeaders = DeferredHeaders.Empty
        )

        val signed = try {
            val config = AwsSigningConfig {
                region = this@AppSyncRequestDecorator.region
                service = APPSYNC_SERVICE_NAME
                credentials = authorizer.credentialsProvider.toSmithyProvider().resolve()
                useDoubleUriEncode = true
                signedBodyHeader = AwsSignedBodyHeader.X_AMZ_CONTENT_SHA256
            }
            DefaultAwsSigner.sign(signable, config).output
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            throw AppSyncSigningException(
                message = "Signing the request with SigV4 failed.",
                cause = error
            )
        }

        // Rebuild the OkHttp request from the signed one. The signed Content-Type is read back out of
        // the headers and reapplied through the body's MediaType, because OkHttp takes Content-Type
        // from the body rather than from a header set on the builder.
        val builder = Request.Builder().url(url)
        var contentType = DEFAULT_CONTENT_TYPE
        signed.headers.entries().forEach { (name, values) ->
            values.forEach { value -> builder.addHeader(name, value) }
            if (name.equals(CONTENT_TYPE_HEADER, ignoreCase = true)) {
                values.firstOrNull()?.let { contentType = it }
            }
        }

        val mediaType = contentType.toMediaTypeOrDefault()
        return builder.method(method, body?.let { bodyBytes.toRequestBody(mediaType) }).build()
    }

    private fun RequestBody?.toByteArray(): ByteArray {
        if (this == null) return ByteArray(0)
        return try {
            Buffer().also { writeTo(it) }.readByteArray()
        } catch (error: Exception) {
            throw AppSyncSigningException(
                message = "The request body could not be read, so the SigV4 signature could not be computed.",
                cause = error
            )
        }
    }

    private fun String.toMediaTypeOrDefault() = runCatching { toMediaType() }
        .getOrElse { DEFAULT_CONTENT_TYPE.toMediaType() }

    private companion object {
        const val API_KEY_HEADER = "x-api-key"
        const val AUTHORIZATION_HEADER = "authorization"
        const val HOST_HEADER = "Host"
        const val CONTENT_TYPE_HEADER = "content-type"
        const val DEFAULT_CONTENT_TYPE = "application/json"
        const val APPSYNC_SERVICE_NAME = "appsync"
    }
}
