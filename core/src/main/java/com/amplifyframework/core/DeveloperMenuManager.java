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
import android.content.Intent;
import android.content.pm.ApplicationInfo;

/**
 * Manage the display and shake detection behavior for the
 * developer menu.
 */
public final class DeveloperMenuManager implements ShakeDetector.Listener {
    // An instance of DeveloperMenuManager.
    private static DeveloperMenuManager sInstance;
    // Indicates whether the developer menu is visible.
    private boolean visible;
    // Action to take when the developer menu should be hidden.
    private HideAction hideAction;
    // Android Context associated with the application.
    private Context context;

    /**
     * Constructs a new DeveloperMenuManager.
     * @param context Android Context
     */
    private DeveloperMenuManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Returns an instance of DeveloperMenuManager.
     * @param context Android Context
     * @return a DeveloperMenuManager
     */
    public static DeveloperMenuManager sharedInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DeveloperMenuManager(context);
        }
        return sInstance;
    }

    /**
     * Allows the developer menu to be activated if the app is in a debuggable build.
     */
    public void enableDeveloperMenu() {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Amplify.Logging.shouldStoreLogs(true);
            startListening();
        }
    }

    /**
     * Start listening for a shake event.
     */
    public void startListening() {
        ShakeDetector shakeDetector = new ShakeDetector(context, this);
        shakeDetector.startDetecting();
    }

    @Override
    public void onShakeDetected() {
        if (visible) {
            if (hideAction != null) {
                hideAction.hideDeveloperMenu();
            }
            visible = false;
        } else {
            Intent mainIntent = new Intent(context, DeveloperMenuActivity.class);
            mainIntent.setAction(Intent.ACTION_MAIN);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainIntent);
            visible = true;
        }
    }

    /**
     * Set the visibility of the developer menu.
     * @param visible boolean indicating whether the developer menu is visible.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Set the action to take when the developer menu should be hidden.
     * @param action HideAction
     */
    public void setOnHideAction(HideAction action) {
        hideAction = action;
    }

    /**
     * Interface to handle the action to take when the developer menu
     * should be hidden.
     */
    public interface HideAction {
        /**
         * Called when the developer menu should be hidden.
         */
        void hideDeveloperMenu();
    }
}
