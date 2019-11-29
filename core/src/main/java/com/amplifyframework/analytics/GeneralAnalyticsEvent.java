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
public class GeneralAnalyticsEvent extends AnalyticsEventType {

    /**
     * Construct a general analytics event.
     *
     * @param eventType name for the event.
     * @param properties event properties.
     */
    public GeneralAnalyticsEvent(String eventType, Properties properties) {
        setEventType(eventType);
        setProperties(properties);
    }
}
