/*
 *
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 *
 *
 */

package com.amplifyframework.geo.options;

import com.amplifyframework.geo.GeoException;

import java.util.HashMap;
import java.util.Map;

public class GeoPositionProperties {
    Map<String, String> properties = new HashMap<String, String>();
    private int MAX_PROPERTIES = 3;
    private int MAX_KEY_LENGTH = 20;
    private int MAX_VALUE_LENGTH = 40;

    void addProperty(String key, String value) throws GeoException {
        if (properties.size() == MAX_PROPERTIES) {
            throw new GeoException("Exceeded max number of position properties allowed.",
                    "Limit the number of position properties to at most" + MAX_PROPERTIES);
        }
        if (!key.isEmpty() && !value.isEmpty()) {
            String keyToAdd = key.substring(0, MAX_KEY_LENGTH);
            String valueToAdd = value.substring(0, MAX_VALUE_LENGTH);
            properties.put(keyToAdd, valueToAdd);
        }
    }

    void removeProperty(String key) {
        properties.remove(key.substring(0, MAX_KEY_LENGTH));
    }

    int getNumberOfProperties() {
        return properties.size();
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
