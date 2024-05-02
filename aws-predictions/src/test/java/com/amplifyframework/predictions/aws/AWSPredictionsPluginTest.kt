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

package com.amplifyframework.predictions.aws

import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.testutils.configuration.amplifyOutputsData
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.mockk
import org.junit.Test

class AWSPredictionsPluginTest {
    @Test
    fun `throws exception when trying to configure with AmplifyOutputs`() {
        val data = amplifyOutputsData {
            // no predictions configuration supported
        }

        val plugin = AWSPredictionsPlugin()

        shouldThrow<PredictionsException> {
            plugin.configure(data, mockk())
        }
    }
}
