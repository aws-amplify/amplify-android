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
import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.ProgressStallTimeout
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageException
import io.mockk.mockk
import junit.framework.TestCase

class AWSS3StoragePluginConfigurationTest : TestCase() {

    fun testGetAWSS3PluginPrefixResolver() {
        val customAWSS3PluginPrefixResolver = object : AWSS3PluginPrefixResolver {
            override fun resolvePrefix(
                accessLevel: StorageAccessLevel,
                targetIdentity: String?,
                onSuccess: Consumer<String>,
                onError: Consumer<StorageException>?
            ) {
                onSuccess.accept("")
            }
        }
        val authCredentialsProvider = mockk<AuthCredentialsProvider>()
        val awsS3StoragePluginConfiguration = AWSS3StoragePluginConfiguration {
            awsS3PluginPrefixResolver = customAWSS3PluginPrefixResolver
        }
        val resultS3PluginPrefixResolver =
            awsS3StoragePluginConfiguration.getAWSS3PluginPrefixResolver(authCredentialsProvider)
        assertEquals(resultS3PluginPrefixResolver, customAWSS3PluginPrefixResolver)
    }

    fun testGetDefaultAWSS3PluginPrefixResolver() {
        val awsS3StoragePluginConfiguration = AWSS3StoragePluginConfiguration {}
        val authCredentialsProvider = mockk<AuthCredentialsProvider>()
        val awsS3PluginPrefixResolver =
            awsS3StoragePluginConfiguration.getAWSS3PluginPrefixResolver(authCredentialsProvider)
        assert(awsS3PluginPrefixResolver is StorageAccessLevelAwarePrefixResolver)
    }

    /**
     * When no [ProgressStallTimeout] is supplied via the builder, the plugin configuration
     * should default to [ProgressStallTimeout.Disabled] to preserve existing upload behavior.
     *
     * - Given: a plugin configuration built without overriding `progressStallTimeout`
     * - When: the configuration is constructed
     * - Then: `progressStallTimeout` equals [ProgressStallTimeout.Disabled]
     */
    fun testProgressStallTimeoutDefaultsToDisabled() {
        val configuration = AWSS3StoragePluginConfiguration {}
        assertEquals(ProgressStallTimeout.Disabled, configuration.progressStallTimeout)
    }

    /**
     * A custom [ProgressStallTimeout.Interval] provided on the builder must be honored by the
     * resulting configuration so that it can be propagated to uploads that do not override it.
     *
     * - Given: a builder that sets `progressStallTimeout` to an [ProgressStallTimeout.Interval]
     * - When: the configuration is built
     * - Then: the same [ProgressStallTimeout.Interval] is exposed on the configuration
     */
    fun testProgressStallTimeoutIntervalPropagatesFromBuilder() {
        val interval = ProgressStallTimeout.Interval(seconds = 15L)
        val configuration = AWSS3StoragePluginConfiguration {
            progressStallTimeout = interval
        }
        assertEquals(interval, configuration.progressStallTimeout)
    }

    /**
     * Non-positive intervals must not attempt to schedule a stall timer. The [ProgressStallTimeout]
     * sealed class reports `secondsForStallTimer = 0` for such values, effectively disabling
     * detection while still preserving the original user-provided configuration object.
     *
     * - Given: a configuration with an [ProgressStallTimeout.Interval] of `0` seconds
     * - When: `secondsForStallTimer` is read from the configured timeout
     * - Then: the returned value is `0`, disabling the stall timer
     */
    fun testProgressStallTimeoutZeroIntervalDisablesStallTimer() {
        val configuration = AWSS3StoragePluginConfiguration {
            progressStallTimeout = ProgressStallTimeout.Interval(seconds = 0L)
        }
        assertEquals(0L, configuration.progressStallTimeout.secondsForStallTimer)
    }
}
