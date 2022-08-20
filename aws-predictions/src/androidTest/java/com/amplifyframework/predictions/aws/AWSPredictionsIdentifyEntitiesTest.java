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
import com.amplifyframework.predictions.models.EmotionType;
import com.amplifyframework.predictions.models.EntityDetails;
import com.amplifyframework.predictions.models.GenderBinaryType;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.result.IdentifyEntitiesResult;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.FeatureAssert;
import com.amplifyframework.testutils.sync.SynchronousAuth;
import com.amplifyframework.testutils.sync.SynchronousPredictions;
import com.amplifyframework.util.Empty;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that Predictions identify delivers a non-null
 * entity detection result for valid input.
 */
public final class AWSPredictionsIdentifyEntitiesTest {

    private static final IdentifyActionType TYPE = IdentifyActionType.DETECT_ENTITIES;

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
     * Assert general entity detection works.
     * @throws Exception if prediction fails
     */
    @Test
    @SuppressWarnings("MagicNumber") // Jeff Bezos' current age
    public void testIdentifyEntities() throws Exception {
        final Bitmap image = Assets.readAsBitmap("jeff_bezos.jpg");

        // Identify the entity inside given image and assert non-null result.
        IdentifyEntitiesResult result = (IdentifyEntitiesResult) predictions.identify(TYPE, image);
        assertNotNull(result);

        // Assert that at least one entity is detected
        assertFalse(Empty.check(result.getEntities()));
        EntityDetails entity = result.getEntities().get(0);

        // Assert features from detected entity
        FeatureAssert.assertMatches(GenderBinaryType.MALE, entity.getGender());
        FeatureAssert.assertMatches(EmotionType.HAPPY, Collections.max(entity.getEmotions()));
        assertNotNull(entity.getAgeRange());

        int jeffAge = 56;

        // When this test was created, Rekognition returned an age range containing Jeff's age.  The algorithm
        // recently changed to predict an age range of 37-55.  The goal of this test is to verify a sane response,
        // not verify the accurate of Rekognition, so we will add a 20 year buffer to the age range (e.g. if age range
        // is 37 to 55, the test will pass because 56 is still between 37-20 (17) and 55+20 (75)).
        int buffer = 20;

        assertTrue(jeffAge <= entity.getAgeRange().getHigh() + buffer);
        assertTrue(jeffAge >= entity.getAgeRange().getLow() - buffer);
    }
}
