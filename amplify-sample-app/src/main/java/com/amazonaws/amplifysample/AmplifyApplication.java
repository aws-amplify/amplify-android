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

package com.amazonaws.amplifysample;

import android.app.Application;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.amplifyframework.analytics.pinpoint.AmazonPinpointAnalyticsPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.exception.AmplifyException;
import com.example.aws_amplify_api_okhttp.Callback;
import com.example.aws_amplify_api_okhttp.OkhttpApiPlugin;

public class AmplifyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

//        try {
            //Amplify.addPlugin(new AmazonPinpointAnalyticsPlugin(getApplicationContext()));
            //Amplify.addPlugin(new OkhttpApiPlugin());
            //Amplify.configure(getApplicationContext());

            //Amplify.API.query("Called via plugin");
//
            OkhttpApiPlugin okhttpApiPlugin = new OkhttpApiPlugin();
            okhttpApiPlugin.graphql("{ list {id name}}").enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull String data) {
                    Log.i("GRAPHQL", data.toString());
                }

                @Override
                public void onError(@NonNull Throwable error) {
                    Log.e("GRAPHQL", error.toString());
                }
            });

//        } catch (AmplifyException e) {
//            e.printStackTrace();
//        }
    }
}
