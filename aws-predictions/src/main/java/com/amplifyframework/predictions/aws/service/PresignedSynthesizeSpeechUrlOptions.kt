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

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider

/**
 * Stores options to use when presigning a URL for a synthesize speech request.
 */
class PresignedSynthesizeSpeechUrlOptions private constructor(
    val credentialsProvider: CredentialsProvider?,
    val expires: Int
){
    companion object {
        /**
         * Returns a new builder instance for constructing PresignedSynthesizeSpeechUrlOptions.
         * @return a new builder instance.
         */
        @JvmStatic fun builder() = Builder()

        /**
         * Returns a new PresignedSynthesizeSpeechUrlOptions instance with default values.
         * @return a default instance.
         */
        @JvmStatic fun defaults() = builder().build()
        
        // Default expiration time is 15 minutes (900 seconds)
        private const val DEFAULT_EXPIRATION_SECONDS = 900
    }

    /**
     * Builder class for constructing a PresignedSynthesizeSpeechUrlOptions instance.
     */
    class Builder {
        // Default is to use the same CredentialsProvider used in the predictions plugin
        var credentialsProvider: CredentialsProvider? = null
            private set
        var expires: Int = DEFAULT_EXPIRATION_SECONDS
            private set

        /**
         * Sets the credentials provider and returns itself.
         * @param credentialsProvider the credentials provider that will provide credentials for signing the URL.
         * @return this builder instance.
         */
        fun credentialsProvider(credentialsProvider: CredentialsProvider) = apply { this.credentialsProvider = credentialsProvider }

        /**
         * Sets the expiration of the URL and returns itself.
         * @param expires number of seconds before the presigned URL expires.
         * @return this builder instance.
         */
        fun expires(expires: Int) = apply { this.expires = expires }

        /**
         * Constructs a new instance of PresignedSynthesizeSpeechUrlOptions using this builder.
         * @return a PresignedSynthesizeSpeechUrlOptions instance with the properties of this builder.
         */
        fun build() = PresignedSynthesizeSpeechUrlOptions(credentialsProvider, expires)
    }
}