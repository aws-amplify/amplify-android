/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws;

import android.content.Context;

import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.test.R;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.result.TranslateTextResult;
import com.amplifyframework.testutils.sync.SynchronousAuth;
import com.amplifyframework.testutils.sync.SynchronousPredictions;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that Predictions translate delivers a non-null result
 * for valid input.
 */
public final class AWSPredictionsTranslateTest {

    private SynchronousPredictions predictions;

    /**
     * Configure Predictions category before each test.
     * @throws Exception if {@link SynchronousAuth} initialization fails
     */
    @Before
    public void setUp() throws Exception {
        
        Context context = getApplicationContext();

        // Set up Auth
        SynchronousAuth.delegatingToCognito(context, new AWSCognitoAuthPlugin());

        // Delegate to Predictions category
        PredictionsCategory asyncDelegate =
                TestPredictionsCategory.create(context, R.raw.amplifyconfiguration);
        predictions = SynchronousPredictions.delegatingTo(asyncDelegate);
    }

    /**
     * Assert that translate fails for blank input.
     * AWS Translate will not accept a blank input, so
     * an exception is thrown.
     * @throws Exception if prediction fails
     */
    @Test(expected = PredictionsException.class)
    public void testTranslateFailsForBlankInput() throws Exception {
        predictions.translateText("");
    }

    /**
     * Assert that category falls back to configured default language.
     * @throws Exception if prediction fails
     */
    @Test
    public void testTranslationWithDefaultLanguage() throws Exception {
        final String sampleText = "Hello world!";

        // Translate english text and assert non-null result.
        // Use configured default languages
        TranslateTextResult result = predictions.translateText(sampleText);
        assertNotNull(result);

        // Assert translated language is in Spanish
        LanguageType language = result.getTargetLanguage();
        assertEquals(LanguageType.SPANISH, language);

        // Assert translation
        String translation = result.getTranslatedText();
        assertTrue(translation.contains("Hola mundo"));
    }

    /**
     * Assert that category falls back to configured default language.
     * @throws Exception if prediction fails
     */
    @Test
    public void testTranslationWithLanguageOverride() throws Exception {
        final String sampleText = "¡Hola mundo!";

        // Translate english text and assert non-null result.
        // Use configured default languages
        TranslateTextResult result = predictions.translateText(
                sampleText,
                LanguageType.SPANISH,
                LanguageType.ENGLISH
        );
        assertNotNull(result);

        // Assert translated language is in English
        LanguageType language = result.getTargetLanguage();
        assertEquals(LanguageType.ENGLISH, language);

        // Assert translation
        String translation = result.getTranslatedText();
        assertEquals("Hello world!", translation);
    }
}
