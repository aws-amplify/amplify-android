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

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.amplifyframework.util.Time;

/**
 * This is the activity to display the developer menu.
 */
public final class DeveloperMenuActivity extends Activity {
    // A device movement is classified as a shake if the acceleration
    // is above this threshold.
    private static final int SHAKE_THRESHOLD = 12;
    // The minimum duration (in milliseconds) that the device needs to
    // be shaken in order to make the developer menu appear or disappear.
    private static final int SHAKE_TIME = 500;

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
                    changeVisibility();
                    shakeStart = 0;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

    // Manager for the device's sensors.
    private SensorManager sensorManager;
    // The accelerometer sensor associated with the device.
    private Sensor accelerometer;
    // The time (in milliseconds) that the device started shaking
    // (or 0 if the device is not shaking).
    private long shakeStart;
    // The parent layout for the developer menu.
    private View devMenuLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_menu);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        devMenuLayout = findViewById(R.id.devLayout);
        devMenuLayout.setFocusable(true);
        devMenuLayout.setVisibility(View.GONE);
        shakeStart = 0;
    }

    @Override
    protected void onResume() {
        if (accelerometer != null) {
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (accelerometer != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
        super.onPause();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // KEYCODE_MENU is the code for pressing ctrl
        // (or command) + m
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            changeVisibility();
            return true;
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    /**
     * If the developer menu is visible, hide the developer menu. Otherwise,
     * make the developer menu visible.
     */
    private void changeVisibility() {
        if (devMenuLayout.getVisibility() == View.VISIBLE) {
            devMenuLayout.setVisibility(View.GONE);
        } else {
            devMenuLayout.setVisibility(View.VISIBLE);
        }
    }
}
