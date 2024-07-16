/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.options.SubpathStrategy
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test that ensures new Auth category APIs have a default implementation in the [AuthPlugin] class. This allows
 * 3rd party Auth plugins to compile against newer versions of Amplify.
 */
class SubpathStrategyTest {

    @Test
    fun `Exclude strategy returns default delimiter`() {
        // The purpose of this test is to ensure that TestPlugin compiles, the assertion is irrelevant
        val excludeSubpathStrategy = SubpathStrategy.Exclude()
        assertEquals("/", excludeSubpathStrategy.delimiter)
    }

    @Test
    fun `Exclude strategy returns overriden delimiter`() {
        // The purpose of this test is to ensure that TestPlugin compiles, the assertion is irrelevant
        val excludeSubpathStrategy = SubpathStrategy.Exclude("$")
        assertEquals("$", excludeSubpathStrategy.delimiter)
    }
}
