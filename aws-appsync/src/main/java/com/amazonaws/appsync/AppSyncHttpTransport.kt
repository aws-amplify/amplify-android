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

import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.util.UserAgent
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Executes GraphQL queries and mutations over HTTP.
 *
 * A private reimplementation of the plugin's `AppSyncGraphQLOperation`, differing in three ways: it
 * suspends rather than taking callbacks, it is cancellable through the calling coroutine, and its
 * failures are typed [AppSyncException]s rather than `ApiException`.
 *
 * @param endpoint The AppSync GraphQL endpoint URL.
 * @param client The OkHttp client to issue requests with.
 * @param authorization The client's authorizer configuration.
 * @param decorator Applies credentials to each request.
 */
internal class AppSyncHttpTransport(
    private val endpoint: String,
    private val client: OkHttpClient,
    private val authorization: AppSyncAuthorization,
    private val decorator: AppSyncRequestDecorator
) {

    /**
     * Sends [request] and deserializes the response.
     *
     * Always uses the default authorizer. TODO: honour per-request auth overrides and `@auth`-rule
     * resolution.
     */
    suspend fun <T> execute(request: GraphQLRequest<T>): GraphQLResponse<T> {
        val httpRequest = Request.Builder()
            .url(endpoint)
            .header(ACCEPT_HEADER, CONTENT_TYPE)
            .header(USER_AGENT_HEADER, UserAgent.string())
            .post(request.content.toRequestBody(CONTENT_TYPE.toMediaType()))
            .build()

        val decorated = decorator.decorate(httpRequest, authorization.defaultAuthorizer)
        val response = client.newCall(decorated).await()

        return response.use { deserialize(request, it) }
    }

    private fun <T> deserialize(request: GraphQLRequest<T>, response: Response): GraphQLResponse<T> {
        val body = try {
            response.body?.string()
        } catch (error: IOException) {
            throw AppSyncDeserializationException(
                message = "The response body could not be read.",
                cause = error
            )
        }

        // AppSync reports GraphQL-level problems with a 4xx and a GraphQL error body. Deserializing it
        // yields the errors themselves, which is far more useful than the status code alone. This is
        // where the plugin collapses everything into a generic ApiException.
        if (response.code in CLIENT_ERROR_CODES) {
            throw clientError(request, response, body)
        }

        if (response.code >= SERVER_ERROR_MIN) {
            throw AppSyncNetworkException(
                message = "The request failed with HTTP status ${response.code}.",
                recoverySuggestion = "This is usually transient. Retry the request."
            )
        }

        if (!response.isSuccessful) {
            // A 3xx reaches here when redirects are disabled on the OkHttp client, or on a redirect
            // OkHttp will not follow. Retrying produces the same response, so it is not suggested.
            throw AppSyncNetworkException(
                message = "The request returned an unexpected HTTP status ${response.code}.",
                recoverySuggestion = "Verify the endpoint URL and any redirect handling configured on " +
                    "the OkHttp client."
            )
        }

        return AppSyncResponseDeserializer.deserialize(request, body)
    }

    private fun <T> clientError(request: GraphQLRequest<T>, response: Response, body: String?): AppSyncException {
        // TODO: classify authorization failures as an AppSyncAuthException. A 401, or an error whose
        //  extensions carry an Unauthorized errorType, currently arrives as a response or request
        //  error, so a caller cannot match the whole auth category to re-authenticate.
        val errors = runCatching { AppSyncResponseDeserializer.deserialize(request, body).errors }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }

        return if (errors != null) {
            AppSyncGraphQLErrorException(
                message = "The request failed with HTTP status ${response.code}: " +
                    errors.joinToString("; ") { it.message },
                errors = errors
            )
        } else {
            AppSyncValidationException(
                message = "The request was rejected with HTTP status ${response.code}.",
                recoverySuggestion = "Check the request document and variables, and the API's auth configuration."
            )
        }
    }

    /**
     * Bridges an OkHttp [Call] to a coroutine. Cancelling the coroutine cancels the call, so a
     * cancelled `query` does not leave a request in flight.
     */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) = continuation.resume(response)

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(
                        AppSyncNetworkException(
                            message = e.message ?: "The request could not reach the AppSync endpoint.",
                            cause = e
                        )
                    )
                }
            }
        )
        continuation.invokeOnCancellation { runCatching { cancel() } }
    }

    private companion object {
        const val CONTENT_TYPE = "application/json"
        const val ACCEPT_HEADER = "accept"
        const val USER_AGENT_HEADER = "User-Agent"
        val CLIENT_ERROR_CODES = 400..499
        const val SERVER_ERROR_MIN = 500
    }
}
