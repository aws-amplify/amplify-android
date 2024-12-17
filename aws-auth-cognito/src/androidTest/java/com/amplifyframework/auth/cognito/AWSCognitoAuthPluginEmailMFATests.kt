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
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.exceptions.service.CodeMismatchException
import com.amplifyframework.auth.cognito.test.R
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.amplifyframework.datastore.generated.model.MfaInfo
import com.amplifyframework.testutils.Assets
import com.amplifyframework.testutils.sync.SynchronousAuth
import java.util.Random
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Test

class AWSCognitoAuthPluginEmailMFATests {

    private val password = "${UUID.randomUUID()}BleepBloop1234!"
    private val userName = "test${Random().nextInt()}"
    private val email = "$userName@amplify-swift-gamma.awsapps.com"

    private var authPlugin = AWSCognitoAuthPlugin()
    private var apiPlugin = AWSApiPlugin()
    private lateinit var synchronousAuth: SynchronousAuth
    private var subscription: GraphQLOperation<MfaInfo>? = null
    private var mfaCode = ""
    private var latch: CountDownLatch? = null

    @Before
    fun initializePlugin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AmplifyOutputsData
            .deserialize(context, AmplifyOutputs.fromResource(R.raw.amplify_outputs_email_or_totp_mfa))

        authPlugin.configure(config, context)
        apiPlugin.configure(config, context)
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
                mfaCode = it.data.code
                latch?.countDown()
            },
            { println("====== Subscription Failed $it ======") },
            { }
        )
    }

    @After
    fun tearDown() {
        subscription?.cancel()
        mfaCode = ""
        synchronousAuth.deleteUser()
    }

    @Test
    fun fresh_email_mfa_setup() {
        // Step 1: Sign up a new user
        signUpNewUser()

        // Step 2: Attempt to sign in with the newly created user
        var signInResult = synchronousAuth.signIn(userName, password)

        // Validation 1: Validate that the next step is MFA Setup Selection
        assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_MFA_SETUP_SELECTION, signInResult.nextStep.signInStep)

        // Validation 2: Validate that the available MFA choices are Email and TOTP
        assertEquals(setOf(MFAType.EMAIL, MFAType.TOTP), signInResult.nextStep.allowedMFATypes)

        // Step 3: Select "Email" as the MFA to set up
        signInResult = synchronousAuth.confirmSignIn("EMAIL_OTP")

        // Validation 2: Validate that the next step is to input the user's email address
        assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_EMAIL_MFA_SETUP, signInResult.nextStep.signInStep)

        // Step 4: Input the email address to send the code to then wait for the MFA code
        latch = CountDownLatch(1)
        signInResult = synchronousAuth.confirmSignIn(email)

        // Validation 3: Validate that the next step is to confirm the emailed MFA code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the MFA code has been received
        latch?.await(20, TimeUnit.SECONDS)

        // Step 5: Input the emailed MFA code for confirmation
        signInResult = synchronousAuth.confirmSignIn(mfaCode)

        // Validation 4: Validate that MFA setup is done
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun sign_in_to_existing_email_mfa() {
        // Step 1: Sign up a new user with an existing email address
        signUpNewUser(email)

        // Step 2: Attempt to sign in with the newly created user
        latch = CountDownLatch(1)
        var signInResult = synchronousAuth.signIn(userName, password)

        // Validation 1: Validate that the next step is to confirm the emailed MFA code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the MFA code has been received
        latch?.await(20, TimeUnit.SECONDS)

        // Step 4: Input the emailed MFA code for confirmation
        signInResult = synchronousAuth.confirmSignIn(mfaCode)

        // Validation 2: Validate that MFA setup is done
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    @Test
    fun use_an_incorrect_MFA_code_then_sign_in_using_the_correct_one() {
        // Step 1: Sign up a new user with an existing email address
        signUpNewUser(email)

        // Step 2: Attempt to sign in with the newly created user
        latch = CountDownLatch(1)
        var signInResult = synchronousAuth.signIn(userName, password)

        // Validation 1: Validate that the next step is to confirm the emailed MFA code
        assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP, signInResult.nextStep.signInStep)

        // Wait until the MFA code has been received
        latch?.await(20, TimeUnit.SECONDS)

        // Step 4: Input the an incorrect MFA code
        // Validation 2: Validate that an incorrect MFA code throws a CodeMismatchException
        assertFailsWith<CodeMismatchException> {
            signInResult = synchronousAuth.confirmSignIn(mfaCode.reversed())
        }

        // Step 5: Input the correct MFA code for validation
        signInResult = synchronousAuth.confirmSignIn(mfaCode)

        // Validation 3: Validate that MFA setup is done
        assertEquals(AuthSignInStep.DONE, signInResult.nextStep.signInStep)
    }

    private fun signUpNewUser(email: String? = null): AuthSignUpResult {
        val attributes = if (email == null) {
            emptyList()
        } else {
            listOf(AuthUserAttribute(AuthUserAttributeKey.email(), email))
        }
        val options = AuthSignUpOptions.builder()
            .userAttributes(
                attributes
            ).build()
        return synchronousAuth.signUp(userName, password, options)
    }
}
