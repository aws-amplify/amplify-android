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

package com.amplifyframework.analytics.pinpoint;

import com.amplifyframework.analytics.Property;

/**
 * Represent pinpoint attributes.
 */
public final class StringProperty implements Property<String> {
    private String value;

    StringProperty(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Instantiate {@link StringProperty} from a {@link String} value.
     * @param value a floating point number.
     * @return an instance of {@link StringProperty}
     */
    public StringProperty of(String value) {
        return new StringProperty(value);
    }
}
