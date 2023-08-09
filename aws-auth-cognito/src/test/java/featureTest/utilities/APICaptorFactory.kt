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

import aws.smithy.kotlin.runtime.tracing.TraceEventData
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
import generated.model.ApiCall
import generated.model.Response
import generated.model.TypeResponse
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.*
import io.mockk.slot
import java.util.concurrent.CountDownLatch

/**
 * Factory with association of results captor to top level APIs
 */
class APICaptorFactory(
    private val authApi: ApiCall,
    private val responseType : TypeResponse,
    private val onSuccess : Any,
    private val onError : Consumer<AuthException>,
    val latch : CountDownLatch
) {
    lateinit var result : Any



    companion object {

        val onError = mockk<Consumer<AuthException>>()
        val onComplete = mapOf(
            AuthAPI.signOut.name to mockk<Consumer<AuthSignOutResult>>()
        )
        val successCaptors: MutableMap<String, CapturingSlot<*>> = mutableMapOf()
        val completeCaptors: MutableMap<String, CapturingSlot<*>> = mutableMapOf()
        val errorCaptor = slot<AuthException>()
        val actionCaptor = slot<Map<String, Any>>().apply {
            captured = emptyMap()
            isCaptured = true
        }
    }

    init {
        successCaptors.clear()
        completeCaptors.clear()
        if (responseType == TypeResponse.Success) setupOnSuccess()
        else if(responseType == TypeResponse.Complete || authApi.name == "signOut") setupOnComplete()
        else setupOnError()
    }

    private fun setupOnSuccess() {

        when (val apiName = authApi.name) {

            AuthAPI.resetPassword.name -> {
                val resultCaptor = slot<AuthResetPasswordResult>()
                val consumer = onSuccess as Consumer<AuthResetPasswordResult>
                every { consumer.accept(capture(resultCaptor)) } answers { latch.countDown() }
                result = resultCaptor
            }
            AuthAPI.signUp.name -> {
                val resultCaptor = slot<AuthSignUpResult>()
                val onSuccess : Consumer<AuthSignUpResult> = mockk()
                every { (onSuccess as Consumer<AuthSignUpResult>).accept(capture(resultCaptor)) } answers { latch.countDown() }
                result = resultCaptor
            }
            AuthAPI.signIn.name -> {
                val resultCaptor = slot<AuthSignInResult>()
                val consumer = onSuccess as Consumer<AuthSignInResult>
                every { consumer.accept(capture(resultCaptor)) } answers { latch.countDown() }
                result = resultCaptor
            }
            AuthAPI.deleteUser.name -> {
                val consumer = onSuccess as Action
                every { consumer.call() } answers { latch.countDown() }
                result = actionCaptor
            }
            AuthAPI.fetchAuthSession.name -> {
                val consumer = onSuccess as Action
                every { consumer.call() } answers { latch.countDown() }
                result = actionCaptor
            }
            AuthAPI.getCurrentUser.name -> {
                val consumer = onSuccess as Action
                every { consumer.call() } answers { latch.countDown() }
                result = actionCaptor
            }
            AuthAPI.rememberDevice.name -> {
                val consumer = onSuccess as Action
                every { consumer.call() } answers { latch.countDown() }
                result = actionCaptor
            }
            AuthAPI.forgetDevice.name -> {
                val consumer = onSuccess as Action
                every { consumer.call() } answers { latch.countDown() }
                result = actionCaptor
            }
            AuthAPI.fetchDevices.name -> {
                val consumer = onSuccess as Action
                every { consumer.call() } answers { latch.countDown() }
                result = actionCaptor
            }
            AuthAPI.fetchUserAttributes.name -> {
                val consumer = onSuccess as Action
                every { consumer.call() } answers { latch.countDown() }
                result = actionCaptor
            }
            else -> throw Error("onSuccess for $authApi is not defined!")
        }

    }

    private fun setupOnComplete() {

        when (val apiName = authApi.name) {
            AuthAPI.signOut.name -> {
                val resultCaptor = slot<AuthSignOutResult>()
                val consumer = onComplete[apiName] as Consumer<AuthSignOutResult>
                every { consumer.accept(capture(resultCaptor)) } answers { latch.countDown() }
                result = resultCaptor
            }
            else -> throw Error("onComplete for $authApi is not defined!")
        }

    }

    private fun setupOnError() {
        every { onError.accept(capture(errorCaptor)) } answers { latch.countDown() }
        result = errorCaptor
    }

    fun getTheResult() : Any {
        return result
    }


}
