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

package com.amplifyframework.testutils;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Test base for Amplify instrumentation tests. Any configurable information should
 * be stored inside "testconfiguration.json", and obtained via static
 * {@link AmplifyTestBase#getPackageConfigure(String)} method.
 */
public abstract class AmplifyTestBase {

    private static final String TEST_CONFIGURATION_IDENTIFIER = "testconfiguration";
    private static final String TEST_CONFIGURATION_FILENAME = "testconfiguration.json";

    private static JSONConfiguration mJSONConfiguration;

    protected static JSONObject getPackageConfigure(String packageName) {
        return getJSONConfiguration().getPackageConfigure(packageName);
    }

    private static int getConfigResourceId(Context context) {
        try {
            return context.getResources().getIdentifier(
                    TEST_CONFIGURATION_IDENTIFIER, "raw",
                    context.getPackageName()
            );
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to locate " + TEST_CONFIGURATION_IDENTIFIER,
                    exception
            );
        }
    }

    private static JSONConfiguration getJSONConfiguration() {
        if (mJSONConfiguration == null) {
            try {
                Context context = ApplicationProvider.getApplicationContext();
                final InputStream inputStream = context.getResources()
                        .openRawResource(getConfigResourceId(context));
                final Scanner in = new Scanner(inputStream);
                final StringBuilder sb = new StringBuilder();
                while (in.hasNextLine()) {
                    sb.append(in.nextLine());
                }
                in.close();

                mJSONConfiguration = new JSONConfiguration(new JSONObject(sb.toString()));
            } catch (JSONException jsonException) {
                throw new RuntimeException(
                        "Failed to read " + TEST_CONFIGURATION_FILENAME +
                                ". Please check that it is correctly formed.",
                        jsonException);
            }
        }
        return mJSONConfiguration;
    }

    private static class JSONConfiguration {
        private JSONObject mJSONObject;

        JSONConfiguration(JSONObject mJSONObject) {
            this.mJSONObject = mJSONObject;
        }

        private JSONObject getPackages() {
            try {
                return mJSONObject.getJSONObject("Packages");
            } catch (JSONException jsonException) {
                throw new RuntimeException(
                        "Failed to get Packages from " + TEST_CONFIGURATION_FILENAME +
                                ". Please check that it is correctly formed.",
                        jsonException);
            }
        }

        private JSONObject getPackageConfigure(String packageName) {
            try {
                return getPackages().getJSONObject(packageName);
            } catch (JSONException jsonException) {
                throw new RuntimeException(
                        "Failed to get package configuration from " +
                                TEST_CONFIGURATION_FILENAME + ":" + packageName,
                        jsonException);
            }
        }
    }
}
