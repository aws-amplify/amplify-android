/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

/**
 * Represents event in its most general form and can have different properties.
 */
public final class BasicAnalyticsEvent implements AnalyticsEventType {
    private final String name;
    private final Properties properties;

    /**
     * Construct a general analytics event.
     *
     * @param name name for the event.
     * @param properties event properties.
     */
    public BasicAnalyticsEvent(String name, Properties properties) {
        this.name = name;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }
}
