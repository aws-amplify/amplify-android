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

package com.amplifyframework.predictions.aws.adapter;

import android.graphics.PointF;
import android.graphics.RectF;

import com.amplifyframework.predictions.models.BoundedKeyValue;
import com.amplifyframework.predictions.models.Cell;
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Selection;
import com.amplifyframework.predictions.models.Table;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import aws.sdk.kotlin.services.textract.model.Block;
import aws.sdk.kotlin.services.textract.model.BlockType;
import aws.sdk.kotlin.services.textract.model.BoundingBox;
import aws.sdk.kotlin.services.textract.model.EntityType;
import aws.sdk.kotlin.services.textract.model.Geometry;
import aws.sdk.kotlin.services.textract.model.Point;
import aws.sdk.kotlin.services.textract.model.Relationship;
import aws.sdk.kotlin.services.textract.model.SelectionStatus;
import io.reactivex.rxjava3.core.Observable;
import kotlin.Unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the result transformer utility methods work
 * as intended.
 */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("ConstantConditions") // NullPointerException will not be thrown
public final class TextractResultTransformersTest {
    private static final double DELTA = 1E-5;

    private Random random;

    /**
     * Sets up test dependencies.
     */
    @Before
    public void setUp() {
        random = new Random();
    }

    /**
     * Tests that the rectangular boundary from Textract
     * is converted to an equivalent Android rectangle object.
     */
    @Test
    public void testBoundingBoxConversion() {
        BoundingBox box = randomBox();
        RectF rect = TextractResultTransformers.fromBoundingBox(box);
        assertEquals(box.getHeight(), rect.height(), DELTA);
        assertEquals(box.getWidth(), rect.width(), DELTA);
        assertEquals(box.getLeft(), rect.left, DELTA);
        assertEquals(box.getTop(), rect.top, DELTA);
    }

    /**
     * Tests that the polygonal boundary from Textract in the form
     * of list of points is converted to an Amplify shape for polygons.
     */
    @Test
    public void testPolygonConversion() {
        List<Point> randomPolygon = randomPolygon();
        Polygon polygon = TextractResultTransformers.fromPoints(randomPolygon);
        List<PointF> actualPoints = polygon.getPoints();
        List<PointF> expectedPoints = new ArrayList<>();
        for (Point point : randomPolygon) {
            expectedPoints.add(new PointF(point.getX(), point.getY()));
        }
        assertEquals(expectedPoints, actualPoints);
    }

