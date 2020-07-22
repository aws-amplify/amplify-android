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

package com.amplifyframework.core;

import android.os.Build;

import java.util.Locale;

/**
 * Contains information about the Android device currently
 * running the application.
 */
public final class DeviceInfo {
    /**
     * Returns true if the device is an emulator and false if not. Based on answers from
     * https://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application
     * -is-running-in-the-emulator
     * @return a boolean indicating whether the device is an emulator or not
     */
    public boolean isEmulator() {
        return Build.DEVICE.toLowerCase(Locale.getDefault()).contains("generic")
                && Build.MODEL.toLowerCase(Locale.getDefault()).contains("sdk");
    }

    /**
     * Returns a String representation of the device information.
     * @return a String containing device information.
     */
    public String toString() {
        return String.format(Locale.US, "Device Manufacturer: %s\nDevice Model: %s\nAndroid System Version: "
                + "%s\nSDK Version: %d\nDevice is an Emulator: %s", Build.MANUFACTURER, Build.MODEL,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT, isEmulator() ? "Yes" : "No");
    }
}
