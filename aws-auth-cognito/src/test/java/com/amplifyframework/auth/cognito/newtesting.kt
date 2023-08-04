package com.amplifyframework.auth.cognito


import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito.CognitoIdentity
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes.Cognito.CognitoIdentityProvider
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase

import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SmithyMod

import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.states.AuthState
import featureTest.utilities.CognitoRequestFactory
import featureTest.utilities.apiExecutor



import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import io.mockk.InternalPlatformDsl.toStr


class NewTesting {

    @Test
    fun running() {
        val testingList = SmithyMod().getTest()
        for (value in testingList) {
            val testingCurr = AWSCognitoAuthPluginFeatureTest(value)
            testingCurr.api_feature_test()
        }


        assertEquals(1, 1)
    }

    @Test
    fun getResetPassword() {
        val smithy = SmithyMod().getApi("resetPassword")

        for (value in smithy) {
            val testing = AWSCognitoAuthPluginFeatureTest(value)
            testing.api_feature_test()

        }



    }
    @Test
    fun getSignIn() {
        val smithy = SmithyMod().getApi("signIn")

        for (value in smithy) {
            val testing = AWSCognitoAuthPluginFeatureTest(value)
            testing.api_feature_test()

        }


    }

}


