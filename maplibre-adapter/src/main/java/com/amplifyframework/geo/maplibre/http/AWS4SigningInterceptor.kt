/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.DefaultRequest
import com.amazonaws.auth.AWS4Signer
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.http.HttpMethodName
import com.amazonaws.util.IOUtils

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.ByteArrayInputStream
import java.net.URI

/**
 * Interceptor that can authorize requests using AWS Signature V4 signer.
 */
internal class AWS4SigningInterceptor(
    private val credentialsProvider: AWSCredentialsProvider,
    private val serviceName: String,
    private val signer: AWS4Signer = AWS4Signer()
): Interceptor {
    init {
        signer.setServiceName(serviceName)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // OkHttpRequest -> AWSRequest
        val awsRequest = toAwsRequest(originalRequest)

        // add signed headers to original request
        val newRequest = originalRequest.newBuilder()
            .headers(signedHeaders(awsRequest))
            .build()
        return chain.proceed(newRequest)
    }

    private fun toAwsRequest(okHttpRequest: Request): DefaultRequest<Any> {
        // read request content
        val body = okHttpRequest.body?.let {
            val buffer = Buffer()
            it.writeTo(buffer)
            IOUtils.toByteArray(buffer.inputStream())
        } ?: ByteArray(0)

        val awsRequest = DefaultRequest<Any>(serviceName)
        awsRequest.resourcePath = okHttpRequest.url.toUri().path
        awsRequest.endpoint = URI.create("${okHttpRequest.url.scheme}://${okHttpRequest.url.host}")
        awsRequest.httpMethod = HttpMethodName.valueOf(okHttpRequest.method)
        awsRequest.content = ByteArrayInputStream(body)
        awsRequest.headers = okHttpRequest.headers.map { it.first to it.second }.toMap()
        return awsRequest
    }

    private fun signedHeaders(awsRequest: com.amazonaws.Request<Any>): Headers {
        // sign request with AWS Signature V4 signer
        signer.sign(awsRequest, credentialsProvider.credentials)

        // flatten headers to array, e.g. [key1, value1, key2, value2, ...]
        val params = awsRequest.headers.flatMap { listOf(it.key, it.value) }.toTypedArray()
        return Headers.headersOf(*params)
    }
}