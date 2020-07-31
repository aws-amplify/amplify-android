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

package com.amplifyframework.devmenu;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.amplifyframework.util.Time;

/**
 * Detects a device shake event.
 */
public final class ShakeDetector {
    // A device movement is classified as a shake if the acceleration
    // is above this threshold.
    private static final double SHAKE_THRESHOLD = 11.7;
    // The minimum duration (in milliseconds) that the device needs to
    // be shaken in order to make the developer menu appear or disappear.
    private static final int SHAKE_TIME = 500;

    // Listener to handle shake events.
    private final ShakeDetector.Listener listener;
    // Manager for the device's sensors.
    private SensorManager sensorManager;
    // The accelerometer sensor associated with the device.
    private Sensor accelerometer;
    // The time (in milliseconds) that the device started shaking
    // (or 0 if the device is not shaking).
    private long shakeStart;

    // Listen to accelerometer sensor events.
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float xAccel = sensorEvent.values[0]; // acceleration in the x-direction
            float yAccel = sensorEvent.values[1]; // acceleration in the y-direction
            float zAccel = sensorEvent.values[2]; // acceleration in the z-direction
            double curAcceleration = Math.sqrt(((xAccel * xAccel) + (yAccel * yAccel) + (zAccel * zAccel)));
            if (curAcceleration > SHAKE_THRESHOLD) {
                long currentTime = Time.now();
                if (shakeStart == 0) {
                    shakeStart = currentTime;
                } else if (currentTime - shakeStart > SHAKE_TIME) {
                    listener.onShakeDetected();
                    shakeStart = 0;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

    /**
     * Gain access to the accelerometer sensor.
     * @param context An Android Context
     * @param listener ShakeDetector.Listener object
     */
    public ShakeDetector(Context context, ShakeDetector.Listener listener) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        this.listener = listener;
        shakeStart = 0;
    }

    /**
     * Start listening to the accelerometer sensor for a shake event.
     */
    public void startDetecting() {
        if (accelerometer != null) {
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * Stop listening to the accelerometer sensor for a shake event.
     */
    public void stopDetecting() {
        if (accelerometer != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    /**
     * Interface to handle shake events when they occur.
     */
    public interface Listener {
        /**
         * Called when a shake event is detected.
         */
        void onShakeDetected();
    }
}
