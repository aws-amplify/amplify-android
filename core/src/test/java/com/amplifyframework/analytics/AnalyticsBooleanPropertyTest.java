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
 * Tests the {@link AnalyticsBooleanProperty}.
 */
public class AnalyticsBooleanPropertyTest {
    /**
     * The {@link AnalyticsBooleanProperty#from(Boolean)} factory will create a property,
     * and its value will encode either {@link Boolean#TRUE}, or {@link Boolean#FALSE},
     * depending on what was provided.
     */
    @Test
    public void fromReturnsBooleanPropertyWithPassedValue() {
        // Arrange: Create the properties
        AnalyticsBooleanProperty trueProperty = AnalyticsBooleanProperty.from(true);
        AnalyticsBooleanProperty falseProperty = AnalyticsBooleanProperty.from(false);

        // Assert: Check that the wrapped values are what was passed in the static factory methods
        assertEquals(Boolean.TRUE, trueProperty.getValue());
        assertEquals(Boolean.FALSE, falseProperty.getValue());
    }
}
