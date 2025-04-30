/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.amplify.util

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignedBodyHeader
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.toHttpBody
import aws.smithy.kotlin.runtime.net.url.Url
import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amazonaws.sdk.appsync.core.AppSyncRequest

internal class AppSyncRequestSigner(
    private val credentialsProvider: AuthCredentialsProvider = CognitoCredentialsProvider(),
    private val awsSigner: AwsSigner = DefaultAwsSigner
) {

    suspend fun signAppSyncRequest(request: AppSyncRequest, region: String): Map<String, String> {
        // First translate the Apollo request to a Smithy request so it can be signed
        val smithyRequest = request.toSmithyRequest()

        // Sign the Smithy request
        val signedRequest = signRequest(
            awsRegion = region,
            credentials = credentialsProvider.resolve(),
            service = "appsync",
            request = smithyRequest
        )

        // Return the headers from the signed request
        return signedRequest.headers.entries().associate { it.key to it.value.first() }
    }

    @OptIn(InternalApi::class)
    private suspend fun signRequest(
        awsRegion: String,
        credentials: Credentials,
        service: String,
        request: HttpRequest
    ): HttpRequest {
        val signingConfig = AwsSigningConfig {
            region = awsRegion
            useDoubleUriEncode = true
            this.service = service
            this.credentials = credentials
            signedBodyHeader = AwsSignedBodyHeader.X_AMZ_CONTENT_SHA256
        }

        // Generate the signature for the smithy request
        return awsSigner.sign(request, signingConfig).output
    }

    private fun AppSyncRequest.toSmithyRequest(): HttpRequest {
        return HttpRequest(
            method = method.toSmithyMethod(),
            url = Url.parse(url),
            headers = createSmithyHeaders(headers),
            body = body?.toHttpBody() ?: HttpBody.Empty
        )
    }

    private fun AppSyncRequest.HttpMethod.toSmithyMethod(): aws.smithy.kotlin.runtime.http.HttpMethod {
        return when (this) {
            AppSyncRequest.HttpMethod.GET -> aws.smithy.kotlin.runtime.http.HttpMethod.GET
            AppSyncRequest.HttpMethod.POST -> aws.smithy.kotlin.runtime.http.HttpMethod.POST
        }
    }

    private fun createSmithyHeaders(headers: Map<String, String>): Headers {
        return Headers {
            headers.forEach {
                this.append(it.key, it.value)
            }
        }
    }
}
