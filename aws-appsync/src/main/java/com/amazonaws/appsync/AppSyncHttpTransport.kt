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
 * Suspends rather than taking callbacks, cancels along with the calling coroutine, and reports every
 * failure as a typed [AppSyncException].
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
    private val decorator: AppSyncRequestDecorator,
    private val authModeResolver: AppSyncAuthModeResolver = AppSyncAuthModeResolver(authorization)
) {

    /**
     * Sends [request], retrying with the next eligible auth mode when one fails for an auth reason.
     *
     * The candidate modes and their order come from [AppSyncAuthModeResolver]. With a single candidate
     * this behaves exactly as the single-auth path did: the underlying failure is surfaced directly
     * rather than wrapped, because wrapping one failure in "auth exhausted" hides it for no benefit.
     */
    suspend fun <T> execute(request: GraphQLRequest<T>): GraphQLResponse<T> {
        val authModes = authModeResolver.resolve(request)
        // With one candidate there is nothing to fall back to, so auth failures are reported as they
        // were before multi-auth existed: the real error, or a response carrying its own errors.
        val canFallBack = authModes.size > 1
        var lastAuthFailure: AppSyncException? = null

        authModes.forEachIndexed { index, authMode ->
            val isLastAttempt = index == authModes.lastIndex

            val authorizer = authorization.authorizerFor(authMode)
                ?: throw AppSyncProviderNotConfiguredException(
                    message = "No authorizer is configured for auth mode $authMode."
                )

            val decorated = try {
                decorate(request, authorizer)
            } catch (error: AppSyncAuthException) {
                // Credentials for this mode could not be obtained. Another mode may still work, so
                // this is only terminal once the candidates run out.
                if (isLastAttempt) throw exhausted(authModes, error)
                lastAuthFailure = error
                return@forEachIndexed
            }

            // A rejection can arrive either as a thrown 4xx or as a 200 carrying Unauthorized errors,
            // and both mean the same thing: this identity was refused. Only the second was considered
            // before, so a plain 401 propagated straight out and no fallback was ever attempted.
            val response = try {
                client.newCall(decorated).await().use { deserialize(request, it) }
            } catch (rejected: AppSyncUnauthorizedException) {
                if (!canFallBack) throw rejected
                if (isLastAttempt) throw exhausted(authModes, rejected)
                lastAuthFailure = rejected
                return@forEachIndexed
            }

            // An unauthorized response is the other retryable signal: AppSync accepted the request but
            // rejected the identity, which a different mode may satisfy.
            if (canFallBack && response.hasUnauthorizedError()) {
                lastAuthFailure = AppSyncGraphQLErrorException(
                    message = "Authorization failed with $authMode.",
                    errors = response.errors
                )
                if (isLastAttempt) throw exhausted(authModes, lastAuthFailure)
                return@forEachIndexed
            }

            return response
        }

        // Unreachable in practice: the loop either returns or throws on its last iteration. Kept so
        // the function is total rather than relying on that reasoning holding after an edit.
        throw exhausted(authModes, lastAuthFailure)
    }

    private suspend fun <T> decorate(request: GraphQLRequest<T>, authorizer: AppSyncClientAuthorizer): Request {
        val httpRequest = Request.Builder()
            .url(endpoint)
            .header(ACCEPT_HEADER, CONTENT_TYPE)
            .header(USER_AGENT_HEADER, UserAgent.string())
            .post(request.content.toRequestBody(CONTENT_TYPE.toMediaType()))
            .build()

        return decorator.decorate(httpRequest, authorizer)
    }

    private fun exhausted(attempted: List<AppSyncAuthMode>, cause: AppSyncException?): AppSyncException {
        // With one candidate there is nothing to be exhausted, so the real failure is more useful.
        if (attempted.size <= 1) {
            return cause ?: AppSyncProviderNotConfiguredException(
                message = "No auth mode was available to authorize the request."
            )
        }
        return AppSyncAuthExhaustedException(
            message = "The request failed with every eligible auth mode: ${attempted.joinToString()}.",
            attemptedAuthModes = attempted,
            cause = cause
        )
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

        // AppSync reports GraphQL-level problems with a 4xx carrying a GraphQL error body. Deserializing
        // it yields the errors themselves, which say far more than the status code alone.
        if (response.code in CLIENT_ERROR_CODES) {
            throw clientError(request, response, body)
        }

        if (response.code >= SERVER_ERROR_MIN) {
            // Only some 5xx statuses describe a condition that can clear on its own. Advising a retry
            // for the rest — 501 and 505, say — sends a caller round a loop that cannot succeed.
            val transient = response.code in TRANSIENT_SERVER_ERROR_CODES
            throw AppSyncNetworkException(
                message = "The request failed with HTTP status ${response.code}.",
                recoverySuggestion = if (transient) {
                    "This is usually transient. Retry the request."
                } else {
                    "Verify that the endpoint URL addresses an AppSync API and that the request is a " +
                        "POST of a GraphQL document."
                }
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
        val parsed = runCatching { AppSyncResponseDeserializer.deserialize(request, body) }.getOrNull()
        val errors = parsed?.errors?.takeIf { it.isNotEmpty() }

        // A throttle is not a defect in the request, so it must not arrive as one — the advice to check
        // the document and variables would send a caller to correct something that is already correct.
        if (response.code == HTTP_TOO_MANY_REQUESTS || parsed?.hasRateLimitError() == true) {
            return AppSyncRateLimitExceededException(
                message = "The request was throttled (HTTP status ${response.code})" +
                    (errors?.joinToString("; ") { it.message }?.let { ": $it" } ?: ".")
            )
        }

        // Reported as an auth failure so a caller can match the whole category to re-authenticate,
        // rather than having to recognise a status code or an error string. Either signal is enough:
        // AppSync answers a rejected identity with a 401, and a rejected operation with an
        // Unauthorized error type that can arrive under any 4xx.
        if (response.code == HTTP_UNAUTHORIZED || parsed?.hasUnauthorizedError() == true) {
            return AppSyncUnauthorizedException(
                message = "The request was not authorized (HTTP status ${response.code})" +
                    (errors?.joinToString("; ") { it.message }?.let { ": $it" } ?: "."),
                errors = errors ?: emptyList()
            )
        }

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
                // The response is closed by the resume callback when the continuation has already been
                // cancelled. A resume into a cancelled continuation is discarded, so the caller never
                // reaches the `use` block that would otherwise close the body, and the connection leaks.
                override fun onResponse(call: Call, response: Response) =
                    continuation.resume(response) { _, closed, _ -> closed.close() }

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
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val SERVER_ERROR_MIN = 500

        // Internal error, bad gateway, service unavailable and gateway timeout. Each can clear without
        // the request changing, so a retry is worth suggesting.
        val TRANSIENT_SERVER_ERROR_CODES = setOf(500, 502, 503, 504)
    }
}
