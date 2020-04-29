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

package com.amplifyframework.predictions.aws.adapter;

import android.graphics.PointF;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.models.BoundedKeyValue;
import com.amplifyframework.predictions.models.Cell;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Table;
import com.amplifyframework.util.CollectionUtils;

import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.BlockType;
import com.amazonaws.services.textract.model.BoundingBox;
import com.amazonaws.services.textract.model.EntityType;
import com.amazonaws.services.textract.model.Point;
import com.amazonaws.services.textract.model.Relationship;
import com.amazonaws.services.textract.model.SelectionStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class to transform Amazon service-specific
 * models to be compatible with AWS Amplify.
 */
public final class IdentifyTextResultTransformers {
    private IdentifyTextResultTransformers() {}

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
        if (CollectionUtils.isNullOrEmpty(polygon)) {
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
     * Converts a given Amazon Textract block into Amplify-compatible
     * table object. Returns null if not a valid table.
     * @param block Textract text block
     * @param blockMap map of Textract blocks by their IDs
     * @return Amplify Table instance
     */
    @Nullable
    public static Table processTable(@NonNull Block block, @NonNull Map<String, Block> blockMap) {
        Objects.requireNonNull(block);
        Objects.requireNonNull(blockMap);

        if (!BlockType.TABLE.toString().equals(block.getBlockType())) {
            return null;
        }

        RectF box = fromBoundingBox(block.getGeometry().getBoundingBox());
        Polygon polygon = fromPoints(block.getGeometry().getPolygon());
        List<Cell> cells = new ArrayList<>();
        Set<Integer> rows = new HashSet<>();
        Set<Integer> cols = new HashSet<>();
        // Each TABLE block contains CELL blocks
        doForEachRelatedBlock(block, blockMap, cellBlock -> {
            rows.add(cellBlock.getRowIndex() - 1);
            cols.add(cellBlock.getColumnIndex() - 1);
            cells.add(processTableCell(cellBlock, blockMap));
        });

        return Table.builder()
                .cells(cells)
                .confidence(block.getConfidence())
                .box(box)
                .polygon(polygon)
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
    public static BoundedKeyValue processKeyValue(@NonNull Block block, @NonNull Map<String, Block> blockMap) {
        Objects.requireNonNull(block);
        Objects.requireNonNull(blockMap);

        if (!BlockType.KEY_VALUE_SET.toString().equals(block.getBlockType())) {
            return null;
        }

        // Must be of entity type "KEY"
        List<String> entityTypes = block.getEntityTypes();
        if (entityTypes == null || !entityTypes.contains(EntityType.KEY.toString())) {
            return null;
        }

        RectF box = fromBoundingBox(block.getGeometry().getBoundingBox());
        Polygon polygon = fromPoints(block.getGeometry().getPolygon());
        StringBuilder keyBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();
        doForEachRelatedBlock(block, blockMap, relatedBlock -> {
            // For key block
            if (relatedBlock.getText() != null) {
                keyBuilder.append(relatedBlock.getText()).append(" ");
            }

            // For value block
            doForEachRelatedBlock(relatedBlock, blockMap, valueBlock -> {
                valueBuilder.append(valueBlock.getText()).append(" ");
            });
        });

        return BoundedKeyValue.builder()
                .keyValuePair(keyBuilder.toString().trim(),
                        valueBuilder.toString().trim())
                .confidence(block.getConfidence())
                .box(box)
                .polygon(polygon)
                .build();
    }

    private static Cell processTableCell(Block block, Map<String, Block> blockMap) {
        RectF box = fromBoundingBox(block.getGeometry().getBoundingBox());
        Polygon polygon = fromPoints(block.getGeometry().getPolygon());
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
            String selectionStatus = relatedBlock.getSelectionStatus();
            if (selectionStatus != null) {
                isSelected.set(SelectionStatus.SELECTED.toString().equals(selectionStatus));
            }
        });

        return Cell.builder()
                .text(wordsBuilder.toString().trim())
                .confidence(block.getConfidence())
                .box(box)
                .polygon(polygon)
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
            for (String id : relationship.getIds()) {
                Block relatedBlock = blockMap.get(id);
                if (relatedBlock == null) {
                    continue;
                }
                forEach.accept(relatedBlock);
            }
        }
    }
}
