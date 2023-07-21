/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.helpers

import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.exceptions.service.InvalidGrantException
import com.amplifyframework.auth.cognito.exceptions.service.ParseTokenException
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import java.io.BufferedReader
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal object HostedUIHttpHelper {

    private val json = Json { ignoreUnknownKeys = true }

    @Throws(Exception::class)
    fun fetchTokens(
        url: URL,
        headerParams: Map<String, String>,
        bodyParams: Map<String, String>
    ): CognitoUserPoolTokens {
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            // add headers
            headerParams.map { addRequestProperty(it.key, it.value) }
            // add body
            DataOutputStream(outputStream).use { dos ->
                val requestBody = bodyParams.map {
                    "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
                }.joinToString("&")
                dos.writeBytes(requestBody)
            }
        }

        val responseCode = connection.responseCode

        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_INTERNAL_ERROR) {
            val responseStream = if (responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseString = responseStream.bufferedReader().use(BufferedReader::readText)

            return parseTokenResponse(responseString)
        } else {
            throw ServiceException(
                message = connection.responseMessage,
                recoverySuggestion = AmplifyException.TODO_RECOVERY_SUGGESTION
            )
        }
    }

    private fun parseTokenResponse(responseString: String): CognitoUserPoolTokens {

        if (responseString.isEmpty()) {
            throw ParseTokenException()
        }

        try {
            val response = json.decodeFromString<FetchTokenResponse>(responseString)

            response.error?.let {
                if (it == "invalid_grant") {
                    throw SessionExpiredException(it, cause = InvalidGrantException(it, response.errorDescription))
                } else {
                    throw ServiceException(it, AmplifyException.TODO_RECOVERY_SUGGESTION)
                }
            }

            return CognitoUserPoolTokens(
                idToken = response.idToken,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiration = response.expiration
            )
        } catch (e: Exception) {
            if (e !is SessionExpiredException && e !is ServiceException) {
                throw ServiceException(
                    message = e.message ?: "An unknown service error has occurred",
                    recoverySuggestion = AmplifyException.TODO_RECOVERY_SUGGESTION,
                    cause = e
                )
            } else throw e
        }
    }
}

@Serializable
internal class FetchTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") private val expiresIn: Int? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
) {
    val expiration = expiresIn?.let { Instant.now().plus(it.seconds).epochSeconds }
}
