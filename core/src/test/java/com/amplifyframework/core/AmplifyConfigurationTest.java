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

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link AmplifyConfiguration} behavior.
 */
@RunWith(RobolectricTestRunner.class)
public class AmplifyConfigurationTest {
    private Context context = ApplicationProvider.getApplicationContext();

    /**
     * Attempting to call {@link AmplifyConfiguration#builder(Context)}
     * without a generated configuration file throws an AmplifyException.
     *
     * @throws Exception if the premise of the test is incorrect
     */
    @Test(expected = AmplifyException.class)
    public void testMissingConfigurationFileThrowsAmplifyException() throws Exception {
        AmplifyConfiguration.builder(context);
    }
}
