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

import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.Sleep;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link ShakeDetector} behavior.
 */
@RunWith(RobolectricTestRunner.class)
public class ShakeDetectorTest {
    // Amount of time in milliseconds between sending sensor
    // events to the sensor listener.
    private static final int SENSOR_DATA_POLLING_INTERVAL_MS = 65;
    // Maximum amount of time in seconds before a test times out if the callback
    // method has not been called.
    private static final int SHAKE_RESULT_TIMEOUT_SECS = 5;

    /**
     * Test that the callback method is called when a shake
     * event is triggered.
     */
    @Test
    public void shakeTriggersCallback() {
        simulateSensorEvent("shake-accel-values.csv");
    }

    /**
     * Test that the callback method is not called when
     * no shake is detected.
     */
    @Test(expected = RuntimeException.class)
    public void noShakeDetected() {
        simulateSensorEvent("no-shake-accel-values.csv");
    }

    /**
     * Test that the callback method is not called
     * when the sensor listener has not been registered.
     */
    @Test(expected = RuntimeException.class)
    public void beforeSensorListenerRegistered() {
        Completable.create(emitter -> {
            ShakeDetector sd = new ShakeDetector(mock(Context.class), emitter::onComplete);
        }).observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .timeout(SHAKE_RESULT_TIMEOUT_SECS, TimeUnit.SECONDS)
                .blockingAwait();
    }

    /**
     * Test that the callback method is not called
     * after the sensor listener has been unregistered.
     */
    @Test(expected = RuntimeException.class)
    public void afterSensorListenerUnregistered() {
        Completable.create(emitter -> {
            ShakeDetector sd = new ShakeDetector(mock(Context.class), emitter::onComplete);
            sd.startDetecting();
            sd.stopDetecting();
        }).observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .timeout(SHAKE_RESULT_TIMEOUT_SECS, TimeUnit.SECONDS)
                .blockingAwait();
    }

    /**
     * For each line in the CSV file with the given name, creates and starts a thread that sends
     * a sensor event with the corresponding data in the CSV file to the sensor listener and waits
     * for the callback method to be called (times out after TIMEOUT seconds).
     * @param accelDataFileName Name of the CSV file containing acceleration data.
     */
    private void simulateSensorEvent(String accelDataFileName) {
        Context mockContext = mock(Context.class);
        SensorManager mockSensorManager = mock(SensorManager.class);
        when(mockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mockSensorManager);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mock(Sensor.class));
        AtomicReference<Thread> streamingThread = new AtomicReference<>();
        doAnswer(invocation -> {
            int indexOfListener = 0; // First argument passed to registerListener(...)
            SensorEventListener listener = invocation.getArgument(indexOfListener);
            streamingThread.set(createStreamingThread(processAccelCsv(accelDataFileName), listener));
            streamingThread.get().start();
            return true;
        }).when(mockSensorManager).registerListener(any(SensorEventListener.class), any(Sensor.class), anyInt());

        Completable.create(emitter -> {
            ShakeDetector sd = new ShakeDetector(mockContext, emitter::onComplete);
            sd.startDetecting();
        }).observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .timeout(SHAKE_RESULT_TIMEOUT_SECS, TimeUnit.SECONDS)
                .blockingAwait();
    }

    /**
     * Converts each line in the CSV file with the given name to a float[].
     * @param fileName Name of CSV file containing the acceleration data.
     * @return List of float[] where each element in the List corresponds to
     *         one line in the CSV file.
     */
    private List<float[]> processAccelCsv(String fileName) {
        List<float[]> sensorValues = new ArrayList<>();
        for (String line : Resources.readLines(fileName)) {
            String[] currentLine = line.split(",");
            float[] accelValues = new float[currentLine.length];
            for (int i = 0; i < currentLine.length; i++) {
                accelValues[i] = Float.parseFloat(currentLine[i]);
            }
            sensorValues.add(accelValues);
        }
        return sensorValues;
    }

    /**
     * Creates a new thread that when started will send a sensor event to the given
     * SensorEventListener for each element in the given List.
     * @param sensorValues List where each element represents the acceleration in the x, y, and z directions.
     * @param listener SensorEventListener object.
     * @return a Thread object.
     */
    private Thread createStreamingThread(List<float[]> sensorValues, SensorEventListener listener) {
        return new Thread(() -> {
            for (float[] accelValues : sensorValues) {
                SensorEvent mockEvent = mock(SensorEvent.class);
                try {
                    Field sensorValuesField = SensorEvent.class.getField("values");
                    sensorValuesField.setAccessible(true);
                    sensorValuesField.set(mockEvent, accelValues);
                    listener.onSensorChanged(mockEvent);
                    Sleep.milliseconds(SENSOR_DATA_POLLING_INTERVAL_MS);
                } catch (NoSuchFieldException | IllegalAccessException reflectionFailure) {
                    throw new RuntimeException(reflectionFailure);
                }
            }
        });
    }
}
