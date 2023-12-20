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

package com.amplifyframework.predictions.aws.options

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSCredentialsProvider
import com.amplifyframework.predictions.options.FaceLivenessSessionOptions

@InternalAmplifyApi
open class AWSFaceLivenessSessionOptions private constructor(
    val credentialsProvider: AWSCredentialsProvider<AWSCredentials>?
) : FaceLivenessSessionOptions() {
    companion object {
        @JvmStatic fun builder() = Builder()
        @JvmStatic fun defaults() = builder().build()
    }

    class Builder : FaceLivenessSessionOptions.Builder<Builder>() {
        var credentialsProvider: AWSCredentialsProvider<AWSCredentials>? = null
            private set

        fun credentialsProvider(credentialsProvider: AWSCredentialsProvider<AWSCredentials>) =
            apply { this.credentialsProvider = credentialsProvider }

        override fun getThis() = this

        override fun build() = AWSFaceLivenessSessionOptions(credentialsProvider)
    }
}
