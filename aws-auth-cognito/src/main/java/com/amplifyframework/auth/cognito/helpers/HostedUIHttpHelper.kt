package com.amplifyframework.auth.cognito.helpers

import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.AWSCognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.jvm.Throws
import kotlin.time.Duration.Companion.seconds

object HostedUIHttpHelper {

    private val json = Json { ignoreUnknownKeys = true }

    @Throws(Exception::class)
    fun fetchTokens(url: URL, headerParams: Map<String, String>, bodyParams: Map<String, String>): CognitoUserPoolTokens {
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
            val responseStream = if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseString = responseStream.bufferedReader().use(BufferedReader::readText)
            return parseTokenResponse(responseString)
        } else {
            // throw AuthServiceException(httpsURLConnection.getResponseMessage())
            throw Exception()
        }
    }

    private fun parseTokenResponse(responseString: String): CognitoUserPoolTokens {

        if (responseString.isEmpty()) {
            // throw
        }

        try {

            val response = json.decodeFromString<FetchTokenResponse>(responseString)

            response.error?.let {
                if (it == "invalid_grant") {
                    throw Exception() // throw AuthInvalidGrantException(errorText)
                } else {
                    throw Exception() // throw AuthServiceException(errorText)
                }
            }

            return CognitoUserPoolTokens(
                idToken = response.idToken,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiration = response.expiration
            )
        } catch (e: Exception) {
            // throw AuthClientException(e.message, e)
            throw e
        }
    }
}

@Serializable
class FetchTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") private val expiresIn: Int? = null,
    @SerialName("error") val error: String? = null
) {
    val expiration = expiresIn?.let { Instant.now().plus(it.seconds).epochSeconds }
}