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

package com.amplifyframework.auth

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.time.Instant

/**
 * Provides access to the AWS credentials used for accessing AWS services: AWS
 * access key ID and secret access key. These credentials are used to securely
 * sign requests to AWS services.
 *
 * For more details on AWS access keys,
 * [see](https://docs.aws.amazon.com/general/latest/gr/aws-security-credentials.html)
 */
open class AWSCredentials(
    /**
     * The AWS access key ID for this credentials object.
     */
    open val accessKeyId: String,

    /**
     * The AWS secret access key for this credentials object.
     */
    open val secretAccessKey: String
) {
    companion object Factory {

        fun createAWSCredentials(
            accessKeyId: String?,
            secretAccessKey: String?,
            sessionToken: String?,
            expiration: Long?,
        ): AWSCredentials? {
            return when {
                accessKeyId == null || secretAccessKey == null -> {
                    null
                }
                sessionToken != null && expiration != null -> {
                    AWSTemporaryCredentials(
                        accessKeyId,
                        secretAccessKey,
                        sessionToken,
                        Instant.fromEpochSeconds(expiration)
                    )
                }
                else -> {
                    AWSCredentials(accessKeyId, secretAccessKey)
                }
            }
        }
    }
}

class AWSTemporaryCredentials(
    /**
     * {@inheritDoc}
     */
    override val accessKeyId: String,

    /**
     * {@inheritDoc}
     */
    override val secretAccessKey: String,

    /**
     * The session Token
     */
    val sessionToken: String,

    /**
     * The expiration.
     */
    val expiration: Instant
) : AWSCredentials(accessKeyId, secretAccessKey)

internal fun AWSCredentials.toSdkCredentials(): Credentials {
    return Credentials(
        accessKeyId = this.accessKeyId,
        secretAccessKey = this.secretAccessKey,
        sessionToken = (this as? AWSTemporaryCredentials)?.sessionToken,
        expiration = (this as? AWSTemporaryCredentials)?.expiration
    )
}
