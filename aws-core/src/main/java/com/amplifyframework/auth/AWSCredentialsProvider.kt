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

package com.amplifyframework.auth

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.util.Attributes
import com.amplifyframework.core.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Customer provided CredentialsProvider implementation that fetches and returns AWSCredentials or a subclass such
 * as AWSTemporaryCredentials. For example:
 *
 * class AWSTemporaryCredentialsProvider: AWSCredentialsProvider<AWSTemporaryCredentials> {
 *   override fun fetchAWSCredentials(
 *     onSuccess: Consumer<AWSTemporaryCredentials>,
 *     onError: Consumer<AuthException>
 *   ) {
 *     // customer provided fetch implementation
 *   }
 * }
 */
interface AWSCredentialsProvider<out T : AWSCredentials> {

    fun fetchAWSCredentials(
        onSuccess: Consumer<@UnsafeVariance T>,
        onError: Consumer<AuthException>
    )
}

fun <T : AWSCredentials> convertToSdkCredentialsProvider(
    awsCredentialsProvider: AWSCredentialsProvider<T>
): CredentialsProvider {

    return object : CredentialsProvider {
        override suspend fun resolve(attributes: Attributes): Credentials {
            return suspendCoroutine { continuation ->
                awsCredentialsProvider.fetchAWSCredentials(
                    { continuation.resume(it.toSdkCredentials()) },
                    { continuation.resumeWithException(it) }
                )
            }
        }
    }
}
