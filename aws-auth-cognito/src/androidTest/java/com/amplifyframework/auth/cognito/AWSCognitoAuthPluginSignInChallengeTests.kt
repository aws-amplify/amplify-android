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

package com.amplifyframework.auth.cognito

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.test.R
import com.amplifyframework.auth.cognito.testutils.blockForCode
import com.amplifyframework.auth.cognito.testutils.blockUntilEstablished
import com.amplifyframework.auth.cognito.testutils.createMfaSubscription
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.amplifyframework.datastore.generated.model.MfaInfo
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.api.SubscriptionHolder
import com.amplifyframework.testutils.sync.SynchronousAuth
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.Random
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests that fetchAuthSession and signOut behave correctly when called during an in-progress
 * sign-in with MFA challenge. This reproduces the bug where navigating away from an MFA screen
 * and back causes InvalidStateException because the state machine is still in a signing-in state.
 */
class AWSCognitoAuthPluginSignInChallengeTests : DeviceFarmTestBase() {

    private val password = "${UUID.randomUUID()}BleepBloop1234!"
    private val username = "test${Random().nextInt()}"
    private val email = "$username@amplify-swift-gamma.awsapps.com"

    private var authPlugin = AWSCognitoAuthPlugin()
    private var apiPlugin = AWSApiPlugin()
    private lateinit var synchronousAuth: SynchronousAuth
    private lateinit var subscription: SubscriptionHolder<MfaInfo>

    @Before
    fun initializePlugin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AmplifyOutputsData
            .deserialize(context, AmplifyOutputs.fromResource(R.raw.amplify_outputs_email_or_totp_mfa))

        authPlugin.configure(config, context)
        apiPlugin.configure(config, context)
        synchronousAuth = SynchronousAuth.delegatingTo(authPlugin)

        subscription = apiPlugin.createMfaSubscription()
    }

    @After
    fun tearDown() {
        subscription.cancel()
        try {
            synchronousAuth.deleteUser()
        } catch (_: Exception) {
            // User may not be signed in if the test didn't complete sign-in
        }
    }

    @Test
    fun signIn_succeeds_after_fetchAuthSession_during_mfa_challenge() {
        signUpNewUser()
        subscription.blockUntilEstablished()

        // Sign in and reach MFA challenge state
        var signInResult = synchronousAuth.signIn(username, password)
        signInResult.nextStep.signInStep shouldBe AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP

        // Call fetchAuthSession (simulates what AuthenticatorViewModel does on re-creation)
        val session = synchronousAuth.fetchAuthSession()
        session.isSignedIn.shouldBeFalse()

        // Confirm sign-in can still complete with the MFA code
        val mfaCode = subscription.blockForCode(username)
        signInResult = synchronousAuth.confirmSignIn(mfaCode)
        signInResult.nextStep.signInStep shouldBe AuthSignInStep.DONE
    }

    @Test
    fun signIn_succeeds_after_signOut_during_mfa_challenge() {
        signUpNewUser()
        subscription.blockUntilEstablished()

        // Sign in and reach MFA challenge state
        var signInResult = synchronousAuth.signIn(username, password)
        signInResult.nextStep.signInStep shouldBe AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP
        subscription.blockForCode(username)

        // Call signOut to cancel the in-progress sign-in
        val signOutResult = synchronousAuth.signOut()
        signOutResult.shouldBeInstanceOf<AWSCognitoAuthSignOutResult.CompleteSignOut>()

        // Start a fresh sign-in and complete it
        signInResult = synchronousAuth.signIn(username, password)
        signInResult.nextStep.signInStep shouldBe AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP

        val mfaCode = subscription.blockForCode(username)
        signInResult = synchronousAuth.confirmSignIn(mfaCode)
        signInResult.nextStep.signInStep shouldBe AuthSignInStep.DONE
    }

    private fun signUpNewUser() {
        val options = AuthSignUpOptions.builder()
            .userAttributes(listOf(AuthUserAttribute(AuthUserAttributeKey.email(), email)))
            .build()
        synchronousAuth.signUp(username, password, options)
    }
}
