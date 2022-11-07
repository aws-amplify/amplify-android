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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * Stores device information such as name and ID.
 */
public final class AuthDevice {
    private final String id;
    private final String name;

    @SuppressWarnings("checkstyle:ParameterName")
    private AuthDevice(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Constructs an {@link AuthDevice} with just the ID.
     * @param id ID to assign to this device
     * @return Auth device with just the ID
     */
    @SuppressWarnings("checkstyle:ParameterName")
    public static AuthDevice fromId(@NonNull String id) {
        return fromId(id, null);
    }

    /**
     * Constructs an {@link AuthDevice} with both the name and ID.
     * @param id ID to assign to this device
     * @param name user-friendly name to assign to this device
     * @return Auth device with both the name and ID
     */
    @SuppressWarnings("checkstyle:ParameterName")
    public static AuthDevice fromId(@NonNull String id,
                                    @Nullable String name) {
        return new AuthDevice(Objects.requireNonNull(id), name);
    }

    /**
     * Gets the unique identifier of this Auth device.
     * @return the device ID
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Gets the user-friendly name of this Auth device if assigned any.
     * @return the device name
     */
    @Nullable
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getId(),
                getName()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthDevice authDevice = (AuthDevice) obj;
            return ObjectsCompat.equals(getId(), authDevice.getId()) &&
                    ObjectsCompat.equals(getName(), authDevice.getName());
        }
    }

    @Override
    public String toString() {
        return "AuthDevice{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
