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

import com.amplifyframework.hub.BackgroundExecutorHubPlugin;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubPayloadFilter;
import com.amplifyframework.hub.HubListener;
import com.amplifyframework.hub.HubPayload;

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

        performHubOperations();
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

    private void performHubOperations() {

        BackgroundExecutorHubPlugin backgroundExecutorHubPlugin = new BackgroundExecutorHubPlugin();

        backgroundExecutorHubPlugin.subscribe(HubChannel.STORAGE, new HubListener() {
            @Override
            public void onEvent(@NonNull HubPayload payload) {
                if (payload.getEventData() instanceof String) {
                    Log.d(TAG, "String: => " + payload.getEventName() + ":" + (String) payload.getEventData());
                } else if (payload.getEventData() instanceof Float) {
                    Log.d(TAG, "Float: => " + payload.getEventName() + ":" + (Float) payload.getEventData());
                }
            }
        });

        backgroundExecutorHubPlugin.subscribe(HubChannel.STORAGE, new HubPayloadFilter() {
            @Override
            public boolean filter(@NonNull HubPayload payload) {
                return true;
            }
        }, new HubListener() {
            @Override
            public void onEvent(@NonNull HubPayload payload) {
                if (payload.getEventData() instanceof String) {
                    Log.d(TAG, "String: => " + payload.getEventName() + ":" + (String) payload.getEventData());
                } else if (payload.getEventData() instanceof Float) {
                    Log.d(TAG, "Float: => " + payload.getEventName() + ":" + (Float) payload.getEventData());
                }
            }
        });

        backgroundExecutorHubPlugin.publish(HubChannel.STORAGE,
                new HubPayload("weatherString", new String("Too Cold in Seattle")));

        backgroundExecutorHubPlugin.publish(HubChannel.STORAGE,
                new HubPayload("weatherFloat", 5.3F));
    }
}
