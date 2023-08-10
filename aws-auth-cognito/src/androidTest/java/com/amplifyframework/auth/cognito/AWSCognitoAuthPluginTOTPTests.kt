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
package com.amplifyframework.auth.cognito

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.test.R
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.category.CategoryConfiguration
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.AndroidLoggingPlugin
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.testutils.Sleep
import com.amplifyframework.testutils.sync.SynchronousAuth
import dev.robinohs.totpkt.otp.totp.TotpGenerator
import dev.robinohs.totpkt.otp.totp.timesupport.generateCode
import java.util.Random
import java.util.UUID
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AWSCognitoAuthPluginTOTPTests {

    private lateinit var authPlugin: AWSCognitoAuthPlugin
    private lateinit var synchronousAuth: SynchronousAuth
    private val password = UUID.randomUUID().toString()
    private val userName = "testUser${Random().nextInt()}"
    private val email = "$userName@testdomain.com"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Amplify.addPlugin(AndroidLoggingPlugin(LogLevel.VERBOSE))
        val config = AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfiguration_totp)
        val authConfig: CategoryConfiguration = config.forCategoryType(CategoryType.AUTH)
        val authConfigJson = authConfig.getPluginConfig("awsCognitoAuthPlugin")
        authPlugin = AWSCognitoAuthPlugin()
        authPlugin.configure(authConfigJson, context)
        synchronousAuth = SynchronousAuth.delegatingTo(authPlugin)
        signUpNewUser(userName, password, email)
        synchronousAuth.signOut()
    }

    @After
    fun tearDown() {
        synchronousAuth.deleteUser()
    }

    @Test
    fun mfa_setup() {
        val result = synchronousAuth.signIn(userName, password)
        Assert.assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP, result.nextStep.signInStep)
        val otp = TotpGenerator().generateCode(
            result.nextStep.totpSetupDetails!!.sharedSecret.toByteArray(),
            System.currentTimeMillis(),
        )
        synchronousAuth.confirmSignIn(otp)
        val currentUser = synchronousAuth.currentUser
        Assert.assertEquals(userName.lowercase(), currentUser.username)
    }

    @Test
    fun mfasetup_with_incorrect_otp() {
        val result = synchronousAuth.signIn(userName, password)
        Assert.assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP, result.nextStep.signInStep)
        try {
            synchronousAuth.confirmSignIn("123456")
        } catch (e: Exception) {
            Assert.assertEquals("Code mismatch", e.cause?.message)
            val otp = TotpGenerator().generateCode(
                result.nextStep.totpSetupDetails!!.sharedSecret.toByteArray(),
                System.currentTimeMillis(),
            )
            synchronousAuth.confirmSignIn(otp)
            val currentUser = synchronousAuth.currentUser
            Assert.assertEquals(userName.lowercase(), currentUser.username)
        }
    }

    @Test
    fun signIn_with_totp_after_mfa_setup() {
        val result = synchronousAuth.signIn(userName, password)
        Assert.assertEquals(AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP, result.nextStep.signInStep)
        val otp = TotpGenerator().generateCode(
            result.nextStep.totpSetupDetails!!.sharedSecret.toByteArray(),
        )
        Log.d("signIn_with_totp_after_mfa_setup", "otp is $otp")
        synchronousAuth.confirmSignIn(otp)
        synchronousAuth.signOut()
        Sleep.milliseconds(30 * 1000)
        val signInResult = synchronousAuth.signIn(userName, password)
        Assert.assertEquals(AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE, signInResult.nextStep.signInStep)
        val otpCode = TotpGenerator().generateCode(
            result.nextStep.totpSetupDetails!!.sharedSecret.toByteArray(),
        )
        Log.d("signIn_with_totp_after_mfa_setup", "otp is $otp")
        synchronousAuth.confirmSignIn(otpCode)
        val currentUser = synchronousAuth.currentUser
        Assert.assertEquals(userName.lowercase(), currentUser.username)
    }

    private fun signUpNewUser(userName: String, password: String, email: String) {
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), email)
            .build()
        synchronousAuth.signUp(userName, password, options)
    }
}
