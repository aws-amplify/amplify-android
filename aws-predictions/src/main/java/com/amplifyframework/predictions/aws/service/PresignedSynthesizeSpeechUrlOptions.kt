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
package com.amplifyframework.predictions.aws.service

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials

class PresignedSynthesizeSpeechUrlOptions private constructor(
    val credentials: Credentials?,
    val expires: Int
){
    companion object {
        @JvmStatic fun builder() = Builder()
        @JvmStatic fun defaults() = builder().build()
        // Default expiration time is 15 minutes (900 seconds)
        private const val DEFAULT_EXPIRATION_SECONDS = 900
    }
    
    class Builder {
        // By default, the credentials provided by the CredentialsProvider in the predictions plugin are used
        var credentials: Credentials? = null
            private set
        var expires: Int = DEFAULT_EXPIRATION_SECONDS
            private set
        fun credentials(credentials: Credentials) = apply { this.credentials = credentials }
        fun expires(expires: Int) = apply { this.expires = expires }
        fun build() = PresignedSynthesizeSpeechUrlOptions(credentials, expires)
    }
}