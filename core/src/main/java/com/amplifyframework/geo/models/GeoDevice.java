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

package com.amplifyframework.geo.models;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.geo.GeoException;

/**
 Amplify generated Device ID Characteristcs

 <tiedTo>User
 1. Device ID consistent across sessions, tied to specific user but not device:
 - <cognito-identity.amazonaws.com:sub>

 <tiedTo>UserAndDevice
 2. Device ID consistent across sessions, tied to specific user and device combination:
 - <cognito-identity.amazonaws.com:sub>-<UUID generated for, and stored on, device>

 <tiedTo>Device
 3. Device ID consistent across sessions, tied to specific device but not user:
 - <UUID generated for, and stored on, device>
 (This situation would by definition not use authorization)
 */
public class GeoDevice {
    String id;
    GeoDeviceType type;

    public String getId() {
        return id;
    }

    public GeoDeviceType getType() {
        return type;
    }

    private GeoDevice (String id, GeoDeviceType type) {
        this.id = id;
        this.type = type;
    }

    public static GeoDevice createUncheckedId(String id) {
        return new GeoDevice(id, GeoDeviceType.UNCHECKED);
    }

    public static GeoDevice createIdTiedToUser() {
        return new GeoDevice("", GeoDeviceType.USER);
    }

    public static GeoDevice createIdTiedToUserAndDevice() {
        return new GeoDevice("", GeoDeviceType.USER_AND_DEVICE);
    }

    public static GeoDevice createIdTiedToDevice() {
        return new GeoDevice("", GeoDeviceType.DEVICE);
    }
}
