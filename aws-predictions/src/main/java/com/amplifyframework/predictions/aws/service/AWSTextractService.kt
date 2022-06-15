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
package com.amplifyframework.predictions.aws.service

import com.amazonaws.services.textract.AmazonTextractClient
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.models.Selection
import com.amplifyframework.predictions.models.Table
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.HashMap

/**
 * Predictions service for performing text translation.
 */
internal class AWSTextractService(
    pluginConfiguration: AWSPredictionsPluginConfiguration,
    credentialsProvider: AWSCredentialsProvider
) {
    private val textract: AmazonTextractClient
    private val pluginConfiguration: AWSPredictionsPluginConfiguration
    private fun createTextractClient(credentialsProvider: AWSCredentialsProvider): AmazonTextractClient {
        val configuration = ClientConfiguration()
        configuration.setUserAgent(UserAgent.string())
        return AmazonTextractClient(credentialsProvider, configuration)
    }

    fun detectDocumentText(
        type: TextFormatType,
        imageData: ByteBuffer,
        onSuccess: Consumer<IdentifyResult?>,
        onError: Consumer<PredictionsException?>
    ) {
        val features: MutableList<String> = ArrayList()
        if (TextFormatType.FORM == type || TextFormatType.ALL == type) {
            features.add(FeatureType.FORMS.toString())
        }
        if (TextFormatType.TABLE == type || TextFormatType.ALL == type) {
            features.add(FeatureType.TABLES.toString())
        }
        try {
            onSuccess.accept(analyzeDocument(imageData, features))
        } catch (exception: PredictionsException) {
            onError.accept(exception)
        }
    }

    @Throws(PredictionsException::class)
    private fun detectDocumentText(imageData: ByteBuffer): IdentifyDocumentTextResult {
        val request: DetectDocumentTextRequest = DetectDocumentTextRequest()
            .withDocument(Document().withBytes(imageData))

        // Extract text from given image via Amazon Textract
        val result: DetectDocumentTextResult
        result = try {
            textract.detectDocumentText(request)
        } catch (serviceException: AmazonClientException) {
            throw PredictionsException(
                "AWS Textract encountered an error while detecting document text.",
                serviceException, "See attached service exception for more details."
            )
        }
        return processTextractBlocks(result.getBlocks())
    }

    @Throws(PredictionsException::class)
    private fun analyzeDocument(
        imageData: ByteBuffer,
        features: List<String>
    ): IdentifyDocumentTextResult {
        val request: AnalyzeDocumentRequest = AnalyzeDocumentRequest()
            .withDocument(Document().withBytes(imageData))
            .withFeatureTypes(features)

        // Analyze document from given image via Amazon Textract
        val result: AnalyzeDocumentResult
        result = try {
            textract.analyzeDocument(request)
        } catch (serviceException: AmazonClientException) {
            throw PredictionsException(
                "AWS Textract encountered an error while analyzing document.",
                serviceException, "See attached service exception for more details."
            )
        }
        return processTextractBlocks(result.getBlocks())
    }

    private fun processTextractBlocks(blocks: List<Block>): IdentifyDocumentTextResult {
        val fullTextBuilder = StringBuilder()
        val rawLineText: MutableList<String> = ArrayList()
        val words: MutableList<IdentifiedText> = ArrayList<IdentifiedText>()
        val lines: MutableList<IdentifiedText> = ArrayList<IdentifiedText>()
        val selections: MutableList<Selection> = ArrayList()
        val tables: MutableList<Table> = ArrayList()
        val keyValues: MutableList<BoundedKeyValue> = ArrayList<BoundedKeyValue>()
        val tableBlocks: MutableList<Block> = ArrayList<Block>()
        val keyValueBlocks: MutableList<Block> = ArrayList<Block>()
        val blockMap: MutableMap<String, Block> = HashMap<String, Block>()
        for (block in blocks) {
            // This is the map that will be used for traversing the graph.
            // Each block can contain "relationships", which point to other blocks by ID.
            val id: String = block.getId()
            blockMap[id] = block
            val type: BlockType = BlockType.fromValue(block.getBlockType())
            when (type) {
                LINE -> {
                    rawLineText.add(block.getText())
                    lines.add(TextractResultTransformers.fetchIdentifiedText(block))
                    continue
                }
                WORD -> {
                    fullTextBuilder.append(block.getText()).append(" ")
                    words.add(TextractResultTransformers.fetchIdentifiedText(block))
                    continue
                }
                SELECTION_ELEMENT -> {
                    selections.add(TextractResultTransformers.fetchSelection(block))
                    continue
                }
                TABLE -> {
                    tableBlocks.add(block)
                    continue
                }
                KEY_VALUE_SET -> {
                    keyValueBlocks.add(block)
                    continue
                }
                else -> {
                }
            }
        }
        for (tableBlock in tableBlocks) {
            val table: Table = TextractResultTransformers.fetchTable(tableBlock, blockMap)
            if (table != null) {
                tables.add(table)
            }
        }
        for (keyValueBlock in keyValueBlocks) {
            val keyValue: BoundedKeyValue =
                TextractResultTransformers.fetchKeyValue(keyValueBlock, blockMap)
            if (keyValue != null) {
                keyValues.add(keyValue)
            }
        }
        return IdentifyDocumentTextResult.builder()
            .fullText(fullTextBuilder.toString().trim { it <= ' ' })
            .rawLineText(rawLineText)
            .lines(lines)
            .words(words)
            .selections(selections)
            .tables(tables)
            .keyValues(keyValues)
            .build()
    }

    val client: AmazonTextractClient
        get() = textract

    init {
        textract = createTextractClient(credentialsProvider)
        this.pluginConfiguration = pluginConfiguration
    }
}