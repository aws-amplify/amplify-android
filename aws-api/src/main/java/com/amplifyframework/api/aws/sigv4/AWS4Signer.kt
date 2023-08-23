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

package com.amplifyframework.api.aws.sigv4

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignedBodyHeader
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningResult
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlinx.coroutines.runBlocking

/**
 * Abstraction for signing a HTTP Request using [aws.sdk.kotlin.runtime.auth.signing.sign]
 */
abstract class AWS4Signer(private val regionName: String) {
    protected var awsSignedBodyHeaderType = AwsSignedBodyHeader.NONE

    /**
     * Async signing
     */
    @OptIn(InternalApi::class)
    suspend fun sign(
        httpRequest: HttpRequest,
        credentialsProvider: CredentialsProvider,
        serviceName: String
    ): AwsSigningResult<HttpRequest> {
        val signingConfig = AwsSigningConfig.invoke {
            region = regionName
            useDoubleUriEncode = true
            service = serviceName
            this.credentials = credentialsProvider.resolve()
            signedBodyHeader = awsSignedBodyHeaderType
        }
        return DefaultAwsSigner.sign(httpRequest, signingConfig)
    }

    /**
     * Sign synchronously
     */
    @OptIn(InternalApi::class)
    fun signBlocking(
        httpRequest: HttpRequest,
        credentialsProvider: CredentialsProvider,
        serviceName: String
    ): AwsSigningResult<HttpRequest> {
        return runBlocking {
            sign(httpRequest, credentialsProvider, serviceName)
        }
    }
}

/**
 * Signer used for AppSync requests.
 */
class AppSyncV4Signer(regionName: String) : AWS4Signer(regionName) {
    init {
        awsSignedBodyHeaderType = AwsSignedBodyHeader.X_AMZ_CONTENT_SHA256
    }
}

/**
 * Signer for API Gateway requests
 */
class ApiGatewayIamSigner(regionName: String) : AWS4Signer(regionName)
