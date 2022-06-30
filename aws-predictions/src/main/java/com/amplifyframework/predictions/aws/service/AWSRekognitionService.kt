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

import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration
import aws.sdk.kotlin.services.rekognition.RekognitionClient
import com.amplifyframework.predictions.result.IdentifyResult
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.result.IdentifyLabelsResult
import com.amplifyframework.predictions.result.IdentifyCelebritiesResult
import com.amplifyframework.predictions.result.IdentifyEntitiesResult
import com.amplifyframework.predictions.result.IdentifyEntityMatchesResult
import android.graphics.RectF
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.sdk.kotlin.services.rekognition.model.*
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.aws.adapter.RekognitionResultTransformers
import com.amplifyframework.predictions.aws.adapter.GenderBinaryTypeAdapter
import com.amplifyframework.predictions.aws.adapter.EmotionTypeAdapter
import com.amplifyframework.predictions.models.*
import com.amplifyframework.predictions.models.Celebrity
import com.amplifyframework.predictions.models.Emotion
import com.amplifyframework.predictions.models.Gender
import com.amplifyframework.predictions.models.Label
import com.amplifyframework.predictions.result.IdentifyTextResult
import kotlinx.coroutines.runBlocking
import java.lang.StringBuilder
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors

/**
 * Predictions service for performing image analysis.
 */
