/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.statemachine.codegen.data

import android.app.Activity
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.statemachine.util.mask
import java.lang.ref.WeakReference

/**
 * Class that accumulates the information needed to sign in via WebAuthn. The data may be built up over time.
 */
internal data class WebAuthnSignInContext(
    val username: String,
    val callingActivity: WeakReference<Activity>,
    val session: String?,
    val requestJson: String? = null,
    val responseJson: String? = null
) {
    override fun toString() = "WebAuthnSignInContext(" +
        "username='$username', " +
        "callingActivity='$callingActivity', " +
        "session='${session.mask()}', " +
        "requestJson='${requestJson.mask()}', " +
        "responseJson='${responseJson.mask()}'" +
        ")"
}

internal fun WebAuthnSignInContext.requireRequestJson(): String =
    requestJson ?: throw InvalidStateException("Missing request json")
internal fun WebAuthnSignInContext.requireResponseJson(): String =
    responseJson ?: throw InvalidStateException("Missing response json")
