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
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

/**
 * This is the activity to display the developer menu.
 */
public final class DeveloperMenuActivity extends Activity {
    // The parent layout for the developer menu.
    private View devMenuLayout;
    // Detect and handle shake events.
    private ShakeDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_menu);
        devMenuLayout = findViewById(R.id.dev_layout);
        devMenuLayout.setFocusable(true);
        devMenuLayout.setVisibility(View.GONE);
        detector = new ShakeDetector(getApplicationContext(), this::changeVisibility);
    }

    @Override
    protected void onResume() {
        detector.startDetecting();
        super.onResume();
    }

    @Override
    protected void onPause() {
        detector.stopDetecting();
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
