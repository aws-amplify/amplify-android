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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.models.BoundedKeyValue;
import com.amplifyframework.predictions.models.Cell;
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Selection;
import com.amplifyframework.predictions.models.Table;
import com.amplifyframework.util.Empty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import aws.sdk.kotlin.services.textract.model.Block;
import aws.sdk.kotlin.services.textract.model.BlockType;
import aws.sdk.kotlin.services.textract.model.BoundingBox;
import aws.sdk.kotlin.services.textract.model.EntityType;
import aws.sdk.kotlin.services.textract.model.Point;
import aws.sdk.kotlin.services.textract.model.Relationship;
import aws.sdk.kotlin.services.textract.model.SelectionStatus;

/**
 * Utility class to transform Amazon Textract service-specific
 * models to be compatible with AWS Amplify.
 */
public final class TextractResultTransformers {
    private TextractResultTransformers() {}

    /**
     * Converts bounding box geometry from Amazon Rekognition into
     * Android's graphic {@link RectF} object for Amplify
     * compatibility.
     * @param box the bounding box provided by Amazon Rekognition
     * @return the RectF object representing the same dimensions
     */
    @Nullable
    public static RectF fromBoundingBox(@Nullable BoundingBox box) {
        if (box == null) {
            return null;
        }
        return new RectF(
                box.getLeft(),
                box.getTop(),
                box.getLeft() + box.getWidth(),
                box.getTop() + box.getHeight()
        );
    }

    /**
     * Converts geometric polygon from Amazon Textract into
     * Amplify-compatible polygon object.
     * @param polygon the polygon object by Amazon Textract
     * @return the polygon object representing vertices
     */
    @Nullable
    public static Polygon fromPoints(@Nullable List<Point> polygon) {
        if (Empty.check(polygon)) {
            return null;
        }
        List<PointF> points = new ArrayList<>();
        for (Point point : polygon) {
            PointF androidPoint = new PointF(
                    point.getX(),
                    point.getY()
            );
            points.add(androidPoint);
        }
        return Polygon.fromPoints(points);
    }

    /**
     * Converts a given Amazon Textract block into Amplify's
     * image text feature. Returns null if not a valid text.
     * @param block Textract text block
     * @return Amplify text feature instance
     */
    @Nullable
    public static IdentifiedText fetchIdentifiedText(@Nullable Block block) {
        if (block == null || block.getText() == null || block.getConfidence() == null) {
            return null;
        }
        List<Point> polygon = null;
        if (block.getGeometry() != null) {
            polygon = block.getGeometry().getPolygon();
        }
        RectF box = null;
        if (block.getGeometry() != null) {
            box = fromBoundingBox(block.getGeometry().getBoundingBox());
        }
        return IdentifiedText.builder()
                .text(block.getText())
                .confidence(block.getConfidence())
                .box(box)
                .polygon(fromPoints(polygon))
                .page(block.getPage() != null ? block.getPage() : 0)
                .build();
    }

    /**
     * Converts a given Amazon Textract block into Amplify's
     * Selection feature. Returns null if not a valid selection.
     * @param block Textract selection block
     * @return Amplify text feature instance
     */
    @Nullable
    public static Selection fetchSelection(@Nullable Block block) {
        if (block == null || block.getSelectionStatus() == null) {
            return null;
        }
        List<Point> polygon = null;
        if (block.getGeometry() != null) {
            polygon = block.getGeometry().getPolygon();
        }
        RectF box = null;
        if (block.getGeometry() != null) {
            box = fromBoundingBox(block.getGeometry().getBoundingBox());
        }
        return Selection.builder()
                .box(box)
                .polygon(fromPoints(polygon))
                .selected(SelectionStatus.Selected.INSTANCE.equals(block.getSelectionStatus()))
                .build();
    }

    /**
     * Converts a given Amazon Textract block into Amplify-compatible
     * table object. Returns null if not a valid table.
     * @param block Textract text block
     * @param blockMap map of Textract blocks by their IDs
     * @return Amplify Table instance
     */
    @Nullable
    public static Table fetchTable(@Nullable Block block, @NonNull Map<String, Block> blockMap) {
        Objects.requireNonNull(blockMap);
        if (block == null || !BlockType.Table.INSTANCE.equals(block.getBlockType()) || block.getConfidence() == null) {
            return null;
        }
        List<Cell> cells = new ArrayList<>();
        Set<Integer> rows = new HashSet<>();
        Set<Integer> cols = new HashSet<>();

        // Each TABLE block contains CELL blocks
        doForEachRelatedBlock(block, blockMap, cellBlock -> {
            if (cellBlock.getRowIndex() != null && cellBlock.getColumnIndex() != null) {
                rows.add(cellBlock.getRowIndex() - 1);
                cols.add(cellBlock.getColumnIndex() - 1);
                cells.add(fetchTableCell(cellBlock, blockMap));
            }
        });
        List<Point> polygon = null;
        if (block.getGeometry() != null) {
            polygon = block.getGeometry().getPolygon();
        }
        RectF box = null;
        if (block.getGeometry() != null) {
            box = fromBoundingBox(block.getGeometry().getBoundingBox());
        }
        return Table.builder()
                .cells(cells)
                .confidence(block.getConfidence())
                .box(box)
                .polygon(fromPoints(polygon))
                .rowSize(rows.size())
                .columnSize(cols.size())
                .build();
    }

