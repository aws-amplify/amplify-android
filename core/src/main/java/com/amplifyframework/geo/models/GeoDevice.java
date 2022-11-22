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

    private GeoDevice (String id) {
        this.id = id;
    }

    public static GeoDevice createUncheckedId(String id) {
        return new GeoDevice(id);
    }

    public static GeoDevice createIdTiedToUser(Consumer<GeoDevice> onResult, Consumer<GeoException> onError) {
        // TODO("Blocked by ALS not allowing colons in ids");
        return new GeoDevice("id");
    }

    public static GeoDevice createIdTiedToUserAndDevice(Consumer<GeoDevice> onResult, Consumer<GeoException> onError) {
        // TODO("Blocked by ALS not allowing colons in ids");
        return new GeoDevice("id");
    }

    public static GeoDevice createIdTiedToDevice(Consumer<GeoDevice> onResult, Consumer<GeoException> onError) {
        // TODO("Not finished");
        return new GeoDevice("id");
    }
}

