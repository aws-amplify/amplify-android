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

import java.util.Date;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AnalyticsPropertiesTest {
    @Test
    public void builderConfiguresCompleteProperties() {
        // Arrange: Build a new AnalyticsProperties object
        AnalyticsProperties properties = AnalyticsProperties.builder()
                .add("TimeZone", "EDT")
                .add("inGeoFence", false)
                .add("LoginDuration", 98.36)
                .add("ProfileCount", 2)
                .build();

        // Assert: Verify the builder correctly constructed the AnalyticsProperties
        assertEquals(4, properties.size());

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

    @Test
    public void builderCanAcceptTypesThatImplementAnalyticsPropertiesBehavior() {
        // Arrange: Build a FooProperty and pass it to an AnalyticsProperties Builder
        Date date = new Date();
        FooProperty property = new FooProperty(date);
        AnalyticsProperties properties = AnalyticsProperties.builder()
                .add("SignUpDate", property)
                .build();

        // Assert: Ensure we can retrieve the FooProperty value
        assertEquals(date, properties.get("SignUpDate").getValue());
    }

    @Test
    public void getRaisesNoSuchElementExceptionWhenPropertyNotFound() {
        AnalyticsProperties properties = AnalyticsProperties.builder().build();

        assertThrows(NoSuchElementException.class, () -> {
            properties.get("key-that-doesnt-exist");
        });
    }

    class FooProperty implements AnalyticsPropertyBehavior<Date> {
        private final Date value;

        FooProperty(Date value) {
            this.value = value;
        }

        @Override
        public Date getValue() {
            return value;
        }
    }
}
