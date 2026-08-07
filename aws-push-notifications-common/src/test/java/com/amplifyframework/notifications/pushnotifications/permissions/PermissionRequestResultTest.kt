/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.notifications.pushnotifications.permissions

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

/**
 * Unit tests for [PermissionRequestResult].
 */
class PermissionRequestResultTest {

    @Test
    fun `granted is a singleton`() {
        val result: PermissionRequestResult = PermissionRequestResult.Granted
        result.shouldBeInstanceOf<PermissionRequestResult.Granted>()
    }

    @Test
    fun `not granted carries shouldShowRationale true`() {
        val result = PermissionRequestResult.NotGranted(shouldShowRationale = true)
        result.shouldShowRationale shouldBe true
    }

    @Test
    fun `not granted carries shouldShowRationale false`() {
        val result = PermissionRequestResult.NotGranted(shouldShowRationale = false)
        result.shouldShowRationale shouldBe false
    }

    @Test
    fun `not granted equality is based on shouldShowRationale`() {
        PermissionRequestResult.NotGranted(true) shouldBe PermissionRequestResult.NotGranted(true)
    }
}
