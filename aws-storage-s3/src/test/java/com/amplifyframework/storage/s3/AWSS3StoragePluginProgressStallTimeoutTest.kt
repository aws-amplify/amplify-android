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

package com.amplifyframework.storage.s3

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.NoOpConsumer
import com.amplifyframework.storage.ProgressStallTimeout
import com.amplifyframework.storage.StorageCategory
import com.amplifyframework.storage.StorageCategoryConfiguration
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions
import com.amplifyframework.storage.s3.configuration.AWSS3StoragePluginConfiguration
import com.amplifyframework.storage.s3.options.AWSS3StorageUploadFileOptions
import com.amplifyframework.storage.s3.options.AWSS3StorageUploadInputStreamOptions
import com.amplifyframework.storage.s3.service.AWSS3StorageService
import com.amplifyframework.storage.s3.service.StorageService
import com.amplifyframework.storage.s3.transfer.StorageTransferClientProvider
import com.amplifyframework.storage.s3.transfer.TransferObserver
import com.amplifyframework.storage.s3.transfer.TransferStatusUpdater
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import java.io.ByteArrayInputStream
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AWSS3StoragePluginProgressStallTimeoutTest {

    private lateinit var storageService: StorageService
    private lateinit var authCredentialsProvider: AuthCredentialsProvider
    private lateinit var transferObserver: TransferObserver

    @Before
    fun setup() {
        storageService = mock(AWSS3StorageService::class.java)
        authCredentialsProvider = mockk(relaxed = true)
        coEvery { authCredentialsProvider.getIdentityId() } returns "test-identity"
        transferObserver = mock(TransferObserver::class.java)
        `when`(
            storageService.uploadFile(
                anyString(),
                anyString(),
                any(),
                any(),
                anyBoolean(),
                org.mockito.ArgumentMatchers.anyLong()
            )
        ).thenReturn(transferObserver)
        `when`(
            storageService.uploadInputStream(
                anyString(),
                anyString(),
                any(),
                any(),
                anyBoolean(),
                org.mockito.ArgumentMatchers.anyLong()
            )
        ).thenReturn(transferObserver)
    }

    private fun configuredPlugin(pluginConfiguration: AWSS3StoragePluginConfiguration): AWSS3StoragePlugin {
        val factory = object : AWSS3StorageService.Factory {
            override fun create(
                context: Context,
                region: String,
                bucketName: String,
                clientProvider: StorageTransferClientProvider,
                transferStatusUpdater: TransferStatusUpdater
            ): AWSS3StorageService = storageService as AWSS3StorageService
        }
        val plugin = AWSS3StoragePlugin(factory, authCredentialsProvider, pluginConfiguration)
        // Wire the plugin into a configured StorageCategory so executors and config are initialized.
        val category = StorageCategory()
        category.addPlugin(plugin)
        category.configure(buildConfiguration(), ApplicationProvider.getApplicationContext())
        category.initialize(ApplicationProvider.getApplicationContext())
        return plugin
    }

    private fun buildConfiguration(): StorageCategoryConfiguration {
        val configuration = StorageCategoryConfiguration()
        try {
            configuration.populateFromJSON(
                JSONObject().put(
                    "plugins",
                    JSONObject().put(
                        "awsS3StoragePlugin",
                        JSONObject()
                            .put("region", "us-east-1")
                            .put("bucket", "test-bucket")
                    )
                )
            )
        } catch (jsonException: org.json.JSONException) {
            throw AssertionError(jsonException)
        } catch (amplifyException: AmplifyException) {
            throw AssertionError(amplifyException)
        }
        return configuration
    }

    private fun captureUploadFileStallSeconds(): Long {
        val captor = ArgumentCaptor.forClass(Long::class.java)
        // The plugin schedules the actual `storageService.uploadFile` call on its internal
        // executor, so verification needs to wait for the asynchronous work to land.
        verify(storageService, timeout(5_000)).uploadFile(
            anyString(),
            anyString(),
            any(),
            any(),
            anyBoolean(),
            captor.capture()
        )
        return captor.value
    }

    private fun captureUploadInputStreamStallSeconds(): Long {
        val captor = ArgumentCaptor.forClass(Long::class.java)
        verify(storageService, timeout(5_000)).uploadInputStream(
            anyString(),
            anyString(),
            any(),
            any(),
            anyBoolean(),
            captor.capture()
        )
        return captor.value
    }

    /**
     * Test that the plugin-level default of [ProgressStallTimeout.Disabled] resolves to `0` when
     * the upload options do not supply a per-upload override. This is the legacy behavior — no
     * stall detection is armed for the upload.
     *
     * - Given: a plugin configured with `ProgressStallTimeout.Disabled` and a default upload options
     * - When: `uploadFile` is invoked with a string key
     * - Then: `StorageService.uploadFile` is called with `progressStallTimeoutSeconds = 0`
     */
    @Test
    fun `string key uploadFile with no override and Disabled plugin default uses zero seconds`() {
        val plugin = configuredPlugin(AWSS3StoragePluginConfiguration {})

        plugin.uploadFile(
            "test-key",
            java.io.File.createTempFile("any", ".tmp"),
            StorageUploadFileOptions.defaultInstance(),
            NoOpConsumer.create(),
            NoOpConsumer.create<com.amplifyframework.storage.result.StorageUploadFileResult>(),
            Consumer<StorageException> { /* no-op */ }
        )

        captureUploadFileStallSeconds() shouldBe 0L
    }

    /**
     * Test that the plugin-level default `Interval` flows through to the storage service when no
     * per-upload override is supplied. This covers the "non-Disabled" branch of the plugin
     * configuration without any options-side overrides.
     *
     * - Given: a plugin configured with `ProgressStallTimeout.Interval(seconds = 25)`
     *   and a default upload options
     * - When: `uploadFile` is invoked with a string key
     * - Then: `StorageService.uploadFile` is called with `progressStallTimeoutSeconds = 25`
     */
    @Test
    fun `string key uploadFile uses plugin Interval default when override is null`() {
        val plugin = configuredPlugin(
            AWSS3StoragePluginConfiguration {
                progressStallTimeout = ProgressStallTimeout.Interval(seconds = 25L)
            }
        )

        plugin.uploadFile(
            "test-key",
            java.io.File.createTempFile("any", ".tmp"),
            StorageUploadFileOptions.defaultInstance(),
            NoOpConsumer.create(),
            NoOpConsumer.create<com.amplifyframework.storage.result.StorageUploadFileResult>(),
            Consumer<StorageException> { /* no-op */ }
        )

        captureUploadFileStallSeconds() shouldBe 25L
    }

    /**
     * Test that a per-upload [ProgressStallTimeout.Interval] override wins over the plugin-level
     * default. This is the primary feature path: upload-level options must take precedence so
     * apps can opt individual uploads in or out of stall detection without changing the plugin
     * configuration.
     *
     * - Given: a plugin configured with `ProgressStallTimeout.Disabled` and AWS S3 upload options
     *   carrying a per-upload `Interval(60)` override
     * - When: `uploadFile` is invoked with a string key
     * - Then: `StorageService.uploadFile` is called with `progressStallTimeoutSeconds = 60`
     */
    @Test
    fun `string key uploadFile uses per-upload Interval override over Disabled plugin default`() {
        val plugin = configuredPlugin(AWSS3StoragePluginConfiguration {})
        val options = AWSS3StorageUploadFileOptions.builder()
            .progressStallTimeout(ProgressStallTimeout.Interval(seconds = 60L))
            .build()

        plugin.uploadFile(
            "test-key",
            java.io.File.createTempFile("any", ".tmp"),
            options,
            NoOpConsumer.create(),
            NoOpConsumer.create<com.amplifyframework.storage.result.StorageUploadFileResult>(),
            Consumer<StorageException> { /* no-op */ }
        )

        captureUploadFileStallSeconds() shouldBe 60L
    }

    /**
     * Test that a per-upload `Disabled` override wins over a plugin-level `Interval` default,
     * letting an individual upload opt out of detection even when the plugin enables it.
     *
     * - Given: a plugin configured with `ProgressStallTimeout.Interval(seconds = 30)` and AWS S3
     *   upload options carrying a per-upload `Disabled` override
     * - When: `uploadFile` is invoked with a `StoragePath`
     * - Then: `StorageService.uploadFile` is called with `progressStallTimeoutSeconds = 0`
     */
    @Test
    fun `path uploadFile uses per-upload Disabled override over Interval plugin default`() {
        val plugin = configuredPlugin(
            AWSS3StoragePluginConfiguration {
                progressStallTimeout = ProgressStallTimeout.Interval(seconds = 30L)
            }
        )
        val options = AWSS3StorageUploadFileOptions.builder()
            .progressStallTimeout(ProgressStallTimeout.Disabled)
            .build()

        plugin.uploadFile(
            StoragePath.fromString("public/test-key"),
            java.io.File.createTempFile("any", ".tmp"),
            options,
            NoOpConsumer.create(),
            NoOpConsumer.create<com.amplifyframework.storage.result.StorageUploadFileResult>(),
            Consumer<StorageException> { /* no-op */ }
        )

        captureUploadFileStallSeconds() shouldBe 0L
    }

    /**
     * Test the `instanceof` fallback branch: when a caller supplies a plain
     * [StorageUploadInputStreamOptions] (not an AWS-specific subclass), the plugin must fall
     * back to its configured default rather than treating the upload as an explicit override.
     *
     * - Given: a plugin configured with `ProgressStallTimeout.Interval(seconds = 15)` and a
     *   non-AWS `StorageUploadInputStreamOptions` instance
     * - When: `uploadInputStream` is invoked with a string key
     * - Then: `StorageService.uploadInputStream` is called with `progressStallTimeoutSeconds = 15`
     */
    @Test
    fun `string key uploadInputStream falls back to plugin default for generic options`() {
        val plugin = configuredPlugin(
            AWSS3StoragePluginConfiguration {
                progressStallTimeout = ProgressStallTimeout.Interval(seconds = 15L)
            }
        )
        val options = StorageUploadInputStreamOptions.defaultInstance()

        plugin.uploadInputStream(
            "test-key",
            ByteArrayInputStream(byteArrayOf(1, 2, 3)),
            options,
            NoOpConsumer.create(),
            NoOpConsumer.create<com.amplifyframework.storage.result.StorageUploadInputStreamResult>(),
            Consumer<StorageException> { /* no-op */ }
        )

        captureUploadInputStreamStallSeconds() shouldBe 15L
    }

    /**
     * Test that a per-upload [ProgressStallTimeout.Interval] override is honored on the
     * `StoragePath` overload of `uploadInputStream`. This mirrors the same precedence rules
     * proven for `uploadFile` but exercises the input-stream code path.
     *
     * - Given: a plugin configured with `ProgressStallTimeout.Disabled` and AWS S3 upload options
     *   carrying a per-upload `Interval(90)` override
     * - When: `uploadInputStream` is invoked with a `StoragePath`
     * - Then: `StorageService.uploadInputStream` is called with `progressStallTimeoutSeconds = 90`
     */
    @Test
    fun `path uploadInputStream uses per-upload Interval override over Disabled plugin default`() {
        val plugin = configuredPlugin(AWSS3StoragePluginConfiguration {})
        val options = AWSS3StorageUploadInputStreamOptions.builder()
            .progressStallTimeout(ProgressStallTimeout.Interval(seconds = 90L))
            .build()

        plugin.uploadInputStream(
            StoragePath.fromString("public/test-key"),
            ByteArrayInputStream(byteArrayOf(1, 2, 3)),
            options,
            NoOpConsumer.create(),
            NoOpConsumer.create<com.amplifyframework.storage.result.StorageUploadInputStreamResult>(),
            Consumer<StorageException> { /* no-op */ }
        )

        captureUploadInputStreamStallSeconds() shouldBe 90L
    }
}
