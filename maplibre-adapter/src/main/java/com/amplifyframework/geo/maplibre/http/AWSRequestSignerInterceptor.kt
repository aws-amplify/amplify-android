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

package com.amplifyframework.geo.maplibre.http

import aws.sdk.kotlin.runtime.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.runtime.auth.signing.sign
import aws.smithy.kotlin.runtime.http.Headers as AwsHeaders
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

const val AMAZON_HOST = "amazonaws.com"

/**
 * Interceptor that can authorize requests using AWS Signature V4 signer.
 */
internal class AWSRequestSignerInterceptor(
    private val plugin: AWSLocationGeoPlugin
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.host.contains(AMAZON_HOST)) {
            return chain.proceed(request)
        }

        // OkHttpRequest -> Signed AWSRequest
        val awsRequest = signRequest(request)

        // add signed parameters and headers to original request
        val signedRequest = request.newBuilder()
            .copyFrom(awsRequest)
            .build()
        return chain.proceed(signedRequest)
    }

    private fun Request.Builder.copyFrom(request: HttpRequest): Request.Builder {
        val urlBuilder = HttpUrl.Builder()
            .host(request.url.host)
            .scheme(request.url.scheme.protocolName)
            .encodedPath(request.url.encodedPath)

        request.url.parameters.forEach { name, parameters ->
            parameters.forEach {
                urlBuilder.setQueryParameter(name, it)
            }
        }
        request.headers.forEach { name, values ->
            values.forEach {
                this.header(name, it)
            }
        }
        return this.url(urlBuilder.build())
    }

    private fun signRequest(request: Request): HttpRequest {
        val url = request.url
        val headers: AwsHeaders = AwsHeaders.invoke {
            request.headers.forEach { (name, value) ->
                setMissing(name, value)
            }
            set("Host", request.url.host)
        }

        val client = plugin.escapeHatch
        val signingConfig = AwsSigningConfig.invoke {
            region = client.config.region
            service = client.serviceName
            credentialsProvider = plugin.credentialsProvider
        }

        // set the request body
        val bodyBytes: ByteArray = getBytes(request.body)
        val body2 = ByteArrayContent(bodyBytes)
        val method = HttpMethod.parse(request.method)
        val awsRequest = HttpRequest(method, Url.parse(url.toString()), headers, body2)

        runBlocking {
            // sign request with AWS Signer for the underlying service
            sign(awsRequest, signingConfig)
        }
        return awsRequest
    }

    private fun getBytes(body: RequestBody?): ByteArray {
        if (body == null) {
            return "".toByteArray()
        }
        val BUFFER_SIZE = 1024 * 4
        try {
            ByteArrayOutputStream().use { output ->
                // write the body to a byte array.
                val buffer = Buffer()
                body.writeTo(buffer)
                val bytes = ByteArray(BUFFER_SIZE)
                var n: Int
                while (buffer.inputStream().read(bytes).also { n = it } != -1) {
                    output.write(bytes, 0, n)
                }
                return output.toByteArray()
            }
        } catch (exception: IOException) {
            throw Exception(
                "Unable to calculate SigV4 signature for the request",
                exception
            )
        }
    }
}
