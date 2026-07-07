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
package com.amplifyframework.connect.internal

import com.amplifyframework.connect.ConnectAccessDeniedException
import com.amplifyframework.connect.ConnectNetworkException
import com.amplifyframework.connect.ConnectNotSignedInException
import com.amplifyframework.connect.ConnectServiceException
import com.amplifyframework.connect.ConnectSession
import com.amplifyframework.connect.ConnectThrottlingException
import com.amplifyframework.connect.ConnectValidationException
import com.amplifyframework.foundation.credentials.AwsCredentials
import java.net.URL
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Sends identify requests to the Customer Profiles endpoint.
 *
 * Selects the route and authorization from the resolved [ConnectSession]:
 *  - Authenticated: `POST {endpoint}/identify-user` with a bearer token.
 *  - Guest: `POST {endpoint}/identify-user-guest`, SigV4-signed for
 *    `execute-api` with the guest credentials.
 */
internal class IdentifyUserService(
    private val endpoint: String,
    private val region: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    /**
     * Sends an identify request with [body], authorized per [session].
     */
    suspend fun identify(session: ConnectSession, body: String) {
        val response = if (session.isAuthenticated) {
            sendAuthenticated(session.accessToken!!, body)
        } else {
            val credentials = session.credentials
                ?: throw ConnectNotSignedInException()
            sendGuest(credentials, body)
        }

        if (response.code !in 200..299) {
            val responseBody = response.body?.string().orEmpty()
            throw mapErrorResponse(response.code, responseBody)
        }
    }

    private fun sendAuthenticated(accessToken: String, body: String): okhttp3.Response {
        val request = Request.Builder()
            .url("$endpoint$IDENTIFY_USER_PATH")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw ConnectNetworkException(cause = e)
        }
    }

    private fun sendGuest(credentials: AwsCredentials, body: String): okhttp3.Response {
        val url = "$endpoint$GUEST_IDENTIFY_USER_PATH"
        val parsedUrl = URL(url)
        val host = parsedUrl.host
        val path = parsedUrl.path.ifEmpty { "/" }
        val bodyBytes = body.toByteArray(Charsets.UTF_8)

        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val amzDate = now.format(AMZ_DATE_FORMAT)
        val dateStamp = now.format(DATE_STAMP_FORMAT)

        val payloadHash = sha256Hex(bodyBytes)

        // Build canonical request
        val signedHeaders = buildList {
            add("content-type")
            add("host")
            add("x-amz-date")
            if (credentials is AwsCredentials.Temporary) add("x-amz-security-token")
        }.sorted().joinToString(";")

        val canonicalHeaders = buildString {
            append("content-type:application/json\n")
            append("host:$host\n")
            append("x-amz-date:$amzDate\n")
            if (credentials is AwsCredentials.Temporary) {
                append("x-amz-security-token:${credentials.sessionToken}\n")
            }
        }

        val canonicalRequest = listOf(
            "POST",
            path,
            "", // empty query string
            canonicalHeaders,
            signedHeaders,
            payloadHash
        ).joinToString("\n")

        // String to sign
        val credentialScope = "$dateStamp/$region/$SERVICE_EXECUTE_API/aws4_request"
        val stringToSign = listOf(
            "AWS4-HMAC-SHA256",
            amzDate,
            credentialScope,
            sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))
        ).joinToString("\n")

        // Signing key
        val signingKey = getSignatureKey(credentials.secretAccessKey, dateStamp, region)
        val signature = hmacSha256Hex(signingKey, stringToSign)

        val authHeader = "AWS4-HMAC-SHA256 Credential=${credentials.accessKeyId}/$credentialScope, " +
            "SignedHeaders=$signedHeaders, Signature=$signature"

        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Host", host)
            .addHeader("X-Amz-Date", amzDate)
            .addHeader("Authorization", authHeader)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))

        if (credentials is AwsCredentials.Temporary) {
            requestBuilder.addHeader("X-Amz-Security-Token", credentials.sessionToken)
        }

        return try {
            httpClient.newCall(requestBuilder.build()).execute()
        } catch (e: Exception) {
            throw ConnectNetworkException(cause = e)
        }
    }

    private fun mapErrorResponse(statusCode: Int, responseBody: String): Exception {
        val detail = parseErrorDetail(responseBody)
        return when (statusCode) {
            429 -> ConnectThrottlingException()
            401, 403 -> ConnectAccessDeniedException()
            400 -> ConnectValidationException(detail = detail)
            else -> ConnectServiceException(
                detail = detail ?: "The endpoint returned status $statusCode."
            )
        }
    }

    private fun parseErrorDetail(body: String): String? {
        if (body.isBlank()) return null
        return try {
            val json = Json.parseToJsonElement(body)
            if (json is JsonObject) {
                (json["error"] ?: json["message"] ?: json["Message"])
                    ?.jsonPrimitive?.content
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(key: ByteArray, data: String): String =
        hmacSha256(key, data).joinToString("") { "%02x".format(it) }

    private fun getSignatureKey(secretKey: String, dateStamp: String, region: String): ByteArray {
        val kDate = hmacSha256("AWS4$secretKey".toByteArray(Charsets.UTF_8), dateStamp)
        val kRegion = hmacSha256(kDate, region)
        val kService = hmacSha256(kRegion, SERVICE_EXECUTE_API)
        return hmacSha256(kService, "aws4_request")
    }

    internal companion object {
        const val IDENTIFY_USER_PATH = "/identify-user"
        const val GUEST_IDENTIFY_USER_PATH = "/identify-user-guest"
        const val SERVICE_EXECUTE_API = "execute-api"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        val AMZ_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        val DATE_STAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
