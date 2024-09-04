/*
 *  Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.apollo.appsync.util

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignedBodyHeader
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.request.HttpRequest as SmithyHttpRequest
import aws.smithy.kotlin.runtime.net.url.Url
import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.apollographql.apollo.api.http.HttpRequest
import okio.Buffer

internal class ApolloRequestSigner(
    private val credentialsProvider: AuthCredentialsProvider = CognitoCredentialsProvider(),
    private val awsSigner: AwsSigner = DefaultAwsSigner
) {

    suspend fun signAppSyncRequest(request: HttpRequest, region: String): Map<String, String> {
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
        request: SmithyHttpRequest
    ): SmithyHttpRequest {
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

    private fun HttpRequest.toSmithyRequest(): SmithyHttpRequest {
        // Read the body from the request
        val buffer = Buffer().also { body?.writeTo(it) }

        return SmithyHttpRequest(
            method = method.toSmithyMethod(),
            url = Url.parse(url),
            headers = headers.toSmithyHeaders(),
            body = HttpBody.fromBytes(buffer.readByteArray())
        )
    }
}
