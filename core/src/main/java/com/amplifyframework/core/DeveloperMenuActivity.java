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

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

/**
 * This is the activity to display the developer menu.
 */
public final class DeveloperMenuActivity extends FragmentActivity {
    // Parent layout for the developer menu.
    private View devMenuLayout;
    // Detect and handle shake events.
    private ShakeDetector detector;
    // Manages app navigation.
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_menu);
        devMenuLayout = findViewById(R.id.dev_layout);
        devMenuLayout.setFocusable(true);
        devMenuLayout.setVisibility(View.GONE);
        detector = new ShakeDetector(getApplicationContext(), this::changeVisibility);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(findViewById(R.id.toolbar), navController,
                new AppBarConfiguration.Builder(navController.getGraph()).build());
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

    @Override
    public void onBackPressed() {
        NavDestination currentDestination = navController.getCurrentDestination();
        if (currentDestination != null && currentDestination.getId() == R.id.main_fragment) {
            devMenuLayout.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
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
