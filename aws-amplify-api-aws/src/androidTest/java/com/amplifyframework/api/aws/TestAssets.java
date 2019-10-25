/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A little utility read the contents of the androidTest assets
 * directory at runtime, on a device.
 */
public final class TestAssets {
    /**
     * Dis-allows instantiation of this test utility.
     */
    private TestAssets() {
    }

    /**
     * Reads a test asset as a string.
     * @param name Name of asset in the assets dir
     * @return The string contents of the named asset
     */
    public static String readAsString(final String name) {
        try {
            final Context context = ApplicationProvider.getApplicationContext();
            final InputStream inputStream = context.getAssets().open(name);
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            final StringBuilder stringBuilder = new StringBuilder();

            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }

            return stringBuilder.toString();

        } catch (final IOException ioException) {
            throw new RuntimeException("Failed to load asset " + name, ioException);
        }
    }
}

