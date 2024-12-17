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

package com.amplifyframework.auth.cognito.helpers

import android.app.Activity
import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PublicKeyCredential
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.lang.ref.WeakReference
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebAuthnHelperTest {

    private val requestJson = """{"user":{"name":"test"}}"""
    private val responseJson = """{"response":"json"}"""

    private val context = mockk<Context>()
    private val credentialManager = mockk<CredentialManager> {
        coEvery { getCredential(any(), any<GetCredentialRequest>()) } returns
            GetCredentialResponse(PublicKeyCredential(responseJson))
        coEvery { createCredential(any(), any()) } returns CreatePublicKeyCredentialResponse(responseJson)
    }
    private val helper = WebAuthnHelper(
        context = context,
        credentialManager = credentialManager
    )

    @Test
    fun `gets credential`() = runTest {
        val result = helper.getCredential(requestJson, WeakReference(mockk()))
        result shouldBe responseJson
    }

    @Test
    fun `uses activity context if provided`() = runTest {
        val activity = mockk<Activity>()
        helper.getCredential(requestJson, WeakReference(activity))
        coVerify {
            credentialManager.getCredential(activity, any<GetCredentialRequest>())
        }
    }

    @Test
    fun `uses application context if activity is not provided`() = runTest {
        helper.getCredential(requestJson, WeakReference(null))
        coVerify {
            credentialManager.getCredential(context, any<GetCredentialRequest>())
        }
    }

    @Test
    fun `creates credential`() = runTest {
        val result = helper.createCredential(requestJson, mockk())
        result shouldBe responseJson
    }
}
