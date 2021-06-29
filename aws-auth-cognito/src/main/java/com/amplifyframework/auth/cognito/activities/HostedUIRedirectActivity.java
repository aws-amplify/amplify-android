/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.amplifyframework.core.Amplify;

import com.amazonaws.mobileconnectors.cognitoauth.activities.CustomTabsManagerActivity;

/**
 * Handles auth redirect for hosted-UI sign-in and sign-out.
 */
public final class HostedUIRedirectActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        Uri redirectUri = getIntent().getData();
        Intent redirectHandler = CustomTabsManagerActivity.createResponseHandlingIntent(this, redirectUri);
        startActivity(redirectHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
        Amplify.Auth.handleWebUISignInResponse(getIntent());
        finish();
    }
}
