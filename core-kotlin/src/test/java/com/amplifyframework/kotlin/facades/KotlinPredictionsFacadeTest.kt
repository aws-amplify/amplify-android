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

package com.amplifyframework.kotlin.facades

import android.graphics.Bitmap
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.PredictionsCategoryBehavior
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.models.IdentifyActionType
import com.amplifyframework.predictions.models.LanguageType.ENGLISH
import com.amplifyframework.predictions.models.LanguageType.SPANISH
import com.amplifyframework.predictions.result.IdentifyResult
import com.amplifyframework.predictions.result.InterpretResult
import com.amplifyframework.predictions.result.TextToSpeechResult
import com.amplifyframework.predictions.result.TranslateTextResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the KotlinPredictionsFacade.
 */
@Suppress("UNCHECKED_CAST")
class KotlinPredictionsFacadeTest {
    private val delegate = mockk<PredictionsCategoryBehavior>()
    private val predictions = KotlinPredictionsFacade(delegate)

    /**
     * When the underlying convertTextToSpeech() emits a result,
     * it should be returned from the Kotlin API
     */
    @Test
    fun convertTextToSpeechSucceeds() = runBlocking {
        val text = "Good day"
        val expectedResult = mockk<TextToSpeechResult>()
        every {
            delegate.convertTextToSpeech(eq(text), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val arg = it.invocation.args[indexOfResultConsumer]
            val onResult = arg as Consumer<TextToSpeechResult>
            onResult.accept(expectedResult)
            mockk()
        }
        assertEquals(expectedResult, predictions.convertTextToSpeech(text))
    }

    /**
     * When the underlying convertText() throws an error, it
     * should be thrown from the coroutine API.
     */
    @Test(expected = PredictionsException::class)
    fun convertTextToSpeechThrows(): Unit = runBlocking {
        val text = "Good day"
        val expectedError = PredictionsException("uh", "oh")
        every {
            delegate.convertTextToSpeech(eq(text), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val arg = it.invocation.args[indexOfErrorConsumer]
            val onError = arg as Consumer<PredictionsException>
            onError.accept(expectedError)
            mockk()
        }
        predictions.convertTextToSpeech(text)
    }

    /**
     * When the underlying translateText() renders a result,
     * it should be returned from the coroutine API.
     */
    @Test
    fun translateTextViaOptionsSucceeds() = runBlocking {
        val text = "Good day"
        val expectedResult = TranslateTextResult.builder()
            .targetLanguage(SPANISH)
            .translatedText("Buenos dias")
            .build()
        every {
            delegate.translateText(eq(text), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 2
            val arg = it.invocation.args[indexOfResultConsumer]
            val onResult = arg as Consumer<TranslateTextResult>
            onResult.accept(expectedResult)
            mockk()
        }
        assertEquals(expectedResult, predictions.translateText(text))
    }

    /**
     * When the underlying translateText() emits an error,
     * it should be thrown from the coroutine API.
     */
    @Test(expected = PredictionsException::class)
    fun translateTextViaOptionsThrows(): Unit = runBlocking {
        val text = "Good day"
        val expectedError = PredictionsException("uh", "oh")
        every {
            delegate.translateText(eq(text), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val arg = it.invocation.args[indexOfErrorConsumer]
            val onError = arg as Consumer<PredictionsException>
            onError.accept(expectedError)
            mockk()
        }
        predictions.translateText(text)
    }

    /**
     * When the underlying translateText() renders a result,
     * it should be returned from the coroutine API.
     */
    @Test
    fun translateTextViaLanguageSpecsSucceeds() = runBlocking {
        val text = "Good day"
        val original = ENGLISH
        val converted = SPANISH
        val expectedResult = TranslateTextResult.builder()
            .targetLanguage(converted)
            .translatedText("Buenos dias")
            .build()
        every {
            delegate.translateText(eq(text), eq(original), eq(converted), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 4
            val arg = it.invocation.args[indexOfResultConsumer]
            val onResult = arg as Consumer<TranslateTextResult>
            onResult.accept(expectedResult)
            mockk()
        }
        assertEquals(expectedResult, predictions.translateText(text, original, converted))
    }

    /**
     * When the underlying translateText() emits an error, it should be
     * thrown via the coroutine API.
     */
    @Test(expected = PredictionsException::class)
    fun translateTextViaLanguageSpecsThrows(): Unit = runBlocking {
        val text = "Good day"
        val original = ENGLISH
        val converted = SPANISH
        val error = PredictionsException("uh", "oh")
        every {
            delegate.translateText(eq(text), eq(original), eq(converted), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 5
            val arg = it.invocation.args[indexOfErrorConsumer]
            val onError = arg as Consumer<PredictionsException>
            onError.accept(error)
            mockk()
        }
        predictions.translateText(text, original, converted)
    }

    /**
     * When the underlying identify() API emits a result, it should be
     * returned from the coroutine API.
     */
    @Test
    fun identifySucceeds(): Unit = runBlocking {
        val actionType = IdentifyActionType.DETECT_TEXT
        val bitmap = mockk<Bitmap>()
        val result = mockk<IdentifyResult>()
        every {
            delegate.identify(eq(actionType), eq(bitmap), any(), any(), any())
        } answers {
            val indexOfResultConsumer = 3
            val arg = it.invocation.args[indexOfResultConsumer]
            val onResult = arg as Consumer<IdentifyResult>
            onResult.accept(result)
            mockk()
        }
        assertEquals(result, predictions.identify(actionType, bitmap))
    }

    /**
     * When the underlying identify() delegate emits an error,
     * it should be thrown from the Kotlin API.
     */
    @Test(expected = PredictionsException::class)
    fun identifyThrows(): Unit = runBlocking {
        val error = PredictionsException("uh", "oh")
        val actionType = IdentifyActionType.DETECT_TEXT
        val bitmap = mockk<Bitmap>()
        every {
            delegate.identify(eq(actionType), eq(bitmap), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 4
            val arg = it.invocation.args[indexOfErrorConsumer]
            val onError = arg as Consumer<PredictionsException>
            onError.accept(error)
            mockk()
        }
        predictions.identify(actionType, bitmap)
    }

    /**
     * When the underlying interpret() API emits a result, it should be
     * returned from the coroutine API.
     */
    @Test
    fun interpretSucceeds() = runBlocking {
        val text = "This is a moody sentence; interpret as you will!"
        val result = mockk<InterpretResult>()
        every {
            delegate.interpret(eq(text), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 2
            val arg = it.invocation.args[indexOfErrorConsumer]
            val onResult = arg as Consumer<InterpretResult>
            onResult.accept(result)
            mockk()
        }
        assertEquals(result, predictions.interpret(text))
    }

    /**
     * When the underlying interpret() API emits an error,
     * it should be thrown from the coroutine API
     */
    @Test(expected = PredictionsException::class)
    fun interpretThrows(): Unit = runBlocking {
        val error = PredictionsException("uh", "oh")
        val text = "This is a moody sentence; interpret as you will!"
        every {
            delegate.interpret(eq(text), any(), any(), any())
        } answers {
            val indexOfErrorConsumer = 3
            val arg = it.invocation.args[indexOfErrorConsumer]
            val onError = arg as Consumer<PredictionsException>
            onError.accept(error)
            mockk()
        }
        predictions.interpret(text)
    }
}
