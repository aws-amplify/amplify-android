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
    private final String deviceId;
    private final String deviceName;

    private AuthDevice(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    /**
     * Constructs an {@link AuthDevice} with just the ID.
     * @param deviceId ID to assign to this device
     * @return Auth device with just the ID
     */
    public static AuthDevice fromId(@NonNull String deviceId) {
        return fromId(deviceId, null);
    }

    /**
     * Constructs an {@link AuthDevice} with both the name and ID.
     * @param deviceId ID to assign to this device
     * @param deviceName user-friendly name to assign to this device
     * @return Auth device with both the name and ID
     */
    public static AuthDevice fromId(@NonNull String deviceId,
                                    @Nullable String deviceName) {
        return new AuthDevice(Objects.requireNonNull(deviceId), deviceName);
    }

    /**
     * Gets the unique identifier of this Auth device.
     * @return the device ID
     */
    @NonNull
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Gets the user-friendly name of this Auth device if assigned any.
     * @return the device name
     */
    @Nullable
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getDeviceId(),
                getDeviceName()
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
            return ObjectsCompat.equals(getDeviceId(), authDevice.getDeviceId()) &&
                    ObjectsCompat.equals(getDeviceName(), authDevice.getDeviceName());
        }
    }

    @Override
    public String toString() {
        return "AuthDevice{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                '}';
    }
}
