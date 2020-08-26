/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.rx;

import android.graphics.Bitmap;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsCategoryBehavior;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.operation.IdentifyOperation;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.operation.TextToSpeechOperation;
import com.amplifyframework.predictions.operation.TranslateTextOperation;
import com.amplifyframework.predictions.result.IdentifyDocumentTextResult;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TextToSpeechResult;
import com.amplifyframework.predictions.result.TranslateTextResult;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;

import static com.amplifyframework.rx.Matchers.anyConsumer;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;

/**
 * Tests that the {@link RxPredictionsBinding} works by proxying calls into
 * an {@link PredictionsCategoryBehavior} instance.
 */
@RunWith(RobolectricTestRunner.class)
public final class RxPredictionsBindingTest {
    private static final long TIMEOUT_SECONDS = 2;

    private PredictionsCategoryBehavior delegate;
    private RxPredictionsCategoryBehavior rxPredictions;

    /**
     * Creates an {@link RxPredictionsBinding}, under test, and a mock
     * {@link PredictionsCategoryBehavior}.
     */
    @Before
    public void setup() {
        delegate = mock(PredictionsCategoryBehavior.class);
        rxPredictions = new RxPredictionsBinding(delegate);
    }

    /**
     * When the delegate behavior succeeds for the {@link RxPredictionsBinding#convertTextToSpeech(String)}
     * its result value should be emitted via the returned {@link Single}.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void testSuccessfulTextToSpeechConversion() throws InterruptedException {
        String text = RandomString.string();
        TextToSpeechResult result = TextToSpeechResult.fromAudioData(new ByteArrayInputStream(new byte[0]));
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 1; // 0 = text, 1 = result, 2 = error
            Consumer<TextToSpeechResult> onResult = invocation.getArgument(indexOfResultConsumer);
            onResult.accept(result);
            return mock(TextToSpeechOperation.class);
        }).when(delegate).convertTextToSpeech(eq(text), anyConsumer(), anyConsumer());
        TestObserver<TextToSpeechResult> observer = rxPredictions.convertTextToSpeech(text).test();
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertValue(result);
    }

    /**
     * When the delegate behavior fails for the {@link RxPredictionsBinding#convertTextToSpeech(String)}
     * its emitted error should be propagated via the returned {@link Single}.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void testFailedTextToSpeechConversion() throws InterruptedException {
        String text = RandomString.string();
        PredictionsException predictionsException = new PredictionsException("Uh", "Oh!");
        doAnswer(invocation -> {
            final int indexOfFailureConsumer = 2; // 0 = text, 1 = result, 2 = error
            Consumer<PredictionsException> onFailure = invocation.getArgument(indexOfFailureConsumer);
            onFailure.accept(predictionsException);
            return mock(TextToSpeechOperation.class);
        }).when(delegate).convertTextToSpeech(eq(text), anyConsumer(), anyConsumer());
        TestObserver<TextToSpeechResult> observer = rxPredictions.convertTextToSpeech(text).test();
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertError(predictionsException);
    }

    /**
     * When the delegate returns a result for the {@link RxPredictionsBinding#translateText(String)}
     * call, the result should be emitted via the returned {@link Single}.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void testSuccessfulTranslateText() throws InterruptedException {
        String text = "Cat";
        TranslateTextResult result = TranslateTextResult.builder()
            .targetLanguage(LanguageType.SPANISH)
            .translatedText("Gato")
            .build();
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 1; // 0 = text, 1 = result, 2 = error
            Consumer<TranslateTextResult> onResult = invocation.getArgument(indexOfResultConsumer);
            onResult.accept(result);
            return mock(TranslateTextOperation.class);
        }).when(delegate).translateText(eq(text), anyConsumer(), anyConsumer());
        TestObserver<TranslateTextResult> observer = rxPredictions.translateText(text).test();
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertValue(result);
    }

    /**
     * When the delegate emits an error for the {@link RxPredictionsBinding#translateText(String)}
     * call, that error should be propagated via the returned {@link Single}.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void testFailedTranslateText() throws InterruptedException {
        String text = "Cat";
        PredictionsException predictionsException = new PredictionsException("Uh", "Oh");
        doAnswer(invocation -> {
            final int indexOfFailureConsumer = 2; // 0 = text, 1 = result, 2 = error
            Consumer<PredictionsException> onFailure = invocation.getArgument(indexOfFailureConsumer);
            onFailure.accept(predictionsException);
            return mock(TranslateTextOperation.class);
        }).when(delegate).translateText(eq(text), anyConsumer(), anyConsumer());
        TestObserver<TranslateTextResult> observer = rxPredictions.translateText(text).test();
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertError(predictionsException);
    }

    /**
     * When the delegate of {@link RxPredictionsBinding#identify(IdentifyAction, Bitmap)} succeeds,
     * the result should be propagated via the returned {@link Single}.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void testSuccessfulImageIdentification() throws InterruptedException {
        Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        IdentifyDocumentTextResult result = IdentifyDocumentTextResult.builder()
            .fullText(RandomString.string())
            .keyValues(Collections.emptyList())
            .lines(Collections.emptyList())
            .rawLineText(Collections.emptyList())
            .selections(Collections.emptyList())
            .tables(Collections.emptyList())
            .words(Collections.emptyList())
            .build();
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 2; // 0 = type, 1 = bitmap, 2 = result, 3 = error
            Consumer<IdentifyDocumentTextResult> onResult = invocation.getArgument(indexOfResultConsumer);
            onResult.accept(result);
            return mock(IdentifyOperation.class);
        }).when(delegate).identify(eq(IdentifyActionType.DETECT_TEXT), eq(bitmap), anyConsumer(), anyConsumer());
        TestObserver<IdentifyResult> observer =
            rxPredictions.identify(IdentifyActionType.DETECT_TEXT, bitmap).test();
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertValue(result);
    }

    /**
     * When the delegate of {@link RxPredictionsBinding#identify(IdentifyAction, Bitmap)} fails,
     * the failure should be propagated via the returned {@link Single}.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void testFailedImageIdentification() throws InterruptedException {
        Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        PredictionsException predictionsException = new PredictionsException("Uh", "Oh");
        doAnswer(invocation -> {
            final int indexOfFailureConsumer = 3; // 0 = type, 1 = bitmap, 2 = result, 3 = error
            Consumer<PredictionsException> onFailure = invocation.getArgument(indexOfFailureConsumer);
            onFailure.accept(predictionsException);
            return mock(IdentifyOperation.class);
        }).when(delegate).identify(eq(IdentifyActionType.DETECT_TEXT), eq(bitmap), anyConsumer(), anyConsumer());
        TestObserver<IdentifyResult> observer =
            rxPredictions.identify(IdentifyActionType.DETECT_TEXT, bitmap).test();
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertError(predictionsException);
    }

    /**
     * When the delegate of {@link RxPredictionsBinding#interpret(String)} emits a result,
     * it should be propagated via the returned {@link Single}.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void testSuccessfulTextInterpretation() throws InterruptedException {
        String text = RandomString.string();
        InterpretResult result = InterpretResult.builder().build();
        doAnswer(invocation -> {
            final int indexOfResultConsumer = 1; // 0 = text, 1 = result, 2 = error
            Consumer<InterpretResult> onResult = invocation.getArgument(indexOfResultConsumer);
            onResult.accept(result);
            return mock(InterpretOperation.class);
        }).when(delegate).interpret(eq(text), anyConsumer(), anyConsumer());
        TestObserver<InterpretResult> observer = rxPredictions.interpret(text).test();
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertValue(result);
    }

    /**
     * When the delegate of {@link RxPredictionsBinding#interpret(String)} emits a failure,
     * it should be propagated via the returned {@link Single}.
     * @throws InterruptedException If interrupted while test observer is awaiting terminal event
     */
    @Test
    public void testFailedTextInterpretation() throws InterruptedException {
        String text = RandomString.string();
        PredictionsException predictionsException = new PredictionsException("Uh", "Oh");
        doAnswer(invocation -> {
            final int indexOfFailureConsumer = 2; // 0 = text, 1 = result, 2 = error
            Consumer<PredictionsException> onFailure = invocation.getArgument(indexOfFailureConsumer);
            onFailure.accept(predictionsException);
            return mock(InterpretOperation.class);
        }).when(delegate).interpret(eq(text), anyConsumer(), anyConsumer());
        TestObserver<InterpretResult> observer = rxPredictions.interpret(text).test();
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertError(predictionsException);
    }
}
