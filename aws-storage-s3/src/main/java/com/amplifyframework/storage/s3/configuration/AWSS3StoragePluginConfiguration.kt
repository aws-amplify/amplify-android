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

class AWSS3StoragePluginConfiguration private constructor(builder: Builder) {

    private val awsS3PluginPrefixResolver = builder.awsS3PluginPrefixResolver

    companion object {
        operator fun invoke(block: Builder.() -> Unit): AWSS3StoragePluginConfiguration =
            Builder()
                .apply(block)
                .build()
    }

    fun getAWSS3PluginPrefixResolver(authCredentialsProvider: AuthCredentialsProvider): AWSS3PluginPrefixResolver {
        return awsS3PluginPrefixResolver ?: StorageAccessLevelAwarePrefixResolver(
            authCredentialsProvider
        )
    }

    class Builder {
        var awsS3PluginPrefixResolver: AWSS3PluginPrefixResolver? = null

        fun build(): AWSS3StoragePluginConfiguration = AWSS3StoragePluginConfiguration(this)
    }
}
