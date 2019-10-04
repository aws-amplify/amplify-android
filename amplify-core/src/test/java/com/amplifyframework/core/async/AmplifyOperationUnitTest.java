/*
 * Copyright (c) 2019. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.amplifyframework.core.async;

import android.util.Log;

import com.amplifyframework.core.category.CategoryType;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class AmplifyOperationUnitTest {

    private static final String TAG = AmplifyOperationUnitTest.class.getSimpleName();

    @Test
    public void testCreation() {
        AmplifyOperationRequest<String> amplifyOperationRequest = new AmplifyOperationRequest<String>() {
            @Override
            String getRequestOptions() {
                return "input-parameters";
            }
        };

        AmplifyOperation<AmplifyOperationRequest<String>> amplifyOperation =
                new AmplifyOperation<AmplifyOperationRequest<String>>(CategoryType.ANALYTICS, amplifyOperationRequest) {
                    @Override
                    public void start() {
                        super.start();
                        Log.d(TAG, "Operation started.");
                    }
                };

        amplifyOperation.start();
        assertNotNull(amplifyOperation.getOperationId());
    }

    @Test
    public void testCreationWithListener() {
        AmplifyOperationRequest<String> amplifyOperationRequest = new AmplifyOperationRequest<String>() {
            @Override
            String getRequestOptions() {
                return "input-parameters";
            }
        };

        EventListener<String> eventListener = new EventListener<String>() {
            @Override
            public void onEvent(String event) {
                Log.d(TAG, event);
            }
        };

        AmplifyOperation<AmplifyOperationRequest<String>> amplifyOperation =
                new AmplifyOperation<AmplifyOperationRequest<String>>(CategoryType.ANALYTICS,
                        amplifyOperationRequest,
                        eventListener) {
                    @Override
                    public void start() {
                        super.start();
                        Log.d(TAG, "Operation started.");
                    }
                };

        amplifyOperation.start();
        assertNotNull(amplifyOperation.getOperationId());
    }
}