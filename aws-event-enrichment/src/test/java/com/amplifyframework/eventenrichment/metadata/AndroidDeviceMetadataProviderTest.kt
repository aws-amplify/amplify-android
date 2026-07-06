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
package com.amplifyframework.eventenrichment.metadata

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidDeviceMetadataProviderTest {

    @Test
    fun `resolves real device metadata instead of an empty object`() {
        val metadata = AndroidDeviceMetadataProvider().getDeviceMetadata()

        // Guards against the "empty device" regression: platform is always set.
        metadata.platform shouldBe "Android"
        metadata.platformVersion.shouldNotBeNull()
        metadata.manufacturer.shouldNotBeNull()
        metadata.model.shouldNotBeNull()
        metadata.locale.shouldNotBeNull()
    }
}
