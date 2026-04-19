/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package featureTest.utilities

import java.util.TimeZone
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TimeZoneRule(private val timeZone: TimeZone) : TestWatcher() {
    private val previous: TimeZone = TimeZone.getDefault()

    override fun starting(description: Description) {
        TimeZone.setDefault(timeZone)
    }

    override fun finished(description: Description) {
        TimeZone.setDefault(previous)
    }
}
