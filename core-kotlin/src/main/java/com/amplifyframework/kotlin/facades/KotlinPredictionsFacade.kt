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
import com.amplifyframework.core.Amplify
import com.amplifyframework.kotlin.Predictions
import com.amplifyframework.predictions.PredictionsCategoryBehavior as Delegate
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.models.IdentifyAction
import com.amplifyframework.predictions.models.LanguageType
import com.amplifyframework.predictions.options.IdentifyOptions
import com.amplifyframework.predictions.options.InterpretOptions
import com.amplifyframework.predictions.options.TextToSpeechOptions
import com.amplifyframework.predictions.options.TranslateTextOptions
import com.amplifyframework.predictions.result.IdentifyResult
import com.amplifyframework.predictions.result.InterpretResult
import com.amplifyframework.predictions.result.TextToSpeechResult
import com.amplifyframework.predictions.result.TranslateTextResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class KotlinPredictionsFacade(private val delegate: Delegate = Amplify.Predictions) : Predictions {
    @Throws(PredictionsException::class)
    override suspend fun convertTextToSpeech(
        text: String,
        options: TextToSpeechOptions
    ): TextToSpeechResult {
        return suspendCoroutine { continuation ->
            delegate.convertTextToSpeech(
                text,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(PredictionsException::class)
    override suspend fun translateText(
        text: String,
        options: TranslateTextOptions
    ): TranslateTextResult {
        return suspendCoroutine { continuation ->
            delegate.translateText(
                text,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(PredictionsException::class)
    override suspend fun translateText(
        text: String,
        fromLanguage: LanguageType,
        toLanguage: LanguageType,
        options: TranslateTextOptions
    ): TranslateTextResult {
        return suspendCoroutine { continuation ->
            delegate.translateText(
                text,
                fromLanguage,
                toLanguage,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(PredictionsException::class)
    override suspend fun identify(
        actionType: IdentifyAction,
        image: Bitmap,
        options: IdentifyOptions
    ): IdentifyResult {
        return suspendCoroutine { continuation ->
            delegate.identify(
                actionType,
                image,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    @Throws(PredictionsException::class)
    override suspend fun interpret(text: String, options: InterpretOptions): InterpretResult {
        return suspendCoroutine { continuation ->
            delegate.interpret(
                text,
                options,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }
}
