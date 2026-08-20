/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.helpers

import android.app.Activity
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions
import com.amplifyframework.auth.cognito.options.AuthWebUIPrompt
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Test

class HostedUIHelperTest {

    private val mockActivity = mockk<Activity>()

    @Test
    fun `createHostedUIOptions with AWSCognitoAuthWebUISignInOptions and preferPrivateSession true`() {
        val scopes = listOf("openid", "profile")
        val options = AWSCognitoAuthWebUISignInOptions.builder()
            .scopes(scopes)
            .browserPackage("com.android.chrome")
            .preferPrivateSession(true)
            .build()

        val hostedUIOptions = HostedUIHelper.createHostedUIOptions(
            callingActivity = mockActivity,
            authProvider = AuthProvider.google(),
            options = options
        )

        hostedUIOptions.callingActivity shouldBe mockActivity
        hostedUIOptions.scopes shouldBe scopes
        hostedUIOptions.browserPackage shouldBe "com.android.chrome"
        hostedUIOptions.preferPrivateSession shouldBe true
    }

    @Test
    fun `createHostedUIOptions with AWSCognitoAuthWebUISignInOptions and preferPrivateSession false`() {
        val scopes = listOf("openid", "email")
        val options = AWSCognitoAuthWebUISignInOptions.builder()
            .scopes(scopes)
            .browserPackage("org.mozilla.firefox")
            .preferPrivateSession(false)
            .build()

        val hostedUIOptions = HostedUIHelper.createHostedUIOptions(
            callingActivity = mockActivity,
            authProvider = AuthProvider.facebook(),
            options = options
        )

        hostedUIOptions.callingActivity shouldBe mockActivity
        hostedUIOptions.scopes shouldBe scopes
        hostedUIOptions.browserPackage shouldBe "org.mozilla.firefox"
        hostedUIOptions.preferPrivateSession shouldBe false
    }

    @Test
    fun `createHostedUIOptions with AWSCognitoAuthWebUISignInOptions and preferPrivateSession null`() {
        val scopes = listOf("openid")
        val options = AWSCognitoAuthWebUISignInOptions.builder()
            .scopes(scopes)
            .browserPackage("com.android.chrome")
            .build()

        val hostedUIOptions = HostedUIHelper.createHostedUIOptions(
            callingActivity = mockActivity,
            authProvider = null,
            options = options
        )

        hostedUIOptions.callingActivity shouldBe mockActivity
        hostedUIOptions.scopes shouldBe scopes
        hostedUIOptions.browserPackage shouldBe "com.android.chrome"
        hostedUIOptions.preferPrivateSession shouldBe null
    }

    @Test
    fun `createHostedUIOptions with Cognito OIDC parameters`() {
        val options = AWSCognitoAuthWebUISignInOptions.builder()
            .nonce("nonce")
            .language("en")
            .loginHint("username")
            .prompt(AuthWebUIPrompt.LOGIN, AuthWebUIPrompt.CONSENT)
            .resource("myapp://")
            .build()

        val hostedUIOptions = HostedUIHelper.createHostedUIOptions(
            callingActivity = mockActivity,
            authProvider = null,
            options = options
        )

        hostedUIOptions.callingActivity shouldBe mockActivity
        hostedUIOptions.nonce shouldBe "nonce"
        hostedUIOptions.language shouldBe "en"
        hostedUIOptions.loginHint shouldBe "username"
        hostedUIOptions.prompt shouldBe listOf(AuthWebUIPrompt.LOGIN, AuthWebUIPrompt.CONSENT)
        hostedUIOptions.resource shouldBe "myapp://"
    }

    @Test
    fun `selectRedirectUri prefers non-HTTP scheme`() {
        val redirectUris = listOf(
            "https://example.com/callback",
            "myapp://auth/callback",
            "http://localhost:3000/callback"
        )

        val selectedUri = HostedUIHelper.selectRedirectUri(redirectUris)
        selectedUri shouldBe "myapp://auth/callback"
    }

    @Test
    fun `selectRedirectUri returns first URI when no non-HTTP scheme available`() {
        val redirectUris = listOf(
            "https://example.com/callback",
            "http://localhost:3000/callback"
        )

        val selectedUri = HostedUIHelper.selectRedirectUri(redirectUris)
        selectedUri shouldBe "https://example.com/callback"
    }

    @Test
    fun `selectRedirectUri returns null for empty list`() {
        val redirectUris = emptyList<String>()
        val selectedUri = HostedUIHelper.selectRedirectUri(redirectUris)
        selectedUri shouldBe null
    }
}