internal class AWSRekognitionService(
    private val pluginConfiguration: AWSPredictionsPluginConfiguration,
    private val authCredentialsProvider: CredentialsProvider
) {
    
    val client: RekognitionClient = RekognitionClient {
        this.region = pluginConfiguration.defaultRegion
        this.credentialsProvider = authCredentialsProvider
    }

    private val executor = Executors.newCachedThreadPool()

    fun detectLabels(
        type: LabelType,
        imageData: ByteBuffer,
        onSuccess: Consumer<IdentifyResult>,
        onError: Consumer<PredictionsException>
    ) {
        execute(
            {
                val labels: MutableList<Label> = ArrayList()
                var unsafeContent = false
                // Moderation labels detection
                if (LabelType.ALL == type || LabelType.MODERATION_LABELS == type) {
                    labels.addAll(detectModerationLabels(imageData))
                    unsafeContent = labels.isNotEmpty()
                }
                // Regular labels detection
                if (LabelType.ALL == type || LabelType.LABELS == type) {
                    labels.addAll(detectLabels(imageData))
                }
                IdentifyLabelsResult.builder()
                    .labels(labels)
                    .unsafeContent(unsafeContent)
                    .build()
            },
            { throwable ->
                PredictionsException(
                    "Amazon Rekognition encountered an error while detecting labels.",
                    throwable, "See attached service exception for more details."
                )
            },
            onSuccess,
            onError
        )
    }

    fun recognizeCelebrities(
        imageData: ByteBuffer,
        onSuccess: Consumer<IdentifyResult>,
        onError: Consumer<PredictionsException>
    ) {
        val config = pluginConfiguration.identifyEntitiesConfiguration
        if (!config.isCelebrityDetectionEnabled) {
            onError.accept(
                PredictionsException(
                    "Celebrity detection is disabled.",
                    "Please enable celebrity detection via Amplify CLI. This feature should be accessible by " +
                            "running `amplify update predictions` in the console and updating entities " +
                            "detection resource with advanced configuration setting."
                )
            )
            return
        }
        
        execute(
            {
                val celebrities = detectCelebrities(imageData)
                IdentifyCelebritiesResult.fromCelebrities(celebrities)
            },
            { throwable ->
                PredictionsException(
                    "Amazon Rekognition encountered an error while recognizing celebrities.",
                    throwable, "See attached service exception for more details."
                )
            },
            onSuccess,
            onError
        )
    }

    fun detectEntities(
        imageData: ByteBuffer,
        onSuccess: Consumer<IdentifyResult>,
        onError: Consumer<PredictionsException>
    ) {
        execute(
            {
                val config = pluginConfiguration.identifyEntitiesConfiguration
                if (config.isGeneralEntityDetection) {
                    val entities = detectEntities(imageData)
                    IdentifyEntitiesResult.fromEntityDetails(entities)
                } else {
                    val maxEntities = config.maxEntities
                    val collectionId = config.collectionId
                    val matches = detectEntityMatches(imageData, maxEntities, collectionId)
                    IdentifyEntityMatchesResult.fromEntityMatches(matches)
                }
            },
            { throwable ->
                PredictionsException(
                    "Amazon Rekognition encountered an error while either detecting faces or searching for known faces.",
                    throwable, "See attached service exception for more details."
                )
            },
            onSuccess,
            onError
        )
    }

    fun detectPlainText(
        imageData: ByteBuffer,
        onSuccess: Consumer<IdentifyResult>,
        onError: Consumer<PredictionsException>
    ) {
        execute(
            {
                detectPlainText(imageData)
            },
            { throwable ->
                PredictionsException(
                    "Amazon Rekognition encountered an error while detecting text.",
                    throwable, "See attached service exception for more details."
                )
            },
            onSuccess,
            onError
        )
    }
    
    private suspend fun detectLabels(imageData: ByteBuffer): List<Label> {
        // Detect labels in the given image via Amazon Rekognition
        val result = client.detectLabels { 
            this.image = Image {
                this.bytes = imageData.array()
            }
        }
        val labels: MutableList<Label> = ArrayList()
        result.labels?.forEach { rekognitionLabel ->
            val parents: MutableList<String> = ArrayList()
            rekognitionLabel.parents?.forEach { parent ->
                parent.name?.let { parentName -> parents.add(parentName) }
            }
            val boxes: MutableList<RectF?> = ArrayList()
            rekognitionLabel.instances?.forEach { instance ->
                boxes.add(RekognitionResultTransformers.fromBoundingBox(instance.boundingBox))
            }
            rekognitionLabel.name?.let { labelName ->
                rekognitionLabel.confidence?.let { labelConfidence ->
                    val amplifyLabel = Label.builder()
                        .value(labelName)
                        .confidence(labelConfidence)
                        .parentLabels(parents)
                        .boxes(boxes)
                        .build()
                    labels.add(amplifyLabel)
                }
            }
        }
        return labels
    }
    
    private suspend fun detectModerationLabels(imageData: ByteBuffer): List<Label> {
        val result = client.detectModerationLabels {
            this.image = Image {
                this.bytes = imageData.array()
            }
        }
        val labels: MutableList<Label> = ArrayList()
        result.moderationLabels?.forEach { moderationLabel ->
            moderationLabel.name?.let { labelName ->
                moderationLabel.confidence?.let { labelConfidence ->
                    val label = Label.builder()
                        .value(labelName)
                        .confidence(labelConfidence)
                        .parentLabels(listOf(moderationLabel.parentName))
                        .build()
                    labels.add(label)
                }
            }
        }
        return labels
    }
    
    private suspend fun detectCelebrities(imageData: ByteBuffer): List<CelebrityDetails> {
        val result = client.recognizeCelebrities {
            this.image = Image {
                this.bytes = imageData.array()
            }
        }
        val celebrities: MutableList<CelebrityDetails> = ArrayList()
        result.celebrityFaces?.forEach { rekognitionCelebrity ->
            val amplifyCelebrity = rekognitionCelebrity.id?.let { celebrityId ->
                rekognitionCelebrity.name?.let { celebrityName ->
                    rekognitionCelebrity.matchConfidence?.let { celebrityMatchConfidence ->
                        Celebrity.builder()
                            .id(celebrityId)
                            .value(celebrityName)
                            .confidence(celebrityMatchConfidence)
                            .build()
                    }
                }
            }
            // Get face-specific celebrity details from the result
            val face = rekognitionCelebrity.face
            val box = RekognitionResultTransformers.fromBoundingBox(face?.boundingBox)
            val pose = RekognitionResultTransformers.fromRekognitionPose(face?.pose)
            val landmarks = RekognitionResultTransformers.fromLandmarks(face?.landmarks)

            // Get URL links that are relevant to celebrities
            val urls: MutableList<URL> = ArrayList()
            rekognitionCelebrity.urls?.forEach { url ->
                try {
                    urls.add(URL(url))
                } catch (badUrl: MalformedURLException) {
                    // Ignore bad URL
                }
            }
            amplifyCelebrity?.let {
                val details = CelebrityDetails.builder()
                    .celebrity(it)
                    .box(box)
                    .pose(pose)
                    .landmarks(landmarks)
                    .urls(urls)
                    .build()
                celebrities.add(details)
            }
        }
        return celebrities
    }
    
    private suspend fun detectEntities(imageData: ByteBuffer): List<EntityDetails> {
        val result = client.detectFaces { 
            this.image = Image {
                this.bytes = imageData.array()
            }
            this.attributes = mutableListOf(Attribute.All)
        }
        val entities: MutableList<EntityDetails> = ArrayList()
        result.faceDetails?.forEach { face ->
            // Extract details from face detection
            val box = RekognitionResultTransformers.fromBoundingBox(face.boundingBox)
            val ageRange = RekognitionResultTransformers.fromRekognitionAgeRange(face.ageRange)
            val pose = RekognitionResultTransformers.fromRekognitionPose(face.pose)
            val landmarks = RekognitionResultTransformers.fromLandmarks(face.landmarks)
            val features = RekognitionResultTransformers.fromFaceDetail(face)

            // Gender detection
            val amplifyGender = face.gender?.let { faceGender ->
                faceGender.confidence?.let { faceGenderConfidence ->
                    Gender.builder()
                        .value(GenderBinaryTypeAdapter.fromRekognition(faceGender.value.toString()))
                        .confidence(faceGenderConfidence)
                        .build()
                }
            }

            // Emotion detection
            val emotions: MutableList<Emotion> = ArrayList()
            face.emotions?.forEach { rekognitionEmotion ->
                val emotion = EmotionTypeAdapter.fromRekognition(rekognitionEmotion.type.toString())
                rekognitionEmotion.confidence?.let { emotionConfidence ->
                    val amplifyEmotion = Emotion.builder()
                        .value(emotion)
                        .confidence(emotionConfidence)
                        .build()
                    emotions.add(amplifyEmotion)
                }
            }
            Collections.sort(emotions, Collections.reverseOrder())
            val entity = EntityDetails.builder()
                .box(box)
                .ageRange(ageRange)
                .pose(pose)
                .gender(amplifyGender)
                .landmarks(landmarks)
                .emotions(emotions)
                .features(features)
                .build()
            entities.add(entity)
        }
        return entities
    }
    
    private suspend fun detectEntityMatches(
        imageData: ByteBuffer,
        maxEntities: Int,
        collectionId: String
    ): List<EntityMatch> {
        
        val result = client.searchFacesByImage {
            this.image = Image {
                this.bytes = imageData.array()
            }
            this.maxFaces = maxEntities
            this.collectionId = collectionId
        }
        val matches: MutableList<EntityMatch> = ArrayList()
        result.faceMatches?.forEach { rekognitionMatch ->
            val box = RekognitionResultTransformers.fromBoundingBox(rekognitionMatch.face?.boundingBox)
            rekognitionMatch.face?.externalImageId?.let { faceImageId ->
                rekognitionMatch.similarity?.let { matchSimilarity ->
                    val amplifyMatch = EntityMatch.builder()
                        .externalImageId(faceImageId)
                        .confidence(matchSimilarity)
                        .box(box)
                        .build()
                    matches.add(amplifyMatch)
                }
            }
        }
        return matches
    }
    
    private suspend fun detectPlainText(imageData: ByteBuffer): IdentifyTextResult {
        val result = client.detectText {
            this.image = Image {
                this.bytes = imageData.array()
            }
        }
        val fullTextBuilder = StringBuilder()
        val rawLineText: MutableList<String> = ArrayList()
        val words: MutableList<IdentifiedText?> = ArrayList()
        val lines: MutableList<IdentifiedText?> = ArrayList()
        result.textDetections?.forEach { detection ->
            when (TextTypes.fromValue(detection.type.toString())) {
                TextTypes.Line -> {
                    detection.detectedText?.let { rawLineText.add(it) }
                    lines.add(RekognitionResultTransformers.fromTextDetection(detection))
                }
                TextTypes.Word -> {
                    fullTextBuilder.append(detection.detectedText).append(" ")
                    words.add(RekognitionResultTransformers.fromTextDetection(detection))
                }
                else -> { }
            }
        }
        return IdentifyTextResult.builder()
            .fullText(fullTextBuilder.toString().trim { it <= ' ' })
            .rawLineText(rawLineText)
            .lines(lines)
            .words(words)
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