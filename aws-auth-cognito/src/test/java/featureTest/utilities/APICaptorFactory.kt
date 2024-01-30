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

package featureTest.utilities

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CountDownLatch

/**
 * Factory with association of results captor to top level APIs
 */
class APICaptorFactory(
    private val authApi: ExpectationShapes.Amplify,
    private val latch: CountDownLatch, // ToDo: Remove this param
) {
    companion object {
        val onSuccess = mapOf(
            AuthAPI.resetPassword to mockk<Consumer<AuthResetPasswordResult>>(),
            AuthAPI.signUp to mockk<Consumer<AuthSignUpResult>>(),
            AuthAPI.signIn to mockk<Consumer<AuthSignInResult>>(),
            AuthAPI.deleteUser to mockk<Action>(),
            AuthAPI.fetchAuthSession to mockk<AuthSession>(),
            AuthAPI.getCurrentUser to mockk<AuthUser>(),
            AuthAPI.rememberDevice to mockk<Action>(),
            AuthAPI.forgetDevice to mockk<Action>()
        )
        val onError = mockk<Consumer<AuthException>>()
        val onComplete = mapOf(
            AuthAPI.signOut to mockk<Consumer<AuthSignOutResult>>()
        )
        val successCaptors: MutableMap<AuthAPI, CapturingSlot<*>> = mutableMapOf()
        val completeCaptors: MutableMap<AuthAPI, CapturingSlot<*>> = mutableMapOf()
        val errorCaptor = slot<AuthException>()
        val actionCaptor = slot<Map<String, Any>>().apply {
            captured = emptyMap()
        }
    }

    init {
        successCaptors.clear()
        completeCaptors.clear()
        if (authApi.responseType == ResponseType.Success) setupOnSuccess()
        if (authApi.responseType == ResponseType.Complete) setupOnComplete()
        else setupOnError()
    }

    private fun setupOnSuccess() {
        when (val apiName = authApi.apiName) {
            AuthAPI.resetPassword -> {
                val resultCaptor = slot<AuthResetPasswordResult>()
                val consumer = onSuccess[apiName] as Consumer<AuthResetPasswordResult>
                every { consumer.accept(capture(resultCaptor)) } answers { latch.countDown() }
                successCaptors[apiName] = resultCaptor
            }
            AuthAPI.signUp -> {
                val resultCaptor = slot<AuthSignUpResult>()
                val consumer = onSuccess[apiName] as Consumer<AuthSignUpResult>
                every { consumer.accept(capture(resultCaptor)) } answers { latch.countDown() }
                successCaptors[apiName] = resultCaptor
            }
            AuthAPI.signIn -> {
                val resultCaptor = slot<AuthSignInResult>()
                val consumer = onSuccess[apiName] as Consumer<AuthSignInResult>
                every { consumer.accept(capture(resultCaptor)) } answers { latch.countDown() }
                successCaptors[apiName] = resultCaptor
            }
            AuthAPI.deleteUser -> {
                val consumer = onSuccess[apiName] as Action
                every { consumer.call() } answers { latch.countDown() }
                successCaptors[apiName] = actionCaptor
            }
            AuthAPI.fetchAuthSession -> {
                val consumer = onSuccess[apiName] as Action
                every { consumer.call() } answers { latch.countDown() }
                successCaptors[apiName] = actionCaptor
            }
            AuthAPI.getCurrentUser -> {
                val consumer = onSuccess[apiName] as Action
                every { consumer.call() } answers { latch.countDown() }
                successCaptors[apiName] = actionCaptor
            }
            AuthAPI.rememberDevice -> {
                val consumer = onSuccess[apiName] as Action
                every { consumer.call() } answers { latch.countDown() }
                successCaptors[apiName] = actionCaptor
            }
            AuthAPI.forgetDevice -> {
                val consumer = onSuccess[apiName] as Action
                every { consumer.call() } answers { latch.countDown() }
                successCaptors[apiName] = actionCaptor
            }
            AuthAPI.fetchDevices -> {
                val consumer = onSuccess[apiName] as Action
                every { consumer.call() } answers { latch.countDown() }
                successCaptors[apiName] = actionCaptor
            }
            AuthAPI.fetchUserAttributes -> {
                val consumer = onSuccess[apiName] as Action
                every { consumer.call() } answers { latch.countDown() }
                successCaptors[apiName] = actionCaptor
            }
            else -> throw Error("onSuccess for $authApi is not defined!")
        }
    }

    private fun setupOnComplete() {
        when (val apiName = authApi.apiName) {
            AuthAPI.signOut -> {
                val resultCaptor = slot<AuthSignOutResult>()
                val consumer = onComplete[apiName] as Consumer<AuthSignOutResult>
                every { consumer.accept(capture(resultCaptor)) } answers { latch.countDown() }
                completeCaptors[apiName] = resultCaptor
            }
            else -> throw Error("onComplete for $authApi is not defined!")
        }
    }

    private fun setupOnError() {
        every { onError.accept(capture(errorCaptor)) } answers { latch.countDown() }
    }
}