    /**
     * Converts a given Amazon Textract block into Amplify-compatible
     * key-value pair feature. Returns null if not a valid table.
     * @param block Textract text block
     * @param blockMap map of Textract blocks by their IDs
     * @return Amplify Table instance
     */
    @Nullable
    public static BoundedKeyValue fetchKeyValue(@Nullable Block block, @NonNull Map<String, Block> blockMap) {
        Objects.requireNonNull(blockMap);
        if (block == null || !BlockType.KeyValueSet.INSTANCE.equals(block.getBlockType())
                || block.getConfidence() == null) {
            return null;
        }
        // Must be of entity type "KEY"
        List<EntityType> entityTypes = block.getEntityTypes();
        if (entityTypes == null || !entityTypes.contains(EntityType.Key.INSTANCE)) {
            return null;
        }
        StringBuilder keyBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();

        // KeyValueSet block contains either Key or Value entity type blocks
        doForEachRelatedBlock(block, blockMap, relatedBlock -> {
            // For Key entity block
            if (relatedBlock.getText() != null) {
                keyBuilder.append(relatedBlock.getText()).append(" ");
            }
            // For Value entity block
            doForEachRelatedBlock(relatedBlock, blockMap, valueBlock -> {
                valueBuilder.append(valueBlock.getText()).append(" ");
            });
        });

        String key = keyBuilder.toString().trim();
        String value = valueBuilder.toString().trim();
        List<Point> polygon = null;
        if (block.getGeometry() != null) {
            polygon = block.getGeometry().getPolygon();
        }
        RectF box = null;
        if (block.getGeometry() != null) {
            box = fromBoundingBox(block.getGeometry().getBoundingBox());
        }
        return BoundedKeyValue.builder()
                .keyValuePair(key, value)
                .confidence(block.getConfidence())
                .box(box)
                .polygon(fromPoints(polygon))
                .build();
    }

    @Nullable
    private static Cell fetchTableCell(@Nullable Block block, @NonNull Map<String, Block> blockMap) {
        Objects.requireNonNull(blockMap);
        if (block == null || !BlockType.Cell.INSTANCE.equals(block.getBlockType()) || block.getConfidence() == null) {
            return null;
        }
        StringBuilder wordsBuilder = new StringBuilder();
        AtomicBoolean isSelected = new AtomicBoolean(false);

        // Each CELL block consists of WORD and/or SELECTION_ELEMENT blocks
        doForEachRelatedBlock(block, blockMap, relatedBlock -> {
            // For WORD block
            String text = relatedBlock.getText();
            if (text != null) {
                wordsBuilder.append(text).append(" ");
            }
            // For SELECTION_ELEMENT block
            SelectionStatus selectionStatus = relatedBlock.getSelectionStatus();
            if (selectionStatus != null) {
                isSelected.set(SelectionStatus.Selected.INSTANCE.equals(selectionStatus));
            }
        });

        List<Point> polygon = null;
        if (block.getGeometry() != null) {
            polygon = block.getGeometry().getPolygon();
        }
        if (block.getRowIndex() == null || block.getColumnIndex() == null) {
            return null;
        }
        RectF box = null;
        if (block.getGeometry() != null) {
            box = fromBoundingBox(block.getGeometry().getBoundingBox());
        }
        return Cell.builder()
                .text(wordsBuilder.toString().trim())
                .confidence(block.getConfidence())
                .box(box)
                .polygon(fromPoints(polygon))
                .selected(isSelected.get())
                .row(block.getRowIndex() - 1)
                .column(block.getColumnIndex() - 1)
                .build();
    }

    private static void doForEachRelatedBlock(
            Block block,
            Map<String, Block> blockMap,
            Consumer<Block> forEach
    ) {
        if (block == null || block.getRelationships() == null) {
            return;
        }

        for (Relationship relationship : block.getRelationships()) {
            List<String> relationshipIds = relationship.getIds();
            if (relationshipIds != null) {
                for (String id : relationshipIds) {
                    Block relatedBlock = blockMap.get(id);
                    if (relatedBlock == null) {
                        continue;
                    }
                    forEach.accept(relatedBlock);
                }
            }
        }
    }
}
