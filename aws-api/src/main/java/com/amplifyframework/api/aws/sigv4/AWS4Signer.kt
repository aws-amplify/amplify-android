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

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.signing.AwsSignedBodyHeaderType
import aws.sdk.kotlin.runtime.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.runtime.auth.signing.SigningResult
import aws.sdk.kotlin.runtime.auth.signing.sign
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlinx.coroutines.runBlocking

/**
 * Abstraction for signing a HTTP Request using [aws.sdk.kotlin.runtime.auth.signing.sign]
 */
abstract class AWS4Signer(private val regionName: String) {
    protected var awsSignedBodyHeaderType = AwsSignedBodyHeaderType.NONE

    /**
     * Async signing
     */
    suspend fun sign(
        httpRequest: HttpRequest,
        credentials: Credentials,
        serviceName: String
    ): SigningResult<HttpRequest> {
        val signingConfig = AwsSigningConfig.invoke {
            region = regionName
            useDoubleUriEncode = true
            service = serviceName
            this.credentials = credentials
            signedBodyHeader = awsSignedBodyHeaderType
        }
        return sign(httpRequest, signingConfig)
    }

    /**
     * Sign synchronously
     */
    fun signBlocking(
        httpRequest: HttpRequest,
        credentials: Credentials,
        serviceName: String
    ): SigningResult<HttpRequest> {
        return runBlocking {
            sign(httpRequest, credentials, serviceName)
        }
    }
}

/**
 * Signer used for AppSync requests.
 */
class AppSyncV4Signer(regionName: String) : AWS4Signer(regionName) {
    init {
        awsSignedBodyHeaderType = AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256
    }
}

/**
 * Signer for API Gateway requests
 */
class ApiGatewayIamSigner(regionName: String) : AWS4Signer(regionName)
