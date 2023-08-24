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
package com.amplifyframework.predictions.aws

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.predictions.models.IdentifyActionType
import com.amplifyframework.predictions.models.LabelType
import com.amplifyframework.predictions.models.LanguageType
import com.amplifyframework.predictions.models.TextFormatType
import com.amplifyframework.predictions.result.IdentifyCelebritiesResult
import com.amplifyframework.predictions.result.IdentifyDocumentTextResult
import com.amplifyframework.predictions.result.IdentifyEntitiesResult
import com.amplifyframework.predictions.result.IdentifyLabelsResult
import com.amplifyframework.predictions.result.IdentifyTextResult
import com.amplifyframework.testutils.Assets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class PredictionsCanaryTest {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = PredictionsCanaryTest::class.simpleName

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.addPlugin(AWSPredictionsPlugin())
                Amplify.configure(ApplicationProvider.getApplicationContext())
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    @Test
    fun translateText() {
        val latch = CountDownLatch(1)
        Amplify.Predictions.translateText(
            "I like to eat spaghetti",
            LanguageType.ENGLISH,
            LanguageType.SPANISH,
            {
                Log.i(TAG, it.translatedText)
                latch.countDown()
            },
            { fail("Translation failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun textToSpeech() {
        val latch = CountDownLatch(1)
        Amplify.Predictions.convertTextToSpeech(
            "I like to eat spaghetti!",
            { latch.countDown() },
            { fail("Failed to convert text to speech: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun identifyTextInImage() {
        val latch = CountDownLatch(1)
        val image = Assets.readAsBitmap("sample-table.png")
        Amplify.Predictions.identify(
            TextFormatType.PLAIN,
            image,
            { result ->
                val identifyResult = result as IdentifyTextResult
                Log.i(TAG, identifyResult.fullText)
                latch.countDown()
            },
            { fail("Identify text failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun identifyTextInDocument() {
        val latch = CountDownLatch(1)
        val image = Assets.readAsBitmap("sample-table.png")
        Amplify.Predictions.identify(
            TextFormatType.FORM,
            image,
            { result ->
                val identifyResult = result as IdentifyDocumentTextResult
                Log.i(TAG, identifyResult.fullText)
                latch.countDown()
            },
            { fail("Identify text failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun identifyEntities() {
        val latch = CountDownLatch(1)
        val image = Assets.readAsBitmap("jeff_bezos.jpg")
        Amplify.Predictions.identify(
            IdentifyActionType.DETECT_ENTITIES,
            image,
            { result ->
                val identifyResult = result as IdentifyEntitiesResult
                val metadata = identifyResult.entities.firstOrNull()
                Log.i(TAG, "${metadata?.box?.toShortString()}")
                latch.countDown()
            },
            { fail("Entity detection failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun identifyCelebrities() {
        val latch = CountDownLatch(1)
        val image = Assets.readAsBitmap("jeff_bezos.jpg")
        Amplify.Predictions.identify(
            IdentifyActionType.DETECT_CELEBRITIES,
            image,
            { result ->
                val identifyResult = result as IdentifyCelebritiesResult
                val metadata = identifyResult.celebrities.firstOrNull()
                Log.i(TAG, "${metadata?.celebrity?.name}")
                latch.countDown()
            },
            { fail("Celebrity detection failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun identifyLabels() {
        val latch = CountDownLatch(1)
        val image = Assets.readAsBitmap("jeff_bezos.jpg")
        Amplify.Predictions.identify(
            LabelType.LABELS,
            image,
            { result ->
                val identifyResult = result as IdentifyLabelsResult
                val label = identifyResult.labels.firstOrNull()
                Log.i(TAG, "${label?.name}")
                latch.countDown()
            },
            { fail("Label detection failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun identifyModerationLabels() {
        val latch = CountDownLatch(1)
        val image = Assets.readAsBitmap("jeff_bezos.jpg")
        Amplify.Predictions.identify(
            LabelType.MODERATION_LABELS,
            image,
            { result ->
                val identifyResult = result as IdentifyLabelsResult
                Log.i(TAG, "${identifyResult.isUnsafeContent}")
                latch.countDown()
            },
            { fail("Identify moderation labels failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun interpretSentiment() {
        val latch = CountDownLatch(1)
        Amplify.Predictions.interpret(
            "I like to eat spaghetti",
            {
                Log.i(TAG, "${it.sentiment?.value}")
                latch.countDown()
            },
            { fail("Interpret failed: $it") }
        )
        assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }
}
