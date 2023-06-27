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
package com.amplifyframework.logging.cloudwatch

import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.Url
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.core.Consumer
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * TODO: Add documentation
 */
class DefaultRemoteLoggingConstraintProvider(
    private val url: URL,
    private val regionString: String,
    private val refreshIntervalInSeconds: Int = 1200,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RemoteLoggingConstraintProvider {
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
    }
    private val okHttpClient: OkHttpClient = OkHttpClient()

    override fun fetchLoggingConfig(onSuccess: Consumer<LoggingConstraints>, onError: Consumer<Exception>) {
        coroutineScope.launch {
            try {
                val request = HttpRequest(
                    HttpMethod.GET,
                    Url.parse(url.toString()),
                    Headers.Empty,
                    HttpBody.Empty,
                )
                val signedRequest = DefaultAwsSigner.sign(
                    request,
                    AwsSigningConfig {
                        this.region = regionString
                        service = "execute-api"
                        credentialsProvider = CognitoCredentialsProvider()
                    },
                ).output

                val okhttpRequestBuilder = Request.Builder()
                okhttpRequestBuilder.method(signedRequest.method.name, null)
                okhttpRequestBuilder.url(signedRequest.url.toString())
                signedRequest.headers.entries().forEach { header ->
                    header.value.forEach {
                        okhttpRequestBuilder.addHeader(header.key, it)
                    }
                }
                val response = okHttpClient.newCall(okhttpRequestBuilder.build()).execute()
                if (response.isSuccessful) {
                    val remoteLoggingConstraints = json.decodeFromString<LoggingConstraints>(response.body.string())
                    onSuccess.accept(
                        remoteLoggingConstraints,
                    )
                } else {
                    onError.accept(
                        AmplifyException("Failed to fetch remote logging constraints", response.body.string()),
                    )
                }
            } catch (exception: Exception) {
                onError.accept(
                    AmplifyException(
                        "Failed to fetch remote logging constraints",
                        exception,
                        "Please try again.",
                    ),
                )
            }
        }
    }

    override fun getConstraintsSyncInterval(): Int {
        return refreshIntervalInSeconds
    }
}
