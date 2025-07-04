/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.auth.cognito.activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.amplifyframework.auth.cognito.activities.WebViewActivity.Companion.createResponseHandlingIntent
import com.amplifyframework.core.Amplify

/**
 * Handles auth redirect for sign-in and sign-out.
 */
internal class HostedUIRedirectActivity : Activity() {

    override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)
        startActivity(
            createResponseHandlingIntent(
                this,
                intent.data
            )
        )
    }

    override fun onResume() {
        super.onResume()
        Log.d("AuthClient", "Handling auth redirect response")
        Amplify.Auth.handleWebUISignInResponse(intent)
        finish()
    }
}
