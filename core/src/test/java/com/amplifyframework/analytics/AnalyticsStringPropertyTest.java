/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.analytics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AnalyticsStringProperty}.
 */
public final class AnalyticsStringPropertyTest {
    /**
     * The {@link AnalyticsStringProperty#from(String)} factory will create a property
     * and store the provided string. It will be available via
     * {@link AnalyticsStringProperty#getValue()}.
     */
    @Test
    public void fromReturnsStringPropertyWithPassedValue() {
        // Arrange: Create the property
        AnalyticsStringProperty property = AnalyticsStringProperty.from("what hath God wrought");

        // Assert: Check that the wrapped value is what was passed in the static factory method
        assertEquals("what hath God wrought", property.getValue());
    }
}
