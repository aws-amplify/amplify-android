/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws.http

import android.net.Uri
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import okio.ByteString.Companion.encodeUtf8

internal class AWSV4Signer {

    // Initial prior signature will be signature of initial request (web socket connection request)
    private var priorSignature = ""

    // Using en_US_POSIX for consistency with iOS and the locale gives US English results regardless of user and
    // system preferences. Reference: https://developer.apple.com/library/archive/qa/qa1480/_index.html
    private val dateFormatter = SimpleDateFormat(DATE_PATTERN, Locale("en", "US", "POSIX"))
    private val timeFormatter = SimpleDateFormat(TIME_PATTERN, Locale("en", "US", "POSIX"))
    private val sha256Algorithm = MessageDigest.getInstance("SHA-256")
    val encodedSpace: String = Uri.encode(" ")

    init {
        dateFormatter.timeZone = GMT_TIMEZONE
        dateFormatter.isLenient = false
        timeFormatter.timeZone = GMT_TIMEZONE
        timeFormatter.isLenient = false
    }

    fun getSignedUri(
        uri: URI,
        credentials: Credentials,
        region: String,
        userAgent: String,
        dateMilli: Long = Date().time
    ): URI? {
        val amzDate = getTimeStamp(dateMilli)
        val datestamp = getDateStamp(dateMilli)
        val sessionToken = credentials.sessionToken ?: ""
        val queryParamsMap = buildQueryParamsMap(
            uri,
            credentials.accessKeyId,
            sessionToken,
            region,
            amzDate,
            datestamp,
            userAgent
        )
        val canonicalQueryString = getCanonicalizedQueryString(queryParamsMap)
        val canonicalRequest = getCanonicalRequest(uri, canonicalQueryString)
        val stringToSign = signString(amzDate, createCredentialScope(region, datestamp), canonicalRequest)

        val signatureKey = createSignature(region, credentials.secretAccessKey, datestamp)
        val signature = convertBytesToHex(hmacSha256(stringToSign, signatureKey))

        if (priorSignature.isEmpty()) {
            priorSignature = signature
        }

        val signedCanonicalQueryString = "$canonicalQueryString&$X_AMZ_SIGNATURE=$signature"
        var uriResult: URI? = null
        try {
            uriResult = URI(uri.scheme, uri.rawAuthority, getCanonicalUri(uri), signedCanonicalQueryString, null)
        } catch (e: URISyntaxException) {
            LOG.error("Error creating signed URI.")
        }

        return uriResult
    }

    fun getSignedFrame(region: String, frame: ByteArray, secretKey: String, dateHeader: Pair<String, Date>): String {
        val dateMilli = dateHeader.second.time
        val timestamp = getTimeStamp(dateMilli)
        val datestamp = getDateStamp(dateMilli)

        val credentialScope = createCredentialScope(region, datestamp)
        val stringToSign = signStringWithPreviousSignature(timestamp, credentialScope, frame, dateHeader)
        val signatureKey = createSignature(region, secretKey, datestamp)
        val signature = convertBytesToHex(hmacSha256(stringToSign, signatureKey))
        this.priorSignature = signature

        return signature
    }

    private fun createCredentialScope(region: String, date: String): String {
        return listOf(date, region, SERVICE, AWS4_REQUEST_TYPE).joinToString("/")
    }

    private fun hmacSha256(data: String, key: ByteArray): ByteArray {
        val algorithm = "HmacSHA256"
        try {
            val mac = Mac.getInstance(algorithm)
            mac.init(SecretKeySpec(key, algorithm))
            return mac.doFinal(data.toByteArray(Charset.defaultCharset()))
        } catch (error: NoSuchAlgorithmException) {
            throw IllegalArgumentException(error.message, error)
        } catch (error: InvalidKeyException) {
            throw IllegalArgumentException(error.message, error)
        }
    }

    private fun createSignature(region: String, secretKey: String, date: String): ByteArray {
        val secret = ("AWS4$secretKey").toByteArray(Charset.defaultCharset())
        val hmacDate = hmacSha256(date, secret) // date key
        val hmacRegion = hmacSha256(region, hmacDate) // region key
        val hmacService = hmacSha256(SERVICE, hmacRegion) // service key
        val hmacRequestType = hmacSha256(AWS4_REQUEST_TYPE, hmacService) // signing key
        return hmacRequestType
    }

    private fun signString(date: String, credentialScope: String, canonicalRequest: String): String {
        val hashedPayload = convertBytesToHex(
            sha256Algorithm.digest(canonicalRequest.toByteArray(Charset.defaultCharset()))
        )
        return listOf(ALGORITHM_AWS4_HMAC_SHA_256, date, credentialScope, hashedPayload)
            .joinToString(NEW_LINE_DELIMITER)
    }

    private fun signStringWithPreviousSignature(
        datetime: String,
        credentialScope: String,
        payload: ByteArray,
        dateHeader: Pair<String, Date>
    ): String {
        val hashedPayloadBytes = sha256Algorithm.digest(payload)
        val hashedPayload = convertBytesToHex(hashedPayloadBytes)
        val encodedDateHeader = encodeDateHeader(dateHeader)
        val hashedDateHeaderBytes = sha256Algorithm.digest(encodedDateHeader)
        val hashedDateHeader = convertBytesToHex(hashedDateHeaderBytes)
        return listOf(
            ALGORITHM_AWS4_HMAC_SHA_256_PAYLOAD,
            datetime,
            credentialScope,
            priorSignature,
            hashedDateHeader,
            hashedPayload
        ).joinToString("\n")
    }

