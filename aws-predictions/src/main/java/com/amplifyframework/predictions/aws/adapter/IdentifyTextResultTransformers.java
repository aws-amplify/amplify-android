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
import com.amazonaws.services.textract.model.RelationshipType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        for (Relationship relationship : block.getRelationships()) {
            for (String cellId : relationship.getIds()) {
                Block cellBlock = blockMap.get(cellId);
                if (cellBlock == null) {
                    continue;
                }
                rows.add(cellBlock.getRowIndex() - 1);
                cols.add(cellBlock.getColumnIndex() - 1);
                cells.add(processTableCell(cellBlock, blockMap));
            }
        }

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
        for (Relationship relationship : block.getRelationships()) {
            RelationshipType type = RelationshipType.fromValue(relationship.getType());
            for (String id : relationship.getIds()) {
                Block relatedBlock = blockMap.get(id);
                if (relatedBlock == null) {
                    continue;
                }
                String text = relatedBlock.getText();
                switch (type) {
                    case CHILD:
                        // This is the text content of the actual key
                        keyBuilder.append(text).append(" ");
                        break;
                    case VALUE:
                        // Points to the blocks containing value content
                        valueBuilder.append(text).append(" ");
                        break;
                    default:
                }
            }
        }

        return BoundedKeyValue.builder()
                .keyValuePair(keyBuilder.toString(), valueBuilder.toString())
                .confidence(block.getConfidence())
                .box(box)
                .polygon(polygon)
                .build();
    }

    private static Cell processTableCell(Block block, Map<String, Block> blockMap) {
        RectF box = fromBoundingBox(block.getGeometry().getBoundingBox());
        Polygon polygon = fromPoints(block.getGeometry().getPolygon());
        StringBuilder wordsBuilder = new StringBuilder();

        // Each CELL block consists of WORD blocks
        for (Relationship relationship : block.getRelationships()) {
            for (String wordId : relationship.getIds()) {
                Block wordBlock = blockMap.get(wordId);
                if (wordBlock == null) {
                    continue;
                }
                String text = wordBlock.getText();
                wordsBuilder.append(text).append(" ");
            }
        }

        return Cell.builder()
                .text(wordsBuilder.toString())
                .confidence(block.getConfidence())
                .box(box)
                .polygon(polygon)
                .row(block.getRowIndex() - 1)
                .column(block.getColumnIndex() - 1)
                .build();
    }
}
