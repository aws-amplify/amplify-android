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

package com.amplifyframework.foundation.credentials

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Provides access to the AWS credentials used for accessing AWS services: AWS
 * access key ID and secret access key. These credentials are used to securely
 * sign requests to AWS services.
 *
 * For more details on AWS access keys,
 * [see](https://docs.aws.amazon.com/general/latest/gr/aws-security-credentials.html)
 */
sealed interface AwsCredentials {
    /**
     * The AWS access key ID for this credentials object.
     */
    val accessKeyId: String

    /**
     * The AWS secret access key for this credentials object.
     */
    val secretAccessKey: String

    /**
     * Long-term AWS credentials, such as those granted to Admin Users
     */
    open class Static(
        override val accessKeyId: String,
        override val secretAccessKey: String
    ) : AwsCredentials

    /**
     * Temporary credentials, such as those vended by STS
     */
    @OptIn(ExperimentalTime::class)
    open class Temporary(
        override val accessKeyId: String,
        override val secretAccessKey: String,
        val sessionToken: String,
        val expiration: Instant
    ) : AwsCredentials
}
