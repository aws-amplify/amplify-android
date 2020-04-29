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

package com.amplifyframework.predictions.aws.service;

import android.graphics.RectF;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration;
import com.amplifyframework.predictions.aws.adapter.IdentifyTextResultTransformers;
import com.amplifyframework.predictions.models.BoundedKeyValue;
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Selection;
import com.amplifyframework.predictions.models.Table;
import com.amplifyframework.predictions.models.TextFormatType;
import com.amplifyframework.predictions.result.IdentifyDocumentTextResult;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.textract.AmazonTextractClient;
import com.amazonaws.services.textract.model.AnalyzeDocumentRequest;
import com.amazonaws.services.textract.model.AnalyzeDocumentResult;
import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.BlockType;
import com.amazonaws.services.textract.model.DetectDocumentTextRequest;
import com.amazonaws.services.textract.model.DetectDocumentTextResult;
import com.amazonaws.services.textract.model.Document;
import com.amazonaws.services.textract.model.FeatureType;
import com.amazonaws.services.textract.model.Geometry;
import com.amazonaws.services.textract.model.SelectionStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Predictions service for performing text translation.
 */
final class AWSTextractService {
    private final AmazonTextractClient textract;
    private final AWSPredictionsPluginConfiguration pluginConfiguration;

    AWSTextractService(@NonNull AWSPredictionsPluginConfiguration pluginConfiguration) {
        this.textract = createTextractClient();
        this.pluginConfiguration = pluginConfiguration;
    }

    private AmazonTextractClient createTextractClient() {
        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance();
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setUserAgent(UserAgent.string());
        return new AmazonTextractClient(credentialsProvider, configuration);
    }

    void detectDocumentText(
            @NonNull TextFormatType type,
            @NonNull Document document,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        List<String> features = new ArrayList<>();
        if (TextFormatType.FORM.equals(type) || TextFormatType.ALL.equals(type)) {
            features.add(FeatureType.FORMS.toString());
        }
        if (TextFormatType.TABLE.equals(type) || TextFormatType.ALL.equals(type)) {
            features.add(FeatureType.TABLES.toString());
        }

        try {
            onSuccess.accept(analyzeDocument(document, features));
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    private IdentifyDocumentTextResult detectDocumentText(Document document) throws PredictionsException {
        DetectDocumentTextRequest request = new DetectDocumentTextRequest()
                .withDocument(document);

        // Extract text from given image via Amazon Textract
        final DetectDocumentTextResult result;
        try {
            result = textract.detectDocumentText(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Textract encountered an error while detecting document text.",
                    serviceException, "See attached service exception for more details."
            );
        }

        return processTextractBlocks(result.getBlocks());
    }

    private IdentifyDocumentTextResult analyzeDocument(
            Document document,
            List<String> features
    ) throws PredictionsException {
        AnalyzeDocumentRequest request = new AnalyzeDocumentRequest()
                .withDocument(document)
                .withFeatureTypes(features);

        // Analyze document from given image via Amazon Textract
        final AnalyzeDocumentResult result;
        try {
            result = textract.analyzeDocument(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Textract encountered an error while analyzing document.",
                    serviceException, "See attached service exception for more details."
            );
        }

        return processTextractBlocks(result.getBlocks());
    }

    private IdentifyDocumentTextResult processTextractBlocks(List<Block> blocks) {
        StringBuilder fullTextBuilder = new StringBuilder();
        List<String> rawLineText = new ArrayList<>();
        List<IdentifiedText> words = new ArrayList<>();
        List<IdentifiedText> lines = new ArrayList<>();
        List<Selection> selections = new ArrayList<>();
        List<Table> tables = new ArrayList<>();
        List<BoundedKeyValue> keyValues = new ArrayList<>();
        List<Block> tableBlocks = new ArrayList<>();
        List<Block> keyValueBlocks = new ArrayList<>();
        Map<String, Block> blockMap = new HashMap<>();
        for (Block block : blocks) {
            // This is the map that will be used for traversing the graph.
            // Each block can contain "relationships", which point to other blocks by ID.
            String id = block.getId();
            blockMap.put(id, block);

            String detectedText = block.getText();
            Geometry geometry = block.getGeometry();
            RectF box = IdentifyTextResultTransformers.fromBoundingBox(geometry.getBoundingBox());
            Polygon polygon = IdentifyTextResultTransformers.fromPoints(geometry.getPolygon());
            int page = block.getPage() != null ? block.getPage() : 0;

            BlockType type = BlockType.fromValue(block.getBlockType());
            switch (type) {
                case LINE:
                    IdentifiedText line = IdentifiedText.builder()
                            .text(detectedText)
                            .confidence(block.getConfidence())
                            .box(box)
                            .polygon(polygon)
                            .page(page)
                            .build();
                    rawLineText.add(detectedText);
                    lines.add(line);
                    continue;
                case WORD:
                    IdentifiedText word = IdentifiedText.builder()
                            .text(detectedText)
                            .confidence(block.getConfidence())
                            .box(box)
                            .polygon(polygon)
                            .page(page)
                            .build();
                    fullTextBuilder.append(detectedText).append(" ");
                    words.add(word);
                    continue;
                case SELECTION_ELEMENT:
                    SelectionStatus status = SelectionStatus.fromValue(block.getSelectionStatus());
                    boolean isSelected = SelectionStatus.SELECTED.equals(status);
                    Selection selection = Selection.builder()
                            .box(box)
                            .polygon(polygon)
                            .selected(isSelected)
                            .build();
                    selections.add(selection);
                    continue;
                case TABLE:
                    tableBlocks.add(block);
                    continue;
                case KEY_VALUE_SET:
                    keyValueBlocks.add(block);
                    continue;
                default:
            }
        }

        for (Block tableBlock : tableBlocks) {
            Table table = IdentifyTextResultTransformers.processTable(tableBlock, blockMap);
            if (table != null) {
                tables.add(table);
            }
        }

        for (Block keyValueBlock : keyValueBlocks) {
            BoundedKeyValue keyValue = IdentifyTextResultTransformers.processKeyValue(keyValueBlock, blockMap);
            if (keyValue != null) {
                keyValues.add(keyValue);
            }
        }

        return IdentifyDocumentTextResult.builder()
                .fullText(fullTextBuilder.toString().trim())
                .rawLineText(rawLineText)
                .lines(lines)
                .words(words)
                .selections(selections)
                .tables(tables)
                .keyValues(keyValues)
                .build();
    }

    @NonNull
    AmazonTextractClient getClient() {
        return textract;
    }
}
