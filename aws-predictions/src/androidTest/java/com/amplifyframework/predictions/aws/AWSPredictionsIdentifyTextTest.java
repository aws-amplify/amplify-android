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
import androidx.core.util.Pair;

import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.test.R;
import com.amplifyframework.predictions.models.BoundedKeyValue;
import com.amplifyframework.predictions.models.Cell;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.models.Table;
import com.amplifyframework.predictions.models.TextFormatType;
import com.amplifyframework.predictions.result.IdentifyDocumentTextResult;
import com.amplifyframework.predictions.result.IdentifyTextResult;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.FeatureAssert;
import com.amplifyframework.testutils.sync.SynchronousAuth;
import com.amplifyframework.testutils.sync.SynchronousPredictions;
import com.amplifyframework.util.Empty;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that Predictions identify delivers a non-null
 * text detection result for valid input.
 */
public final class AWSPredictionsIdentifyTextTest {

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
     * If {@link IdentifyActionType#DETECT_TEXT} is supplied instead of
     * {@link TextFormatType} as the action type for identify, then the operation
     * triggers error callback.
     * @throws PredictionsException if prediction fails.
     *          this test will throw due to invalid identify action type.
     */
    @Test(expected = PredictionsException.class)
    public void testUnspecificTextFormatDetectionTypeFails() throws PredictionsException {
        IdentifyActionType type = IdentifyActionType.DETECT_TEXT;
        final Bitmap image = Assets.readAsBitmap("sample-table.png");
        predictions.identify(type, image);
    }

    /**
     * Assert plain text detection works.
     * @throws PredictionsException if prediction fails
     */
    @Test
    public void testIdentifyPlainText() throws PredictionsException {
        final Bitmap image = Assets.readAsBitmap("sample-table.png");

        // Identify the text inside given image and assert non-null result.
        IdentifyTextResult result =
                (IdentifyTextResult) predictions.identify(TextFormatType.PLAIN, image);
        assertNotNull(result);

        // Assert matching text
        final String expectedText = Assets.readAsString("sample-table-expected-text.txt");
        final String actualText = result.getFullText();
        assertEquals(expectedText, actualText);
    }

    /**
     * Assert table detection works.
     * @throws PredictionsException if prediction fails
     */
    @Test
    public void testIdentifyTables() throws PredictionsException {
        final Bitmap image = Assets.readAsBitmap("sample-table.png");

        // Identify the text inside given image and assert non-null result.
        IdentifyDocumentTextResult result =
                (IdentifyDocumentTextResult) predictions.identify(TextFormatType.TABLE, image);
        assertNotNull(result);

        // Assert that one table is detected.
        List<Table> tables = result.getTables();
        assertFalse(Empty.check(tables));
        assertEquals(1, tables.size());

        // Assert that table has correct dimensions.
        Table table = tables.get(0);
        assertEquals(2, table.getRowSize());
        assertEquals(2, table.getColumnSize());

        // Assert that table has correct cells.
        List<Cell> cells = table.getCells();
        assertEquals(4, cells.size());
        FeatureAssert.assertContains("Name", cells);
        FeatureAssert.assertContains("Address", cells);
        FeatureAssert.assertContains("Ana Carolina", cells);
        FeatureAssert.assertContains("123 Any Town", cells);
    }

    /**
     * Assert form detection works.
     * @throws PredictionsException if prediction fails
     */
    @Test
    public void testIdentifyForms() throws PredictionsException {
        final Bitmap image = Assets.readAsBitmap("sample-form.png");

        // Identify the text inside given image and assert non-null result.
        IdentifyDocumentTextResult result =
                (IdentifyDocumentTextResult) predictions.identify(TextFormatType.FORM, image);

        // Assert that four key-values are detected.
        List<BoundedKeyValue> keyValues = result.getKeyValues();
        assertFalse(Empty.check(keyValues));
        assertEquals(4, keyValues.size());

        // Assert that key-value pairs have correct values.
        FeatureAssert.assertContains(Pair.create("Name:", "Jane Doe"), keyValues);
        FeatureAssert.assertContains(Pair.create("Address:", "123 Any Street, Anytown, USA"), keyValues);
        FeatureAssert.assertContains(Pair.create("Birth date:", "12-26-1980"), keyValues);
        // FeatureAssert.assertContains(Pair.create("Male:", "true"), keyValues); // Selection not supported
    }
}