    private fun encodeDateHeader(dateHeader: Pair<String, Date>): ByteArray {
        val headerNameLength = dateHeader.first.length
        val headerValueType = 8
        val headerValueLength = 8
        val headerValue = dateHeader.second.time
        val headerLength = headerNameLength + headerValueLength + 2
        val headerByteArray = ByteBuffer.allocate(headerLength)
        headerByteArray.put(headerNameLength.toUByte().toByte())
        headerByteArray.put(dateHeader.first.encodeUtf8().toByteArray())
        headerByteArray.put(headerValueType.toByte())
        headerByteArray.putLong(headerValue)
        return (headerByteArray.position(0) as ByteBuffer).array()
    }

    private fun convertBytesToHex(data: ByteArray): String {
        val stringBuilder = StringBuilder(data.size * 2)
        for (element in data) {
            var hex = Integer.toHexString(element.toInt())
            if (hex.length == 1) {
                // Append leading zero.
                stringBuilder.append("0")
            } else if (hex.length == 8) {
                // Remove ff prefix from negative numbers.
                hex = hex.substring(6)
            }
            stringBuilder.append(hex)
        }
        return stringBuilder.toString().lowercase(Locale.getDefault())
    }

    private fun getTimeStamp(dateMilli: Long): String {
        return timeFormatter.format(Date(dateMilli))
    }

    private fun getDateStamp(dateMilli: Long): String {
        return dateFormatter.format(Date(dateMilli))
    }

    private fun buildQueryParamsMap(
        uri: URI,
        accessKey: String,
        sessionToken: String,
        region: String,
        amzDate: String,
        datestamp: String,
        userAgent: String
    ): Map<String, String> {
        val queryParamsBuilder = mutableMapOf(
            X_AMZ_ALGORITHM to ALGORITHM_AWS4_HMAC_SHA_256,
            X_AMZ_CREDENTIAL to urlEncode(accessKey + "/" + createCredentialScope(region, datestamp)),
            X_AMZ_DATE to amzDate,
            X_AMZ_EXPIRES to "299",
            X_AMZ_SIGNED_HEADERS to SIGNED_HEADERS,
            X_AMZ_USER_AGENT to urlEncode(userAgent).replace("+", encodedSpace)
        )

        if (!sessionToken.isNullOrEmpty()) {
            queryParamsBuilder[X_AMZ_SECURITY_TOKEN] = urlEncode(sessionToken)
        }

        if (!uri.query.isNullOrEmpty()) {
            val params = uri.query.split("&")
            for (param in params) {
                val index = param.indexOf('=')
                if (index > 0) {
                    queryParamsBuilder[param.substring(0, index)] = urlEncode(param.substring(index + 1))
                }
            }
        }
        return queryParamsBuilder.toMap()
    }

    private fun urlEncode(str: String): String {
        try {
            return URLEncoder.encode(str, Charset.defaultCharset().name())
        } catch (e: UnsupportedEncodingException) {
            throw IllegalArgumentException(e.message, e)
        }
    }

    private fun getCanonicalizedQueryString(queryParamsMap: Map<String, String>): String {
        val queryKeys = queryParamsMap.keys.toMutableList()
        queryKeys.sort()
        val builder = StringBuilder()
        for (i in queryKeys.indices) {
            builder.append(queryKeys[i]).append("=").append(queryParamsMap[queryKeys[i]])
            if (queryKeys.size - 1 > i) {
                builder.append("&")
            }
        }
        return builder.toString()
    }

    private fun getCanonicalRequest(uri: URI, canonicalQueryString: String): String {
        val payloadHash = convertBytesToHex(sha256Algorithm.digest("".toByteArray(UTF_8)))
        val canonicalUri = getCanonicalUri(uri)
        val canonicalHeaders = "host:" + uri.host + NEW_LINE_DELIMITER
        return listOf(
            METHOD,
            canonicalUri,
            canonicalQueryString,
            canonicalHeaders,
            SIGNED_HEADERS,
            payloadHash
        ).joinToString(NEW_LINE_DELIMITER)
    }

    private fun getCanonicalUri(uri: URI): String {
        return uri.path.ifEmpty {
            "/"
        }
    }

    companion object {
        private const val AWS4_REQUEST_TYPE = "aws4_request"
        private const val ALGORITHM_AWS4_HMAC_SHA_256_PAYLOAD = "AWS4-HMAC-SHA256-PAYLOAD"
        private const val ALGORITHM_AWS4_HMAC_SHA_256 = "AWS4-HMAC-SHA256"
        private const val SERVICE = "rekognition"
        private const val TIME_PATTERN = "yyyyMMdd'T'HHmmss'Z'"
        private const val DATE_PATTERN = "yyyyMMdd"
        private val GMT_TIMEZONE = TimeZone.getTimeZone("GMT")

        private const val X_AMZ_ALGORITHM = "X-Amz-Algorithm"
        private const val X_AMZ_CREDENTIAL = "X-Amz-Credential"
        private const val X_AMZ_EXPIRES = "X-Amz-Expires"
        private const val X_AMZ_SIGNED_HEADERS = "X-Amz-SignedHeaders"
        private const val SIGNED_HEADERS = "host"
        private const val NEW_LINE_DELIMITER = "\n"
        private const val METHOD = "GET"
        private const val X_AMZ_SIGNATURE = "X-Amz-Signature"
        private const val X_AMZ_DATE = "X-Amz-Date"
        private const val X_AMZ_SECURITY_TOKEN = "X-Amz-Security-Token"
        private const val X_AMZ_USER_AGENT = "x-amz-user-agent"

        private val LOG = Amplify.Logging.logger(CategoryType.PREDICTIONS, "amplify:aws-predictions")
    }
}
