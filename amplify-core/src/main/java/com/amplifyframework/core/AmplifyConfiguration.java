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

package com.amplifyframework.core;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amplifyframework.analytics.AnalyticsCategoryConfiguration;
import com.amplifyframework.api.ApiCategoryConfiguration;
import com.amplifyframework.hub.HubCategoryConfiguration;
import com.amplifyframework.logging.LoggingCategoryConfiguration;
import com.amplifyframework.storage.StorageCategoryConfiguration;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

/**
 * AmplifyConfiguration parses the configuration from
 * the amplifyconfiguration.json file and stores in the
 * in-memory objects for the different Amplify plugins to
 * use.
 */
public class AmplifyConfiguration {

    public AnalyticsCategoryConfiguration analytics;
    public ApiCategoryConfiguration api;
    public HubCategoryConfiguration hub;
    public LoggingCategoryConfiguration logging;
    public StorageCategoryConfiguration storage;

    static final String DEFAULT_IDENTIFIER = "amplifyconfiguration";

    private JSONObject mJSONObject;

    /**
     * Constructor.
     *
     * @param jsonObject JSONObject that contains the configuration.
     */
    public AmplifyConfiguration(JSONObject jsonObject) {
        this.mJSONObject = jsonObject;
    }

    /**
     * Constructor.
     *
     * @param context The configuration information can be read
     *                from the default amplify configuration file.
     */
    public AmplifyConfiguration(Context context) {
        readInputJson(context, getConfigResourceId(context));
    }

    private static int getConfigResourceId(Context context) {
        try {
            return context.getResources().getIdentifier(DEFAULT_IDENTIFIER,
                    "raw", context.getPackageName());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to read " + DEFAULT_IDENTIFIER
                            + " please check that it is correctly formed.",
                    e);
        }
    }

    private void readInputJson(Context context, int resourceId) {
        try {
            final InputStream inputStream = context.getResources().openRawResource(
                    resourceId);
            final Scanner in = new Scanner(inputStream);
            final StringBuilder sb = new StringBuilder();
            while (in.hasNextLine()) {
                sb.append(in.nextLine());
            }
            in.close();

            this.mJSONObject = new JSONObject(sb.toString());
        } catch (Exception je) {
            throw new RuntimeException(
                    "Failed to read " + DEFAULT_IDENTIFIER + " please check that it is correctly formed.",
                    je);
        }
    }
}
