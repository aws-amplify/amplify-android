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

package com.amplifyframework.kotlin.auth

import android.app.Activity
import android.content.Intent
import com.amplifyframework.auth.AuthCategoryBehavior
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthCodeDeliveryDetails.DeliveryMedium.SMS
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests that the various facade APIs in the KotlinAuthFacade are wired
 * to the delegate AuthCategoryBehavior correctly.
 */
@Suppress("UNCHECKED_CAST") // The more things change, the more they stay the same.
class KotlinAuthFacadeTest {
    private val delegate = mockk<AuthCategoryBehavior>()
    private val auth = KotlinAuthFacade(delegate)

    /**
     * When the signUp() delegate renders a result, it should be returned
     * from the coroutine.
     */
    @Test
    fun signUpSucceeds() = runBlocking {
        val username = "tony"
        val password = "password"
        val signUpResult = mockk<AuthSignUpResult>()
        every {
            delegate.signUp(eq(username), eq(password), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 3
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthSignUpResult>
            onResult.accept(signUpResult)
        }
        assertEquals(signUpResult, auth.signUp(username, password))
    }

    /**
     * When the underlying signUp() API emits an error, the coroutine
     * API should bubble it up.
     */
    @Test(expected = AuthException::class)
    fun signUpThrows(): Unit = runBlocking {
        val username = "tony"
        val password = "password"
        val error = AuthException("uh", "oh")
        every {
            delegate.signUp(eq(username), eq(password), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 4
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.signUp(username, password)
    }

    /**
     * When the underlying confirmSignUp() emits a result, it should
     * be returned through the coroutine.
     */
    @Test
    fun confirmSignUpSucceeds() = runBlocking {
        val username = "tony"
        val code = "CoolCode599"
        val signUpResult = mockk<AuthSignUpResult>()
        every {
            delegate.confirmSignUp(eq(username), eq(code), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 3
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthSignUpResult>
            onResult.accept(signUpResult)
        }
        assertEquals(signUpResult, auth.confirmSignUp(username, code))
    }

    /**
     * When the underlying confirmSignUp() delegate emits an error,
     * it should be thrown from the coroutine.
     */
    @Test(expected = AuthException::class)
    fun confirmSignUpThrows(): Unit = runBlocking {
        val username = "tony"
        val code = "CoolCode599"
        val error = AuthException("uh", "oh")
        every {
            delegate.confirmSignUp(eq(username), eq(code), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 4
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.confirmSignUp(username, code)
    }

    /**
     * When the resendSignUpCode() delegate emits a result, it should be
     * rendered by the coroutine API, too.
     */
    @Test
    fun resendSignUpCodeSucceeds() = runBlocking {
        val username = "tony"
        val signUpResult = mockk<AuthSignUpResult>()
        every {
            delegate.resendSignUpCode(eq(username), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthSignUpResult>
            onResult.accept(signUpResult)
        }
        assertEquals(signUpResult, auth.resendSignUpCode(username))
    }

    /**
     * When the resendSignUpCode() delegate emits an error, it should be
     * rendered by the coroutine API, too.
     */
    @Test(expected = AuthException::class)
    fun resendSignUpCodeThrows(): Unit = runBlocking {
        val username = "tony"
        val error = AuthException("uh", "oh")
        every {
            delegate.resendSignUpCode(eq(username), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.resendSignUpCode(username)
    }

    /**
     * When the signIn() delegate emits a result, it should be returned
     * from the coroutine API, too.
     */
    @Test
    fun signInSucceeds() = runBlocking {
        val username = "tony"
        val password = "password"
        val signInResult = mockk<AuthSignInResult>()
        every {
            delegate.signIn(eq(username), eq(password), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 3
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthSignInResult>
            onResult.accept(signInResult)
        }
        assertEquals(signInResult, auth.signIn(username, password))
    }

    /**
     * When the signIn() delegate emits an error, it should be thrown
     * from the coroutine API, too.
     */
    @Test(expected = AuthException::class)
    fun signInThrows(): Unit = runBlocking {
        val username = "tony"
        val password = "password"
        val error = AuthException("uh", "oh")
        every {
            delegate.signIn(eq(username), eq(password), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 4
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.signIn(username, password)
    }

    /**
     * When the confirmSignIn() delegate emits a result, it should
     * be returned via the coroutine API.
     */
    @Test
    fun confirmSignInSucceeds() = runBlocking {
        val code = "CoolCode511"
        val signInResult = mockk<AuthSignInResult>()
        every {
            delegate.confirmSignIn(eq(code), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthSignInResult>
            onResult.accept(signInResult)
        }
        assertEquals(signInResult, auth.confirmSignIn(code))
    }

    /**
     * When the confirmSignIn() delegate emits an error, it should be
     * thrown from the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun confirmSignInThrows(): Unit = runBlocking {
        val code = "IncorrectCodePerhaps"
        val error = AuthException("uh", "oh")
        every {
            delegate.confirmSignIn(eq(code), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.confirmSignIn(code)
    }

    /**
     * When the signInWithSocialWebUI() furnishes a result, it should be returned
     * via the coroutine API, too.
     */
    @Test
    fun signInWithSocialWebUISucceeds() = runBlocking {
        val provider = AuthProvider.google()
        val activity = Activity()
        val signInResult = mockk<AuthSignInResult>()
        every {
            delegate.signInWithSocialWebUI(eq(provider), eq(activity), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 3
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthSignInResult>
            onResult.accept(signInResult)
        }
        assertEquals(signInResult, auth.signInWithSocialWebUI(provider, activity))
    }

    /**
     * When the signInWithSocialWebUI() renders an error, it should be
     * bubbled out through the coroutine API, too.
     */
    @Test(expected = AuthException::class)
    fun signInWithSocialWebUIThrows(): Unit = runBlocking {
        val provider = AuthProvider.google()
        val activity = Activity()
        val error = AuthException("uh", "oh")
        every {
            delegate.signInWithSocialWebUI(eq(provider), eq(activity), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 4
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.signInWithSocialWebUI(provider, activity)
    }

    /**
     * When the signInWithWebUI() delegate renders a result, it should be returned
     * from the coroutine API as well.
     */
    @Test
    fun signInWithWebUISucceeds() = runBlocking {
        val activity = Activity()
        val signInResult = mockk<AuthSignInResult>()
        every {
            delegate.signInWithWebUI(eq(activity), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthSignInResult>
            onResult.accept(signInResult)
        }
        assertEquals(signInResult, auth.signInWithWebUI(activity))
    }

    /**
     * When the signInWithWebUI() delegate emits an error,
     * it should be thrown from the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun signInWithWebUIThrows(): Unit = runBlocking {
        val activity = Activity()
        val error = AuthException("uh", "oh")
        every {
            delegate.signInWithWebUI(eq(activity), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.signInWithWebUI(activity)
    }

    /**
     * When the handleWebUISignInResponse() delegate succeeds, so
     * should the API exposed in the Kotlin facade. Note that this
     * is *not* a coroutine API.
     */
    @Test
    fun handleWebUISignInResponseSucceeds() {
        val intent = object : Intent() {
            override fun toString(): String = "intent!"
        }
        every { delegate.handleWebUISignInResponse(eq(intent)) } returns Unit
        auth.handleWebUISignInResponse(intent)
        verify { delegate.handleWebUISignInResponse(intent) }
    }

    /**
     * When the handleWebSignInResponse() delegate throws an error,
     * so too should the API in the Kotlin facade.
     */
    @Test(expected = AuthException::class)
    fun handleWebUISignInResponseThrows() {
        val intent = Intent()
        val error = AuthException("uh", "oh")
        every { delegate.handleWebUISignInResponse(eq(intent)) } throws error
        auth.handleWebUISignInResponse(intent)
    }

    /**
     * When the fetchAuthSession() delegate emits a session, it should
     * be returned from the coroutine API, too.
     */
    @Test
    fun fetchAuthSessionSucceeds() = runBlocking {
        val session = AuthSession(true)
        every {
            delegate.fetchAuthSession(any(), any())
        } answers {
            val indexOfResultConsumer = 0
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthSession>
            onResult.accept(session)
        }
        assertEquals(session, auth.fetchAuthSession())
    }

    /**
     * When the fetchAuthSession() delegate emits an error, it should
     * be surfaced by the coroutine API, too.
     */
    @Test(expected = AuthException::class)
    fun fetchAuthSessionThrows(): Unit = runBlocking {
        val error = AuthException("uh", "oh")
        every {
            delegate.fetchAuthSession(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.fetchAuthSession()
    }

    /**
     * When rememberDevice() coroutine is called, it should pass through to the
     * delegate. If the delegate succeeds, so should the coroutine.
     */
    @Test
    fun rememberDeviceSucceeds() = runBlocking {
        every {
            delegate.rememberDevice(any(), any())
        } answers {
            val indexOfCompletionAction = 0
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        auth.rememberDevice()
        verify {
            delegate.rememberDevice(any(), any())
        }
    }

    /**
     * When the rememberDevice() delegate emits an error,
     * it should be bubbled up through the coroutine API as well.
     */
    @Test(expected = AuthException::class)
    fun rememberDeviceThrows(): Unit = runBlocking {
        val error = AuthException("uh", "oh")
        every {
            delegate.rememberDevice(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.rememberDevice()
    }

    /**
     * When the forgetDevice() delegate succeeds, so too should the coroutine API.
     */
    @Test
    fun forgetDeviceSucceeds() = runBlocking {
        every {
            delegate.forgetDevice(any(), any())
        } answers {
            val indexOfCompletionAction = 0
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        auth.forgetDevice()
        verify {
            delegate.forgetDevice(any(), any())
        }
    }

    /**
     * When the forgetDevice() delegate renders an error, so should our coroutine API.
     */
    @Test(expected = AuthException::class)
    fun forgetDeviceThrows(): Unit = runBlocking {
        val error = AuthException("uh", "oh")
        every {
            delegate.forgetDevice(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.forgetDevice()
    }

    /**
     * When the fetchDevices() API renders a list of devices, it should
     * be returned via the coroutine API.
     */
    @Test
    fun fetchDevicesSucceeds() = runBlocking {
        val devices = listOf(AuthDevice.fromId("CoolDevice44"))
        every {
            delegate.fetchDevices(any(), any())
        } answers {
            val indexOfResultConsumer = 0
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<List<AuthDevice>>
            onResult.accept(devices)
        }
        assertEquals(devices, auth.fetchDevices())
    }

    /**
     * When the fetchDevices() delegate renders an error, so should the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun fetchDevicesThrows(): Unit = runBlocking {
        val error = AuthException("uh", "oh")
        every {
            delegate.fetchDevices(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.fetchDevices()
    }

    /**
     * When the resetPassword() delegate emits a result, it should be
     * returned from the coroutine API.
     */
    @Test
    fun resetPasswordSucceeds() = runBlocking {
        val username = "TonyDaniels66"
        val passwordResetResult = mockk<AuthResetPasswordResult>()
        every {
            delegate.resetPassword(eq(username), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResultArg = it.invocation.args[indexOfResultConsumer]
            val onResult = onResultArg as Consumer<AuthResetPasswordResult>
            onResult.accept(passwordResetResult)
        }
        assertEquals(passwordResetResult, auth.resetPassword(username))
    }

    /**
     * When the resetPassword() delegate renders an error, so should the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun resetPasswordThrows(): Unit = runBlocking {
        val username = "TonyDaniels6989"
        val error = AuthException("uh", "oh")
        every {
            delegate.resetPassword(eq(username), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.resetPassword(username)
    }

    /**
     * When the confirmResetPassword() delegate succeeds, so should the coroutine API.
     */
    @Test
    fun confirmResetPasswordSucceeds(): Unit = runBlocking {
        val newPassword = "VerySecurePassword=VSPBaby"
        val confirmationCode = "LegitConfirmation"
        every {
            delegate.confirmResetPassword(
                eq(newPassword),
                eq(confirmationCode),
                any(),
                any(),
                any()
            )
        } answers {
            val indexOfCompletionAction = 3
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        auth.confirmResetPassword(newPassword, confirmationCode)
        verify {
            delegate.confirmResetPassword(
                eq(newPassword),
                eq(confirmationCode),
                any(),
                any(),
                any()
            )
        }
    }

    /**
     * When the confirmResetPassword() delegate renders an error, so should the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun confirmResetPasswordThrows(): Unit = runBlocking {
        val newPassword = "SuperSecurePass911"
        val confirmationCode = "ConfirmationCode4u"
        val error = AuthException("uh", "oh")
        every {
            delegate.confirmResetPassword(
                eq(newPassword),
                eq(confirmationCode),
                any(),
                any(),
                any()
            )
        } answers {
            val indexOfErrorConsumer = 4
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.confirmResetPassword(newPassword, confirmationCode)
    }

    @Test
    fun updatePasswordSucceeds() = runBlocking {
    }

    /**
     * When the updatePassword() delegate renders an error, so should the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun updatePasswordThrows(): Unit = runBlocking {
        val oldPassword = "oldPass"
        val newPassword = "nuPass"
        val error = AuthException("uh", "oh")
        every {
            delegate.updatePassword(eq(oldPassword), eq(newPassword), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.updatePassword(oldPassword, newPassword)
    }

    /**
     * When fetchUserAttributes() renders results, they should be returned from the
     * coroutine API.
     */
    @Test
    fun fetchUserAttributesSucceeds(): Unit = runBlocking {
        val attributes = listOf(AuthUserAttribute(AuthUserAttributeKey.gender(), "non-binary"))
        every {
            delegate.fetchUserAttributes(any(), any())
        } answers {
            val onResultArg = it.invocation.args[/* index of result consumer = */ 0]
            val onResult = onResultArg as Consumer<List<AuthUserAttribute>>
            onResult.accept(attributes)
        }
        assertEquals(attributes, auth.fetchUserAttributes())
    }

    /**
     * When the fetchUserAttributes() delegate renders an error, so should the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun fetchUserAttributesThrows(): Unit = runBlocking {
        val error = AuthException("uh", "oh")
        every {
            delegate.fetchUserAttributes(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.fetchUserAttributes()
    }

    /**
     * When the updateUserAttribute() delegate renders a result, it should be
     * returned from the coroutine API.
     */
    @Test
    fun updateUserAttributeSucceeds() = runBlocking {
        // Tony starts going by "T-Bird," as a joke, to ham it up with his buds.
        val attribute = AuthUserAttribute(AuthUserAttributeKey.nickname(), "T-bird")
        val updateAttributeResult = mockk<AuthUpdateAttributeResult>()
        every {
            delegate.updateUserAttribute(attribute, any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResultArg = it.invocation.args[indexOfResultConsumer]
            val onResult = onResultArg as Consumer<AuthUpdateAttributeResult>
            onResult.accept(updateAttributeResult)
        }
        assertEquals(updateAttributeResult, auth.updateUserAttribute(attribute))
    }

    /**
     * When the updateUserAttribute() delegate renders an error, so should the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun updateUserAttributeThrows(): Unit = runBlocking {
        val attribute = AuthUserAttribute(
            AuthUserAttributeKey.email(),
            "tony.daniels@corporate.biz"
        )
        val error = AuthException("uh", "oh")
        every {
            delegate.updateUserAttribute(eq(attribute), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.updateUserAttribute(attribute)
    }

    /**
     * When the updateUserAttributes() delegate renders a result, it should be
     * returned from the coroutine API.
     */
    @Test
    fun updateUserAttributesSucceeds() = runBlocking {
        // Sarah has come out as transgender: congratulations! We need to update name and gender.
        val nameChange = AuthUserAttribute(AuthUserAttributeKey.name(), "Sarah")
        val genderChange = AuthUserAttribute(AuthUserAttributeKey.gender(), "Female")
        val genderAffirmation = listOf(nameChange, genderChange)
        val affirmed = mapOf<AuthUserAttributeKey, AuthUpdateAttributeResult>(
            AuthUserAttributeKey.name() to mockk(),
            AuthUserAttributeKey.givenName() to mockk()
        )
        every {
            delegate.updateUserAttributes(genderAffirmation, any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResult = it.invocation.args[indexOfResultConsumer]
                as Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>>
            onResult.accept(affirmed)
        }
        assertEquals(affirmed, auth.updateUserAttributes(genderAffirmation))
    }

    /**
     * When the updateUserAttributes() delegate renders an error, so should the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun updateUserAttributesThrows(): Unit = runBlocking {
        val tonyEmail = AuthUserAttribute(
            AuthUserAttributeKey.email(),
            "tony.daniels@corporate.biz"
        )
        val attributes = listOf(tonyEmail)
        val error = AuthException("uh", "oh")
        every {
            delegate.updateUserAttributes(eq(attributes), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.updateUserAttributes(attributes)
    }

    /**
     * When the resendUserAttributeConfirmationCode() delgate renders a result,
     * it should be returned from the coroutine API, too.
     */
    @Test
    fun resendUserAttributeConfirmationCodeSucceeds() = runBlocking {
        val attributeKey = AuthUserAttributeKey.email()
        val authCodeDeliveryDetails = AuthCodeDeliveryDetails("+1-206-555-2020", SMS)
        every {
            delegate.resendUserAttributeConfirmationCode(eq(attributeKey), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val onResultArg = it.invocation.args[indexOfResultConsumer]
            val onResult = onResultArg as Consumer<AuthCodeDeliveryDetails>
            onResult.accept(authCodeDeliveryDetails)
        }
        assertEquals(
            authCodeDeliveryDetails,
            auth.resendUserAttributeConfirmationCode(attributeKey)
        )
    }

    /**
     * When the resendUserAttributeConfirmationCode() delegate emits an error,
     * it should be thrown from the coroutine API, too.
     */
    @Test(expected = AuthException::class)
    fun resendUserAttributeConfirmationCodeThrows(): Unit = runBlocking {
        val attributeKey = AuthUserAttributeKey.email()
        val error = AuthException("uh", "oh")
        every {
            delegate.resendUserAttributeConfirmationCode(eq(attributeKey), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.resendUserAttributeConfirmationCode(attributeKey)
    }

    /**
     * When the confirmUserAttribute() delegate furnishes a result, it should be
     * returned via the coroutine API, too.
     */
    @Test
    fun confirmUserAttributeSucceeds() = runBlocking {
        val attributeKey = AuthUserAttributeKey.email()
        val confirmationCode = "bananas+gravy!"
        every {
            delegate.confirmUserAttribute(eq(attributeKey), eq(confirmationCode), any(), any())
        } answers {
            val indexOfCompletionAction = 2
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        auth.confirmUserAttribute(attributeKey, confirmationCode)
        verify {
            delegate.confirmUserAttribute(attributeKey, confirmationCode, any(), any())
        }
    }

    /**
     * When the confirmUserAttribute() delegate emits an error,
     * it should be bubbled out through the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun confirmUserAttributeThrows() = runBlocking {
        val attributeKey = AuthUserAttributeKey.email()
        val confirmationCode = "bananas+gravy!"
        val error = AuthException("uh", "oh")
        every {
            delegate.confirmUserAttribute(eq(attributeKey), eq(confirmationCode), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.confirmUserAttribute(attributeKey, confirmationCode)
    }

    /**
     * When the getCurrentUser() delegate return an AuthUser, the proxy
     * API in the Kotlin facade should, too. Note that this API doesn't
     * have any suspending functions, this is a straight pass through verification.
     */
    @Test
    fun getCurrentUserSucceeds(): Unit = runBlocking {
        val authUser = AuthUser("testUserID", "testUsername")
        every {
            delegate.getCurrentUser(any(), any())
        } answers {
            val indexOfResultConsumer = 0
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthUser>
            onResult.accept(authUser)
        }
        auth.getCurrentUser()
    }

    /**
     * When the getCurrentUser() has null values an Auth Exception should be sent in the onError
     * which should be captured in the Kotlin facade too
     */
    @Test(expected = AuthException.SignedOutException::class)
    fun getCurrentUserThrowsWhenSignedOut(): Unit = runBlocking {
        val expectedException = AuthException.SignedOutException()
        every {
            delegate.getCurrentUser(any(), any())
        } answers {
            val indexOfResultConsumer = 1
            val onResult = it.invocation.args[indexOfResultConsumer] as Consumer<AuthException>
            onResult.accept(expectedException)
        }
        auth.getCurrentUser()
    }

    /**
     * When the getCurrentUser() delegate throws an error, the proxy
     * API in the Kotlin facade should, too. Note that this API doesn't
     * have any suspending functions, this is a straight pass through verification.
     */
    @Test(expected = AuthException::class)
    fun getCurrentUserThrows(): Unit = runBlocking {
        val expectedException = AuthException("uh", "oh")
        every {
            delegate.getCurrentUser(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onResult = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onResult.accept(expectedException)
        }
        auth.getCurrentUser()
    }

    /**
     * When the getCurrentUser() delegate throws an error, the proxy
     * API in the Kotlin facade should, too. Note that this API doesn't
     * have any suspending functions, this is a straight pass through verification.
     */
    @Test(expected = AuthException::class)
    fun getCurrentUserWhenUserNameIsEmpty(): Unit = runBlocking {
        val expectedException = AuthException.InvalidUserPoolConfigurationException()
        every {
            delegate.getCurrentUser(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onResult = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onResult.accept(expectedException)
        }
        auth.getCurrentUser()
    }

    /**
     * Signing out calls through to the delegate. If no error is thrown from
     * the delegate, none is rendered by the coroutine.
     */
    @Test
    fun signOutSucceeds() = runBlocking {
        every {
            delegate.signOut(any(), any(), any())
        } answers {
            val indexOfCompletionAction = 1
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        auth.signOut()
        // Since nothing returned, just verify it called through.
        verify {
            delegate.signOut(any(), any(), any())
        }
    }

    /**
     * The signOut() call falls through to the delegate. If the delegate
     * renders an error, it should be bubbled out through the coroutine API.
     */
    @Test(expected = AuthException::class)
    fun signOutThrows(): Unit = runBlocking {
        val expectedException = AuthException("uh", "oh")
        every {
            delegate.signOut(any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(expectedException)
        }
        auth.signOut()
    }

    /**
     * When deleteUser() coroutine is called, it should pass through to the
     * delegate. If the delegate succeeds, so should the coroutine.
     */
    @Test
    fun deleteUserSucceeds() = runBlocking {
        every {
            delegate.deleteUser(any(), any())
        } answers {
            val indexOfCompletionAction = 0
            val onComplete = it.invocation.args[indexOfCompletionAction] as Action
            onComplete.call()
        }
        auth.deleteUser()
        verify {
            delegate.deleteUser(any(), any())
        }
    }

    /**
     * When the deleteUser() delegate emits an error, it should
     * be bubbled up through the coroutine API as well.
     */
    @Test(expected = AuthException::class)
    fun deleteUserThrows(): Unit = runBlocking {
        val error = AuthException("uh", "oh")
        every {
            delegate.deleteUser(any(), any())
        } answers {
            val indexOfErrorConsumer = 1
            val onError = it.invocation.args[indexOfErrorConsumer] as Consumer<AuthException>
            onError.accept(error)
        }
        auth.deleteUser()
    }
}
