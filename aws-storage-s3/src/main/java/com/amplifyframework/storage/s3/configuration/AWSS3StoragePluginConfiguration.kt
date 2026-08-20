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

package com.amplifyframework.storage.s3.configuration

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.storage.ProgressStallTimeout

class AWSS3StoragePluginConfiguration private constructor(builder: Builder) {

    private val awsS3PluginPrefixResolver = builder.awsS3PluginPrefixResolver

    /**
     * Default progress stall timeout applied to S3 uploads.
     *
     * When an upload does not report any forward progress within this interval, the transfer is
     * cancelled and the `onError` callback receives a [com.amplifyframework.storage.StorageException]
     * whose `cause` is a [com.amplifyframework.storage.ProgressStallTimeoutException]. The default is
     * [ProgressStallTimeout.Disabled], which preserves existing behavior.
     *
     * This value is used whenever a per-upload override has not been supplied on the upload options.
     */
    val progressStallTimeout: ProgressStallTimeout = builder.progressStallTimeout

    companion object {
        operator fun invoke(block: Builder.() -> Unit): AWSS3StoragePluginConfiguration = Builder()
            .apply(block)
            .build()
    }

    @Deprecated("Unused for operations using StoragePath")
    fun getAWSS3PluginPrefixResolver(authCredentialsProvider: AuthCredentialsProvider): AWSS3PluginPrefixResolver =
        awsS3PluginPrefixResolver ?: StorageAccessLevelAwarePrefixResolver(
            authCredentialsProvider
        )

    class Builder {
        @Deprecated("Unused for operations using StoragePath")
        var awsS3PluginPrefixResolver: AWSS3PluginPrefixResolver? = null

        /**
         * Default [ProgressStallTimeout] applied to uploads created by this plugin.
         *
         * Defaults to [ProgressStallTimeout.Disabled].
         */
        var progressStallTimeout: ProgressStallTimeout = ProgressStallTimeout.Disabled

        fun build(): AWSS3StoragePluginConfiguration = AWSS3StoragePluginConfiguration(this)
    }
}