    /**
     * Tests that the individual block from Textract is converted to
     * an Amplify image text feature.
     */
    @Test
    public void testIdentifiedTextConversion() {
        Block block = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setText(RandomString.string());
            blockBuilder.setConfidence(random.nextFloat());
            blockBuilder.setGeometry(randomGeometry());
            blockBuilder.setPage(random.nextInt());
            return Unit.INSTANCE;
        });

        // Test block conversion
        IdentifiedText text = TextractResultTransformers.fetchIdentifiedText(block);
        assertEquals(block.getText(), text.getText());
        assertEquals(block.getConfidence(), text.getConfidence(), DELTA);
        assertEquals((int) block.getPage(), text.getPage());
    }

    /**
     * Tests that the individual block from Textract is properly
     * converted to an Amplify selection item.
     */
    @Test
    public void testSelectionConversion() {
        Block block;
        Selection selection;

        // Assert that SELECTED sets it to selected
        block = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setSelectionStatus(SelectionStatus.Selected.INSTANCE);
            blockBuilder.setGeometry(randomGeometry());
            return Unit.INSTANCE;
        });
        selection = TextractResultTransformers.fetchSelection(block);
        assertTrue(selection.isSelected());

        // Assert that NOT_SELECTED sets it to not selected
        block = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setSelectionStatus(SelectionStatus.NotSelected.INSTANCE);
            blockBuilder.setGeometry(randomGeometry());
            return Unit.INSTANCE;
        });
        selection = TextractResultTransformers.fetchSelection(block);
        assertFalse(selection.isSelected());
    }

    /**
     * Tests that the graph of related blocks from Textract is properly
     * converted to an Amplify table item.
     */
    @Test
    public void testTableConversion() {
        Block cellTextBlock = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setId(RandomString.string());
            blockBuilder.setText(RandomString.string());
            return Unit.INSTANCE;
        });
        Block cellSelectionBlock = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setId(RandomString.string());
            blockBuilder.setSelectionStatus(SelectionStatus.Selected.INSTANCE);
            return Unit.INSTANCE;
        });
        Relationship cellRelationship = Relationship.Companion.invoke((relationshipBuilder) -> {
            List<String> ids = Arrays.asList(cellTextBlock.getId(), cellSelectionBlock.getId());
            relationshipBuilder.setIds(ids);
            return Unit.INSTANCE;
        });
        Block cellBlock = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setId(RandomString.string());
            blockBuilder.setBlockType(BlockType.Cell.INSTANCE);
            blockBuilder.setConfidence(random.nextFloat());
            blockBuilder.setGeometry(randomGeometry());
            blockBuilder.setRowIndex(random.nextInt());
            blockBuilder.setColumnIndex(random.nextInt());
            blockBuilder.setRelationships(Collections.singletonList(cellRelationship));
            return Unit.INSTANCE;     
        });
        Relationship tableRelationship = Relationship.Companion.invoke((relationshipBuilder) -> {
            relationshipBuilder.setIds(Collections.singletonList(cellBlock.getId()));
            return Unit.INSTANCE;
        });
        Block tableBlock = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setId(RandomString.string());
            blockBuilder.setBlockType(BlockType.Table.INSTANCE);
            blockBuilder.setConfidence(random.nextFloat());
            blockBuilder.setGeometry(randomGeometry());
            blockBuilder.setRelationships(Collections.singletonList(tableRelationship));
            return Unit.INSTANCE;
        });

        // Construct a map to act as a graph
        Map<String, Block> blockMap = new HashMap<>();
        Observable.fromArray(cellTextBlock, cellSelectionBlock, cellBlock, tableBlock)
                .blockingForEach(block -> blockMap.put(block.getId(), block));

        // Test table block conversion
        Table table = TextractResultTransformers.fetchTable(tableBlock, blockMap);
        assertEquals(1, table.getCells().size());
        assertEquals(tableBlock.getConfidence(), table.getConfidence(), DELTA);
        assertEquals(1, table.getRowSize());
        assertEquals(1, table.getColumnSize());

        // Test cell block conversion
        Cell cell = table.getCells().iterator().next();
        assertEquals(cellTextBlock.getText(), cell.getText());
        assertEquals(cellBlock.getConfidence(), cell.getConfidence(), DELTA);
        assertTrue(cell.isSelected());
        assertEquals(cellBlock.getRowIndex() - 1, cell.getRow());
        assertEquals(cellBlock.getColumnIndex() - 1, cell.getColumn());
    }

    /**
     * Tests that the graph of related blocks from Textract is properly
     * converted to an Amplify key-value pair item.
     */
    @Test
    public void testKeyValueConversion() {
        Block valueTextBlock = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setId(RandomString.string());
            blockBuilder.setText(RandomString.string());
            return Unit.INSTANCE;
        });
        Block keyTextBlock = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setId(RandomString.string());
            blockBuilder.setText(RandomString.string());
            return Unit.INSTANCE;
        });
        Relationship valueRelationship = Relationship.Companion.invoke((relationshipBuilder) -> {
            relationshipBuilder.setIds(Collections.singletonList(valueTextBlock.getId()));
            return Unit.INSTANCE;
        });
        Block valueBlock = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setId(RandomString.string());
            blockBuilder.setBlockType(BlockType.KeyValueSet.INSTANCE);
            blockBuilder.setEntityTypes(Collections.singletonList(EntityType.Value.INSTANCE));
            blockBuilder.setRelationships(Collections.singletonList(valueRelationship));
            return Unit.INSTANCE;
        });
        Relationship keyRelationship = Relationship.Companion.invoke((relationshipBuilder) -> {
            List<String> ids = Arrays.asList(keyTextBlock.getId(), valueBlock.getId());
            relationshipBuilder.setIds(ids);
            return Unit.INSTANCE;
        });
        Block keyBlock = Block.Companion.invoke((blockBuilder) -> {
            blockBuilder.setId(RandomString.string());
            blockBuilder.setBlockType(BlockType.KeyValueSet.INSTANCE);
            blockBuilder.setEntityTypes(Collections.singletonList(EntityType.Key.INSTANCE));
            blockBuilder.setConfidence(random.nextFloat());
            blockBuilder.setGeometry(randomGeometry());
            blockBuilder.setRelationships(Collections.singletonList(keyRelationship));
            return Unit.INSTANCE;
        });

        // Construct a map to act as a graph
        Map<String, Block> blockMap = new HashMap<>();
        Observable.fromArray(valueTextBlock, keyTextBlock, valueBlock, keyBlock)
                .blockingForEach(block -> blockMap.put(block.getId(), block));

        // Test block conversion
        BoundedKeyValue keyValue = TextractResultTransformers.fetchKeyValue(keyBlock, blockMap);
        assertEquals(keyTextBlock.getText(), keyValue.getKey());
        assertEquals(valueTextBlock.getText(), keyValue.getKeyValue());
        assertEquals(keyBlock.getConfidence(), keyValue.getConfidence(), DELTA);
    }

    private BoundingBox randomBox() {
        return BoundingBox.Companion.invoke((boxBuilder) -> {
            boxBuilder.setHeight(random.nextFloat());
            boxBuilder.setLeft(random.nextFloat());
            boxBuilder.setTop(random.nextFloat());
            boxBuilder.setWidth(random.nextFloat());
            return Unit.INSTANCE;
        });
    }

    private List<Point> randomPolygon() {
        final int minPoints = 3;
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < minPoints; i++) {
            points.add(Point.Companion.invoke((pointBuilder) -> {
                pointBuilder.setX(random.nextFloat());
                pointBuilder.setY(random.nextFloat());
                return Unit.INSTANCE;
            }));
        }
        return points;
    }

    private Geometry randomGeometry() {
        return Geometry.Companion.invoke((geometryBuilder) -> {
            geometryBuilder.setBoundingBox(randomBox());
            geometryBuilder.setPolygon(randomPolygon());
            return Unit.INSTANCE;
        });
    }
}
