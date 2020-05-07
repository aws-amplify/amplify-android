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
 * Tests the {@link AnalyticsEvent}.
 */
public final class AnalyticsEventTest {

    /**
     * An {@link AnalyticsEvent} has a property name, and some various properties
     * which encode different value types. These same values that are passed while
     * building an {@link AnalyticsEvent} (and its properties) should be available
     * by means of getters.
     */
    @Test
    public void builderConfiguresCompleteBasicAnalyticsEvent() {
        // Arrange: Build a new AnalyticsEvent
        AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                .name("user.login")
                .addProperty("TimeZone", "EDT")
                .addProperty("inGeoFence", false)
                .addProperty("LoginDuration", 98.36)
                .addProperty("ProfileCount", 2)
                .build();

        // Assert: Verify the builder correctly constructed the AnalyticsEvent
        assertEquals("user.login", analyticsEvent.getName());

        final AnalyticsProperties properties = analyticsEvent.getProperties();
        final String timeZone = ((AnalyticsStringProperty) properties.get("TimeZone")).getValue();
        final Boolean inGeoFence = ((AnalyticsBooleanProperty) properties.get("inGeoFence")).getValue();
        final Double loginDuration = ((AnalyticsDoubleProperty) properties.get("LoginDuration")).getValue();
        final Integer profileCount = ((AnalyticsIntegerProperty) properties.get("ProfileCount")).getValue();

        assertEquals(4, properties.size());
        assertEquals("EDT", timeZone);
        assertEquals(Boolean.FALSE, inGeoFence);
        assertEquals(Double.valueOf(98.36), loginDuration);
        assertEquals(Integer.valueOf(2), profileCount);
    }
}
