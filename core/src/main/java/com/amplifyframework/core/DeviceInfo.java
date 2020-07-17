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
     * Device manufacturer.
     */
    public static final String MANUFACTURER = Build.MANUFACTURER;
    /**
     * Name of the product/device.
     */
    public static final String MODEL = Build.MODEL;
    /**
     * SDK version running on the device.
     */
    public static final int SDK_VERSION = Build.VERSION.SDK_INT;

    /**
     * Returns true if the device is an emulator and false if not.
     * @return a boolean indicating whether the device is an emulator or not
     */
    public boolean isEmulator() {
        return Build.DEVICE.toLowerCase(Locale.getDefault()).contains("generic")
                && MODEL.toLowerCase(Locale.getDefault()).contains("sdk");
    }

    /**
     * Returns a String representation of the device information.
     * @return a String containing device information.
     */
    public String toString() {
        String result = "Device Manufacturer: " + MANUFACTURER + "\nDevice Model: " + MODEL
                + "\nSDK Version: " + SDK_VERSION + "\nDevice is an Emulator: ";
        if (isEmulator()) {
            return result + "Yes";
        } else {
            return result + "No";
        }
    }
}
