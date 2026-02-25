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
import com.amplifyframework.testutils.rules.CanaryTestRule
import com.amplifyframework.testutils.sync.SynchronousPredictions
import org.junit.BeforeClass
import org.junit.Rule
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

    @get:Rule
    val testRule = CanaryTestRule()

    val syncPredictions = SynchronousPredictions.delegatingTo(Amplify.Predictions)

    @Test
    fun translateText() {
        val result = syncPredictions.translateText(
            "I like to eat spaghetti",
            LanguageType.ENGLISH,
            LanguageType.SPANISH
        )
        Log.i(TAG, result.translatedText)
    }

    @Test
    fun textToSpeech() {
        syncPredictions.convertTextToSpeech("I like to eat spaghetti!")
    }

    @Test
    fun identifyTextInImage() {
        val image = Assets.readAsBitmap("sample-table.png")
        val result = syncPredictions.identify(TextFormatType.PLAIN, image)
        val identifyResult = result as IdentifyTextResult
        Log.i(TAG, identifyResult.fullText)
    }

    @Test
    fun identifyTextInDocument() {
        val image = Assets.readAsBitmap("sample-table.png")
        val result = syncPredictions.identify(TextFormatType.FORM, image)
        val identifyResult = result as IdentifyDocumentTextResult
        Log.i(TAG, identifyResult.fullText)
    }

    @Test
    fun identifyEntities() {
        val image = Assets.readAsBitmap("jeff_bezos.jpg")
        val result = syncPredictions.identify(IdentifyActionType.DETECT_ENTITIES, image)
        val identifyResult = result as IdentifyEntitiesResult
        val metadata = identifyResult.entities.firstOrNull()
        Log.i(TAG, "${metadata?.box?.toShortString()}")
    }

    @Test
    fun identifyCelebrities() {
        val image = Assets.readAsBitmap("jeff_bezos.jpg")
        val result = syncPredictions.identify(IdentifyActionType.DETECT_CELEBRITIES, image)
        val identifyResult = result as IdentifyCelebritiesResult
        val metadata = identifyResult.celebrities.firstOrNull()
        Log.i(TAG, "${metadata?.celebrity?.name}")
    }

    @Test
    fun identifyLabels() {
        val image = Assets.readAsBitmap("jeff_bezos.jpg")
        val result = syncPredictions.identify(LabelType.LABELS, image)
        val identifyResult = result as IdentifyLabelsResult
        val label = identifyResult.labels.firstOrNull()
        Log.i(TAG, "${label?.name}")
    }

    @Test
    fun identifyModerationLabels() {
        val image = Assets.readAsBitmap("jeff_bezos.jpg")
        val result = syncPredictions.identify(LabelType.MODERATION_LABELS, image)
        val identifyResult = result as IdentifyLabelsResult
        Log.i(TAG, "${identifyResult.isUnsafeContent}")
    }

    @Test
    fun interpretSentiment() {
        val result = syncPredictions.interpret("I like to eat spaghetti")
        Log.i(TAG, "${result.sentiment?.value}")
    }
}
