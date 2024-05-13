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

package com.amplifyframework.pushnotifications.pinpoint

import com.amplifyframework.notifications.NotificationsException
import com.amplifyframework.testutils.configuration.amplifyOutputsData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test

class AWSPinpointPushNotificationsConfigurationTest {
    @Test
    fun `extracts values from amplify outputs`() {
        val data = amplifyOutputsData {
            notifications {
                awsRegion = "test-region"
                amazonPinpointAppId = "test-app-id"
            }
        }

        val configuration = AWSPinpointPushNotificationsConfiguration.from(data)

        configuration.appId shouldBe "test-app-id"
        configuration.region shouldBe "test-region"
    }

    @Test
    fun `throws exception if notifications section missing from amplify outputs`() {
        val data = amplifyOutputsData {
            // do not set notifications config
        }

        shouldThrow<NotificationsException> {
            AWSPinpointPushNotificationsConfiguration.from(data)
        }
    }
}
