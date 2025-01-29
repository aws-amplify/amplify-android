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

package com.amplifyframework.auth.cognito

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.api.graphql.GraphQLOperation
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.exceptions.service.CodeMismatchException
import com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException
import com.amplifyframework.auth.cognito.exceptions.service.UserNotFoundException
import com.amplifyframework.auth.cognito.exceptions.service.UsernameExistsException
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.test.R
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.category.CategoryConfiguration
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.datastore.generated.model.MfaInfo
import com.amplifyframework.testutils.Assets
import com.amplifyframework.testutils.sync.SynchronousAuth
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Test

class AWSCognitoAuthPluginUserAuthTests {

    private val password = "${UUID.randomUUID()}BleepBloop1234!"
    private val userName = "test${Random.nextInt()}"
    private val email = "$userName@amplify-swift-gamma.awsapps.com"
    private val phoneNumber = "+1555${Random.nextInt(1000000, 10000000)}"

    private var authPlugin = AWSCognitoAuthPlugin()
    private var apiPlugin = AWSApiPlugin()
    private lateinit var synchronousAuth: SynchronousAuth
    private var subscription: GraphQLOperation<MfaInfo>? = null
    private var otpCode = ""
    private var latch: CountDownLatch? = null

    @Before
    fun initializePlugin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfiguration_passwordless)
        val authConfig: CategoryConfiguration = config.forCategoryType(CategoryType.AUTH)
        val authConfigJson = authConfig.getPluginConfig("awsCognitoAuthPlugin")

        val apiConfig: CategoryConfiguration = config.forCategoryType(CategoryType.API)
        val apiConfigJson = apiConfig.getPluginConfig("awsAPIPlugin")

        authPlugin.configure(authConfigJson, context)
        apiPlugin.configure(apiConfigJson, context)
        synchronousAuth = SynchronousAuth.delegatingTo(authPlugin)

        subscription = apiPlugin.subscribe(
            SimpleGraphQLRequest(
                Assets.readAsString("create-mfa-subscription.graphql"),
                MfaInfo::class.java,
                null
            ),
            { println("====== Subscription Established ======") },
            {
                println("====== Received some MFA Info ======")
                if (it.data.username == userName) {
                    otpCode = it.data.code
                    latch?.countDown()
                }
            },
            { println("====== Subscription Failed $it ======") },
            { }
        )
    }

    @After
    fun tearDown() {
        subscription?.cancel()
        otpCode = ""
        try {
            synchronousAuth.deleteUser()
        } catch (e: SignedOutException) {
            // Catching this because if an assert fails and the test can't delete a user (because the test failed
            // before the sign in fully finishes) then deleteUser will always throw an exception because there isn't a
            // signed in user and that exception will hide the previous, more relevant, exception
            println("Encountered an exception trying to delete the user: $e")
        }
    }

    @Test
    fun signInWithAnIncompatiblePreferredFirstFactorShowsSelectChallenge() {
        // Step 1: Sign up a new user with NO phone number and confirm it
        signUpAndConfirmNewUser(
            usePhoneNumber = false
        )

        // Step 2: Attempt to sign in with the newly created user with SMS as the preferred first factor
        // (Inherently incompatible since the account has no phone number associated with it)
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(AuthFactorType.SMS_OTP)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to select a first factor (since SMS isn't a proper option)
        assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION, signInResult.nextStep.signInStep)

        // Step 3: Select a proper first factor option
        signInResult = synchronousAuth.confirmSignIn(AuthFactorType.EMAIL_OTP.name)

        // Validation 2: Validate that the next step is to confirm the emailed OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Step 4: Input the emailed OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 4: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    // Email related tests

    @Test
    fun signInWithNoFirstFactorPreferenceAndSelectEmailSucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePhoneNumber = true,
            usePassword = true
        )

        // Step 2: Attempt to sign in with the newly created user with NO preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(null)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to select a first factor
        assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION, signInResult.nextStep.signInStep)

        // Step 3: Select a first factor option
        signInResult = synchronousAuth.confirmSignIn(AuthFactorType.EMAIL_OTP.name)

        // Validation 2: Validate that the next step is to confirm the emailed OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Step 4: Input the emailed OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 4: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun signInWithNoFirstFactorPreferenceAndSelectEmailRetrySucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePhoneNumber = true
        )

        // Step 2: Attempt to sign in with the newly created user with NO preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(null)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to select a first factor
        assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION, signInResult.nextStep.signInStep)

        // Step 3: Select a first factor option
        signInResult = synchronousAuth.confirmSignIn(AuthFactorType.EMAIL_OTP.name)

        // Validation 2: Validate that the next step is to confirm the emailed OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Validation 3: Validate that providing an incorrect OTP code throws the proper exception
        assertFailsWith<CodeMismatchException> {
            synchronousAuth.confirmSignIn(otpCode.reversed())
        }

        // Step 4: Input the emailed OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 4: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun signInWithEmailPreferredSucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser()

        // Step 2: Attempt to sign in with the newly created user with EMAIL preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(AuthFactorType.EMAIL_OTP)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to confirm the emailed OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Step 3: Input the emailed OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 2: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun signInWithEmailPreferredRetrySucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser()

        // Step 2: Attempt to sign in with the newly created user with EMAIL preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(AuthFactorType.EMAIL_OTP)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to confirm the emailed OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Validation 2: Validate that providing an incorrect OTP code throws the proper exception
        assertFailsWith<CodeMismatchException> {
            synchronousAuth.confirmSignIn(otpCode.reversed())
        }

        // Step 3: Input the emailed OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 3: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    // SMS Related Tests

    @Test
    fun signInWithNoFirstFactorPreferenceAndSelectSmsSucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePhoneNumber = true
        )

        // Step 2: Attempt to sign in with the newly created user with NO preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(null)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to select a first factor
        assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION, signInResult.nextStep.signInStep)

        // Step 3: Select a first factor option
        signInResult = synchronousAuth.confirmSignIn(AuthFactorType.SMS_OTP.name)

        // Validation 2: Validate that the next step is to confirm the texted OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Step 4: Input the texted OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 3: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun signInWithNoFirstFactorPreferenceAndSelectSmsRetrySucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePhoneNumber = true
        )

        // Step 2: Attempt to sign in with the newly created user with NO preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(null)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to select a first factor
        assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION, signInResult.nextStep.signInStep)

        // Step 3: Select a first factor option
        signInResult = synchronousAuth.confirmSignIn(AuthFactorType.SMS_OTP.name)

        // Validation 2: Validate that the next step is to confirm the texted OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Validation 3: Validate that providing an incorrect OTP code throws the proper exception
        assertFailsWith<CodeMismatchException> {
            synchronousAuth.confirmSignIn(otpCode.reversed())
        }

        // Step 4: Input the texted OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 4: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun signInWithSmsPreferredSucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePhoneNumber = true
        )

        // Step 2: Attempt to sign in with the newly created user with SMS preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(AuthFactorType.SMS_OTP)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to confirm the texted OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Step 4: Input the texted OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 2: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun signInWithSmsPreferredRetrySucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePhoneNumber = true
        )

        // Step 2: Attempt to sign in with the newly created user with SMS preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(AuthFactorType.SMS_OTP)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to confirm the texted OTP code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the OTP code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Validation 2: Validate that providing an incorrect OTP code throws the proper exception
        assertFailsWith<CodeMismatchException> {
            synchronousAuth.confirmSignIn(otpCode.reversed())
        }

        // Step 3: Input the texted OTP code for confirmation
        signInResult = synchronousAuth.confirmSignIn(otpCode)

        // Validation 2: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    // Password Related Tests

    @Test
    fun signInWithNoFirstFactorPreferenceAndSelectPasswordSucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePassword = true
        )

        // Step 2: Attempt to sign in with the newly created user with NO preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(null)
                .build()

        var signInResult = synchronousAuth.signIn(userName, null, options)

        // Validation 1: Validate that the next step is to select a first factor
        assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION, signInResult.nextStep.signInStep)

        // Step 3: Select a first factor option
        signInResult = synchronousAuth.confirmSignIn(AuthFactorType.PASSWORD.name)

        // Validation 2: Validate that the user needs to input their password
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_PASSWORD, signInResult.nextStep.signInStep)

        // Step 4: Input the password
        signInResult = synchronousAuth.confirmSignIn(password)

        // Validation 3: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun signInWithPasswordPreferredRetrySucceeds() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePassword = true
        )

        // Step 2: Attempt to sign in with the newly created user with PASSWORD preferred first factor
        val options =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(AuthFactorType.PASSWORD)
                .build()

        // Validation 1: Validate that an incorrect password returns the proper exception
        assertFailsWith<NotAuthorizedException> {
            synchronousAuth.signIn(userName, password.reversed(), options)
        }

        // Step 3: Sign in with the correct password
        val signInResult = synchronousAuth.signIn(userName, password, options)

        // Validation 2: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    // Sign Up

    @Test
    fun signUpWithEmptyUsernameFails() {
        // Step 1: Try to sign up a new user with an invalid/empty username
        val options = AuthSignUpOptions.builder()
            .userAttributes(listOf(AuthUserAttribute(AuthUserAttributeKey.email(), email)))
            .build()

        // Validation 1: Validate that sign up fails because a username is required
        assertFailsWith<InvalidParameterException> {
            synchronousAuth.signUp("", null, options)
        }
    }

    @Test
    fun signUpWithSameUsernameFails() {
        // Step 1: Sign up a new user and confirm it
        signUpAndConfirmNewUser(
            usePassword = true
        )

        // Step 2: Try to sign up a user with the same username
        val signUpOptions = AuthSignUpOptions.builder()
            .userAttributes(listOf(AuthUserAttribute(AuthUserAttributeKey.email(), email)))
            .build()

        // Validation 1: Validate that sign up fails because the username was already taken
        assertFailsWith<UsernameExistsException> {
            synchronousAuth.signUp(userName, password, signUpOptions)
        }

        // Step 3: Sign in so that we can delete the user in the tear down
        val signInOptions =
            AWSCognitoAuthSignInOptions
                .builder()
                .authFlowType(AuthFlowType.USER_AUTH)
                .preferredFirstFactor(AuthFactorType.PASSWORD)
                .build()

        // Step 3: Sign in with the correct password
        val signInResult = synchronousAuth.signIn(userName, password, signInOptions)

        // Validation 2: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    // Confirm Sign Up and Auto Sign In

    @Test
    fun confirmSignUpAndAutoSignInSucceeds() {
        // Step 1: Sign up a passwordless user with just an email address
        val options = AuthSignUpOptions.builder()
            .userAttributes(listOf(AuthUserAttribute(AuthUserAttributeKey.email(), email)))
            .build()
        var signUpResult = synchronousAuth.signUp(userName, null, options)

        // Validation 1: Validate that the user is currently in the Confirm Sign Up state
        assertEquals(AuthSignUpStep.CONFIRM_SIGN_UP_STEP, signUpResult.nextStep.signUpStep)

        // Validation 2: Validate that calling auto sign in before the user is confirmed fails
        assertFailsWith<InvalidStateException> {
            synchronousAuth.autoSignIn()
        }

        // Wait until the confirmation code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Step 2: Confirm sign up with the correct OTP code
        signUpResult = synchronousAuth.confirmSignUp(userName, otpCode)

        // Validation 3: Validate that the user confirmation is complete and that auto sign in can be completed
        assertEquals(AuthSignUpStep.COMPLETE_AUTO_SIGN_IN, signUpResult.nextStep.signUpStep)

        // Step 3: Sign in so that we can delete the user in the tear down
        val signInResult = synchronousAuth.autoSignIn()

        // Validation 4: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun confirmSignUpRetryAndAutoSignInSucceeds() {
        // Step 1: Sign up a passwordless user with just an email address
        val options = AuthSignUpOptions.builder()
            .userAttributes(listOf(AuthUserAttribute(AuthUserAttributeKey.email(), email)))
            .build()
        var signUpResult = synchronousAuth.signUp(userName, null, options)

        // Validation 1: Validate that the user is currently in the Confirm Sign Up state
        assertEquals(AuthSignUpStep.CONFIRM_SIGN_UP_STEP, signUpResult.nextStep.signUpStep)

        // Validation 2: Validate that calling auto sign in before the user is confirmed fails
        assertFailsWith<InvalidStateException> {
            synchronousAuth.autoSignIn()
        }

        // Wait until the confirmation code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        // Validation 3: Validate that confirm sign up fails the OTP code is incorrect
        assertFailsWith<CodeMismatchException> {
            synchronousAuth.confirmSignUp(userName, otpCode.reversed())
        }

        // Step 2: Confirm sign up with the correct OTP code
        signUpResult = synchronousAuth.confirmSignUp(userName, otpCode)

        // Validation 4: Validate that the user confirmation is complete and that auto sign in can be completed
        assertEquals(AuthSignUpStep.COMPLETE_AUTO_SIGN_IN, signUpResult.nextStep.signUpStep)

        // Step 3: Sign in so that we can delete the user in the tear down
        val signInResult = synchronousAuth.autoSignIn()

        // Validation 5: Validate that user is signed in
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun confirmSignUpFailsForUnregisteredUser() {
        // Validation 1: Validate that confirm sign up fails on an unregistered user
        assertFailsWith<UserNotFoundException> {
            synchronousAuth.confirmSignUp(userName, "123456")
        }
    }

    @Test
    fun confirmSignUpFailsForEmptyUser() {
        // Validation 1: Validate that confirm sign up fails on an invalid username
        assertFailsWith<InvalidParameterException> {
            synchronousAuth.confirmSignUp("", "123456")
        }
    }

    private fun signUpAndConfirmNewUser(usePhoneNumber: Boolean = false, usePassword: Boolean = false) {
        val signUpPassword = if (usePassword) {
            password
        } else {
            null
        }
        val attributes = if (usePhoneNumber) {
            listOf(
                AuthUserAttribute(AuthUserAttributeKey.email(), email),
                AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), phoneNumber)
            )
        } else {
            listOf(AuthUserAttribute(AuthUserAttributeKey.email(), email))
        }

        val options = AuthSignUpOptions.builder()
            .userAttributes(attributes).build()
        synchronousAuth.signUp(userName, signUpPassword, options)

        // Wait until the confirmation code has been received
        latch = CountDownLatch(1)
        latch?.await(20, TimeUnit.SECONDS)

        synchronousAuth.confirmSignUp(userName, otpCode)
    }
}
