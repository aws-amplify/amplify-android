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
package com.amplifyframework.predictions.aws.service

import aws.sdk.kotlin.services.textract.TextractClient
import aws.sdk.kotlin.services.textract.model.Block
import aws.sdk.kotlin.services.textract.model.BlockType
import aws.sdk.kotlin.services.textract.model.Document
import aws.sdk.kotlin.services.textract.model.FeatureType
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration
import com.amplifyframework.predictions.aws.adapter.TextractResultTransformers
import com.amplifyframework.predictions.models.BoundedKeyValue
import com.amplifyframework.predictions.models.IdentifiedText
import com.amplifyframework.predictions.models.Selection
import com.amplifyframework.predictions.models.Table
import com.amplifyframework.predictions.models.TextFormatType
import com.amplifyframework.predictions.result.IdentifyDocumentTextResult
import com.amplifyframework.predictions.result.IdentifyResult
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/**
 * Predictions service for performing text translation.
 */
internal class AWSTextractService(
    private val pluginConfiguration: AWSPredictionsPluginConfiguration,
    private val authCredentialsProvider: CredentialsProvider
) {
    val client: TextractClient = TextractClient {
        this.region = pluginConfiguration.defaultRegion
        this.credentialsProvider = authCredentialsProvider
    }

    private val executor = Executors.newCachedThreadPool()

    fun detectDocumentText(
        type: TextFormatType,
        imageData: ByteBuffer,
        onSuccess: Consumer<IdentifyResult>,
        onError: Consumer<PredictionsException>
    ) {
        execute(
            {
                val features: MutableList<FeatureType> = ArrayList()
                if (TextFormatType.FORM == type || TextFormatType.ALL == type) {
                    features.add(FeatureType.Forms)
                }
                if (TextFormatType.TABLE == type || TextFormatType.ALL == type) {
                    features.add(FeatureType.Tables)
                }
                // Analyze document from given image via Amazon Textract
                val result = client.analyzeDocument {
                    this.document = Document {
                        this.bytes = imageData.array()
                    }
                    this.featureTypes = features
                }
                processTextractBlocks(result.blocks ?: emptyList())
            },
            { throwable ->
                PredictionsException(
                    "AWS Textract encountered an error while analyzing document.",
                    throwable,
                    "See attached exception for more details."
                )
            },
            onSuccess,
            onError
        )
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
            val id: String = block.id ?: ""
            blockMap[id] = block
            block.blockType?.let { blockType ->
                when (blockType) {
                    BlockType.Line -> {
                        block.text?.let { blockText -> rawLineText.add(blockText) }
                        TextractResultTransformers.fetchIdentifiedText(block)?.let { identifiedText ->
                            lines.add(
                                identifiedText
                            )
                        }
                    }
                    BlockType.Word -> {
                        fullTextBuilder.append(block.text).append(" ")
                        TextractResultTransformers.fetchIdentifiedText(block)?.let { identifiedText ->
                            words.add(
                                identifiedText
                            )
                        }
                    }
                    BlockType.SelectionElement -> {
                        TextractResultTransformers.fetchSelection(block)?.let { selection ->
                            selections.add(selection)
                        }
                    }
                    BlockType.Table -> {
                        tableBlocks.add(block)
                    }
                    BlockType.KeyValueSet -> {
                        keyValueBlocks.add(block)
                    }
                    else -> { }
                }
            }
        }
        for (tableBlock in tableBlocks) {
            TextractResultTransformers.fetchTable(tableBlock, blockMap)?.let { table -> tables.add(table) }
        }
        for (keyValueBlock in keyValueBlocks) {
            TextractResultTransformers.fetchKeyValue(keyValueBlock, blockMap)?.let { keyValue ->
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

    private fun <T : Any> execute(
        runnableTask: suspend () -> T,
        errorTransformer: (Throwable) -> PredictionsException,
        onResult: Consumer<T>,
        onError: Consumer<PredictionsException>
    ) {
        executor.execute {
            try {
                runBlocking {
                    val result = runnableTask()
                    onResult.accept(result)
                }
            } catch (error: Throwable) {
                val predictionsException = errorTransformer.invoke(error)
                onError.accept(predictionsException)
            }
        }
    }
}
