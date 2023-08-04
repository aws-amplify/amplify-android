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

package com.amplifyframework.auth;

import androidx.core.util.ObjectsCompat;

/**
 * Holds the key and value for a user attribute.
 */
public final class AuthUserAttribute {
    private final AuthUserAttributeKey key;
    private final String value;

    /**
     * Holds the key and value for a user attribute.
     * @param key Attribute key
     * @param value Attribute value
     */
    public AuthUserAttribute(AuthUserAttributeKey key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Return the attribute key.
     * @return the attribute key
     */
    public AuthUserAttributeKey getKey() {
        return key;
    }

    /**
     * Return the attribute value.
     * @return the attribute value
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getKey(),
                getValue()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthUserAttribute attribute = (AuthUserAttribute) obj;
            return ObjectsCompat.equals(getKey(), attribute.getKey()) &&
                    ObjectsCompat.equals(getValue(), attribute.getValue());
        }
    }

    @Override
    public String toString() {
        return "AuthUserAttribute {" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
