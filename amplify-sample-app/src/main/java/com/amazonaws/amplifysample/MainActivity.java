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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.async.AmplifyOperationContext;
import com.amplifyframework.core.async.AsyncEvent;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubFilter;
import com.amplifyframework.hub.HubListener;
import com.amplifyframework.hub.HubPayload;
import com.amplifyframework.hub.UnsubscribeToken;
import com.amplifyframework.storage.exception.StorageException;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

//        AnalyticsPlugin pinpoint1 = new AmazonPinpointAnalyticsPlugin(getApplicationContext());
//        AnalyticsPlugin pinpoint2 = new AmazonPinpointAnalyticsPlugin(getApplicationContext());
//        AnalyticsPlugin kinesis = new AmazonKinesisAnalyticsPlugin(getApplicationContext());
//        Amplify.addPlugin(pinpoint);
//        Amplify.addPlugin(kinesis);
//        Amplify.configure(getApplicationContext());
//
//        Amplify.Analytics.recordEvent(); // throws exception
//
//        Amplify.Analytics.getPlugin(pinpoint.getPluginKey()).recordEvent();
//        Amplify.Analytics.getPlugin(kinesis.getPluginKey()).recordEvent();

        UnsubscribeToken unsubscribeToken = Amplify.Hub.listen(HubChannel.STORAGE, new HubListener() {
            @Override
            public void onHubEvent(@NonNull HubPayload payload) {
                Log.d(TAG, payload.data.toString());
            }
        });

//        unsubscribeToken = Amplify.Hub.listen(HubChannel.STORAGE,
//                new HubFilter() {
//                    @Override
//                    public boolean filter(@NonNull HubPayload payload) {
//                        return payload.context instanceof AmplifyOperationContext;
//                    }
//                },
//                new HubListener() {
//                    @Override
//                    public void onHubEvent(@NonNull HubPayload payload) {
//                        Log.d(TAG, payload.data.toString());
//
//                        StorageFileOperation.Event event = (StorageFileOperation.Event) payload.data;
//                        if (AsyncEvent.State.COMPLETED.equals(event.getEventState())) {
//                            Log.d(TAG, "Result: " + ((StorageFileOperation.Event<StorageDownloadFileResult>)event).getEventData());
//                        } else if (AsyncEvent.State.FAILED.equals(event.getEventState())) {
//                            Log.d(TAG, "Error: " + ((StorageFileOperation.Event<StorageException>)event).getEventData());
//                        } else if (AsyncEvent.State.IN_PROCESS.equals(event.getEventState())) {
//                            Log.d(TAG, "Progress: " + ((StorageFileOperation.Event<Progress>)event).getEventData());
//                        }
//                    }
//                }
//        );

        Amplify.Hub.dispatch(HubChannel.CUSTOM, new HubPayload().eventName("Some event happened"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
