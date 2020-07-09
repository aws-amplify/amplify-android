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
import android.widget.ImageButton;

/**
 * This is the activity to display the developer menu.
 */
public final class DeveloperMenuActivity extends Activity {
    // Parent layout for the developer menu.
    private View devMenuLayout;
    // Detect and handle shake events.
    private ShakeDetector detector;
    // View containing the buttons on the main
    // developer menu view.
    private View buttons;
    // Environment information view.
    private View envInfoView;
    // Device information view.
    private View deviceInfoView;
    // Logs view.
    private View logsView;
    // File issue view.
    private View fileIssueView;
    // Back button.
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_menu);
        devMenuLayout = findViewById(R.id.dev_layout);
        devMenuLayout.setFocusable(true);
        devMenuLayout.setVisibility(View.GONE);
        detector = new ShakeDetector(getApplicationContext(), this::changeVisibility);

        buttons = findViewById(R.id.buttons);
        envInfoView = findViewById(R.id.env_layout);
        deviceInfoView = findViewById(R.id.device_layout);
        logsView = findViewById(R.id.logs_layout);
        fileIssueView = findViewById(R.id.file_issue_layout);

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(view -> displayMainView());

        // Set on-click listeners for each button on the main view of the developer menu.
        findViewById(R.id.env_button).setOnClickListener(view -> leaveMainView(envInfoView));
        findViewById(R.id.device_button).setOnClickListener(view -> leaveMainView(deviceInfoView));
        findViewById(R.id.logs_button).setOnClickListener(view -> leaveMainView(logsView));
        findViewById(R.id.file_issue_button).setOnClickListener(view -> leaveMainView(fileIssueView));
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
        if (devMenuLayout.getVisibility() != View.VISIBLE) {
            super.onBackPressed();
        } else if (backButton.getVisibility() == View.VISIBLE) {
            displayMainView();
        } else {
            devMenuLayout.setVisibility(View.GONE);
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

    /**
     * Hides the main developer menu view and displays the given View.
     * @param newView View to display.
     */
    private void leaveMainView(View newView) {
        buttons.setVisibility(View.GONE);
        newView.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
    }

    /**
     * Hide the current view and display the main developer menu view.
     */
    private void displayMainView() {
        backButton.setVisibility(View.GONE);
        envInfoView.setVisibility(View.GONE);
        deviceInfoView.setVisibility(View.GONE);
        logsView.setVisibility(View.GONE);
        fileIssueView.setVisibility(View.GONE);
        buttons.setVisibility(View.VISIBLE);
    }
}
