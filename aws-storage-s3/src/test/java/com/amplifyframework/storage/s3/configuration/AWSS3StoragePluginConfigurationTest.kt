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

import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.CognitoAuthProvider
import junit.framework.TestCase
import org.mockito.Mockito

class AWSS3StoragePluginConfigurationTest : TestCase() {

    fun testGetAWSS3PluginPrefixResolver() {
        val customAWSS3PluginPrefixResolver = object : AWSS3PluginPrefixResolver {
            override fun resolvePrefix(
                accessLevel: StorageAccessLevel,
                targetIdentity: String?,
                onSuccess: Consumer<String>,
                onError: Consumer<StorageException>
            ) {
                onSuccess.accept("")
            }
        }
        val cognitoAuthProvider = Mockito.mock(CognitoAuthProvider::class.java)
        val awsS3StoragePluginConfiguration = AWSS3StoragePluginConfiguration(
            AWSS3StoragePluginConfiguration.Builder().apply {
                awsS3PluginPrefixResolver = customAWSS3PluginPrefixResolver
            }
        )
        val resultS3PluginPrefixResolver =
            awsS3StoragePluginConfiguration.getAWSS3PluginPrefixResolver(cognitoAuthProvider)
        assertEquals(resultS3PluginPrefixResolver, customAWSS3PluginPrefixResolver)
    }

    fun testGetDefaultAWSS3PluginPrefixResolver() {
        val awsS3StoragePluginConfiguration = AWSS3StoragePluginConfiguration()
        val cognitoAuthProvider = Mockito.mock(CognitoAuthProvider::class.java)
        val awsS3PluginPrefixResolver =
            awsS3StoragePluginConfiguration.getAWSS3PluginPrefixResolver(cognitoAuthProvider)
        assert(awsS3PluginPrefixResolver is StorageAccessLevelAwarePrefixResolver)
    }
}
