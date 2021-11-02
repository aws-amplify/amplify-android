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
import com.amazonaws.http.HttpMethodName
import com.amazonaws.util.IOUtils
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import okhttp3.*

import okio.Buffer
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.*

private typealias AWSRequest = com.amazonaws.Request<Any>

private fun Request.Builder.copyFrom(request: AWSRequest): Request.Builder {
    val urlBuilder = HttpUrl.Builder()
        .host(request.endpoint.host)
        .scheme(request.endpoint.scheme)
        .encodedPath(request.encodedUriResourcePath)

    request.parameters.forEach { (name, value) ->
        urlBuilder.setQueryParameter(name, value)
    }
    request.headers.forEach { (name, value) ->
        this.header(name, value)
    }
    return this.url(urlBuilder.build())
}

/**
 * Interceptor that can authorize requests using AWS Signature V4 signer.
 */
internal class AWSRequestSignerInterceptor(
    private val plugin: AWSLocationGeoPlugin
): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // OkHttpRequest -> Signed AWSRequest
        val awsRequest = signRequest(request)

        // add signed parameters and headers to original request
        val signedRequest = request.newBuilder()
            .copyFrom(awsRequest)
            .build()
        return chain.proceed(signedRequest)
    }

    private fun signRequest(request: Request): DefaultRequest<Any> {
        // read request content
        val body = request.body?.let {
            val buffer = Buffer()
            it.writeTo(buffer)
            IOUtils.toByteArray(buffer.inputStream())
        } ?: ByteArray(0)

        val client = plugin.escapeHatch
        val url = request.url
        val awsRequest = DefaultRequest<Any>(client.serviceName)
        awsRequest.setEncodedResourcePath(request.url.encodedPath)
        awsRequest.parameters = url.queryParameterNames.associateWith { url.queryParameter(it) }
        awsRequest.endpoint = URI.create("${url.scheme}://${url.host}")
        awsRequest.httpMethod = HttpMethodName.valueOf(request.method.toUpperCase(Locale.ROOT))
        awsRequest.content = ByteArrayInputStream(body)
        awsRequest.headers = request.headers.associate { (name, value) -> name to value }

        // sign request with AWS Signer for the underlying service
        val signer = client.getSignerByURI(awsRequest.endpoint)
        signer.sign(awsRequest, plugin.credentialsProvider.credentials)
        return awsRequest
    }

}