/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

@file:JvmName("SmithyCredentials")

package com.amplifyframework.foundation.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.time.Instant as SmithyInstant
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Converts Amplify Credentials to Smithy Credentials
 */
@OptIn(ExperimentalTime::class)
fun AwsCredentials.toSmithyCredentials(): Credentials = when (this) {
    is AwsCredentials.Static -> Credentials(
        accessKeyId = this.accessKeyId,
        secretAccessKey = this.secretAccessKey
    )
    is AwsCredentials.Temporary -> Credentials(
        accessKeyId = this.accessKeyId,
        secretAccessKey = this.secretAccessKey,
        sessionToken = this.sessionToken,
        expiration = this.expiration.toSmithyInstant()
    )
}

/**
 * Converts Smithy Credentials to Amplify Credentials
 */
@OptIn(ExperimentalTime::class)
fun Credentials.toAwsCredentials(): AwsCredentials {
    val sessionToken = this.sessionToken
    val expiration = this.expiration
    return when {
        sessionToken != null && expiration != null -> AwsCredentials.Temporary(
            accessKeyId = this.accessKeyId,
            secretAccessKey = this.secretAccessKey,
            sessionToken = sessionToken,
            expiration = expiration.toKotlinInstant()
        )
        else -> AwsCredentials.Static(
            accessKeyId = this.accessKeyId,
            secretAccessKey = this.secretAccessKey
        )
    }
}

/**
 * Converts an Amplify CredentialsProvider to a Smithy CredentialsProvider
 */
fun AwsCredentialsProvider<*>.toSmithyProvider() = object : CredentialsProvider {
    override suspend fun resolve(attributes: Attributes) = this@toSmithyProvider.resolve().toSmithyCredentials()
}

/**
 * Converts a Smithy CredentialsProvider to an Amplify CredentialsProvider
 */
fun CredentialsProvider.toAwsCredentialsProvider() = AwsCredentialsProvider {
    resolve(emptyAttributes()).toAwsCredentials()
}

@OptIn(ExperimentalTime::class)
private fun Instant.toSmithyInstant() = SmithyInstant.fromEpochSeconds(epochSeconds)

@OptIn(ExperimentalTime::class)
private fun SmithyInstant.toKotlinInstant() = Instant.fromEpochSeconds(epochSeconds)
