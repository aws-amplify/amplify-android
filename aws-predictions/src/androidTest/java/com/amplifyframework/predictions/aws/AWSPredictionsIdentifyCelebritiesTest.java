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
import android.graphics.Bitmap;

import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.aws.test.R;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.result.IdentifyCelebritiesResult;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.sync.SynchronousAuth;
import com.amplifyframework.testutils.sync.SynchronousPredictions;
import com.amplifyframework.util.Empty;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.rxjava3.core.Observable;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that Predictions identify delivers a non-null
 * celebrity detection result for valid input.
 */
public final class AWSPredictionsIdentifyCelebritiesTest {

    private static final IdentifyActionType TYPE = IdentifyActionType.DETECT_CELEBRITIES;

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
     * Assert celebrity detection works.
     * @throws Exception if prediction fails
     */
    @Test
    public void testIdentifyCelebrities() throws Exception {
        final Bitmap image = Assets.readAsBitmap("jeff_bezos.jpg");

        // Identify the celebrity inside given image and assert non-null result.
        IdentifyCelebritiesResult result = (IdentifyCelebritiesResult) predictions.identify(TYPE, image);
        assertNotNull(result);

        // Assert that Jeff Bezos is detected
        assertFalse(Empty.check(result.getCelebrities()));
        assertTrue(Observable.fromIterable(result.getCelebrities())
                .map(celeb -> celeb.getCelebrity().getName())
                .toList()
                .blockingGet()
                .contains("Jeff Bezos"));
    }
}
