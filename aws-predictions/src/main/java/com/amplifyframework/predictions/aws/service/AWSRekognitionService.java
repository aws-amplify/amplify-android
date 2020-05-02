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
import com.amplifyframework.predictions.aws.adapter.EmotionTypeAdapter;
import com.amplifyframework.predictions.aws.adapter.GenderBinaryTypeAdapter;
import com.amplifyframework.predictions.aws.adapter.RekognitionResultTransformers;
import com.amplifyframework.predictions.aws.configuration.IdentifyEntitiesConfiguration;
import com.amplifyframework.predictions.models.AgeRange;
import com.amplifyframework.predictions.models.BinaryFeature;
import com.amplifyframework.predictions.models.Celebrity;
import com.amplifyframework.predictions.models.CelebrityDetails;
import com.amplifyframework.predictions.models.Emotion;
import com.amplifyframework.predictions.models.EmotionType;
import com.amplifyframework.predictions.models.EntityDetails;
import com.amplifyframework.predictions.models.EntityMatch;
import com.amplifyframework.predictions.models.Gender;
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Label;
import com.amplifyframework.predictions.models.LabelType;
import com.amplifyframework.predictions.models.Landmark;
import com.amplifyframework.predictions.models.Pose;
import com.amplifyframework.predictions.result.IdentifyCelebritiesResult;
import com.amplifyframework.predictions.result.IdentifyEntitiesResult;
import com.amplifyframework.predictions.result.IdentifyEntityMatchesResult;
import com.amplifyframework.predictions.result.IdentifyLabelsResult;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.IdentifyTextResult;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.DetectModerationLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectModerationLabelsResult;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.Face;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.ModerationLabel;
import com.amazonaws.services.rekognition.model.Parent;
import com.amazonaws.services.rekognition.model.RecognizeCelebritiesRequest;
import com.amazonaws.services.rekognition.model.RecognizeCelebritiesResult;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.rekognition.model.TextTypes;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Predictions service for performing image analysis.
 */
final class AWSRekognitionService {
    private final AmazonRekognitionClient rekognition;
    private final AWSPredictionsPluginConfiguration pluginConfiguration;

    AWSRekognitionService(@NonNull AWSPredictionsPluginConfiguration pluginConfiguration) {
        this.rekognition = createRekognitionClient();
        this.pluginConfiguration = pluginConfiguration;
    }

    private AmazonRekognitionClient createRekognitionClient() {
        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance();
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setUserAgent(UserAgent.string());
        return new AmazonRekognitionClient(credentialsProvider, configuration);
    }

    void detectLabels(
            @NonNull LabelType type,
            @NonNull ByteBuffer imageData,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        try {
            List<Label> labels = new ArrayList<>();
            boolean unsafeContent = false;
            // Moderation labels detection
            if (LabelType.ALL.equals(type) || LabelType.MODERATION_LABELS.equals(type)) {
                labels.addAll(detectModerationLabels(imageData));
                unsafeContent = !labels.isEmpty();
            }
            // Regular labels detection
            if (LabelType.ALL.equals(type) || LabelType.LABELS.equals(type)) {
                labels.addAll(detectLabels(imageData));
            }
            onSuccess.accept(IdentifyLabelsResult.builder()
                    .labels(labels)
                    .unsafeContent(unsafeContent)
                    .build());
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    void recognizeCelebrities(
            @NonNull ByteBuffer imageData,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        try {
            List<CelebrityDetails> celebrities = detectCelebrities(imageData);
            onSuccess.accept(IdentifyCelebritiesResult.fromCelebrities(celebrities));
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    void detectEntities(
            @NonNull ByteBuffer imageData,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        final IdentifyEntitiesConfiguration config;
        try {
            config = pluginConfiguration.getIdentifyEntitiesConfiguration();
            if (config.isGeneralEntityDetection()) {
                List<EntityDetails> entities = detectEntities(imageData);
                onSuccess.accept(IdentifyEntitiesResult.fromEntityDetails(entities));
            } else {
                int maxEntities = config.getMaxEntities();
                String collectionId = config.getCollectionId();
                List<EntityMatch> matches = detectEntityMatches(imageData, maxEntities, collectionId);
                onSuccess.accept(IdentifyEntityMatchesResult.fromEntityMatches(matches));
            }
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    void detectPlainText(
            @NonNull ByteBuffer imageData,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        try {
            onSuccess.accept(detectPlainText(imageData));
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    private List<Label> detectLabels(ByteBuffer imageData) throws PredictionsException {
        DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(new Image().withBytes(imageData));

        // Detect labels in the given image via Amazon Rekognition
        final DetectLabelsResult result;
        try {
            result = rekognition.detectLabels(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "Amazon Rekognition encountered an error while detecting labels.",
                    serviceException, "See attached service exception for more details."
            );
        }

        List<Label> labels = new ArrayList<>();
        for (com.amazonaws.services.rekognition.model.Label rekognitionLabel : result.getLabels()) {
            List<String> parents = new ArrayList<>();
            for (Parent parent : rekognitionLabel.getParents()) {
                parents.add(parent.getName());
            }
            Label amplifyLabel = Label.builder()
                    .value(rekognitionLabel.getName())
                    .confidence(rekognitionLabel.getConfidence())
                    .parentLabels(parents)
                    .build();
            labels.add(amplifyLabel);
        }

        return labels;
    }

    private List<Label> detectModerationLabels(ByteBuffer imageData) throws PredictionsException {
        DetectModerationLabelsRequest request = new DetectModerationLabelsRequest()
                .withImage(new Image().withBytes(imageData));

        // Detect moderation labels in the given image via Amazon Rekognition
        final DetectModerationLabelsResult result;
        try {
            result = rekognition.detectModerationLabels(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "Amazon Rekognition encountered an error while detecting moderation labels.",
                    serviceException, "See attached service exception for more details."
            );
        }

        List<Label> labels = new ArrayList<>();
        for (ModerationLabel moderationLabel : result.getModerationLabels()) {
            Label label = Label.builder()
                    .value(moderationLabel.getName())
                    .confidence(moderationLabel.getConfidence())
                    .parentLabels(Collections.singletonList(moderationLabel.getParentName()))
                    .build();
            labels.add(label);
        }

        return labels;
    }

    private List<CelebrityDetails> detectCelebrities(ByteBuffer imageData) throws PredictionsException {
        RecognizeCelebritiesRequest request = new RecognizeCelebritiesRequest()
                .withImage(new Image().withBytes(imageData));

        // Recognize celebrities in the given image via Amazon Rekognition
        final RecognizeCelebritiesResult result;
        try {
            result = rekognition.recognizeCelebrities(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "Amazon Rekognition encountered an error while recognizing celebrities.",
                    serviceException, "See attached service exception for more details."
            );
        }

        List<CelebrityDetails> celebrities = new ArrayList<>();
        for (com.amazonaws.services.rekognition.model.Celebrity rekognitionCelebrity : result.getCelebrityFaces()) {
            Celebrity amplifyCelebrity = Celebrity.builder()
                    .id(rekognitionCelebrity.getId())
                    .value(rekognitionCelebrity.getName())
                    .confidence(rekognitionCelebrity.getMatchConfidence())
                    .build();

            // Get face-specific celebrity details from the result
            ComparedFace face = rekognitionCelebrity.getFace();
            RectF box = RekognitionResultTransformers.fromBoundingBox(face.getBoundingBox());
            Pose pose = RekognitionResultTransformers.fromRekognitionPose(face.getPose());
            List<Landmark> landmarks = RekognitionResultTransformers.fromLandmarks(face.getLandmarks());

            // Get URL links that are relevant to celebrities
            List<URL> urls = new ArrayList<>();
            for (String url : rekognitionCelebrity.getUrls()) {
                try {
                    urls.add(new URL(url));
                } catch (MalformedURLException badUrl) {
                    // Ignore bad URL
                }
            }

            CelebrityDetails details = CelebrityDetails.builder()
                    .celebrity(amplifyCelebrity)
                    .box(box)
                    .pose(pose)
                    .landmarks(landmarks)
                    .urls(urls)
                    .build();
            celebrities.add(details);
        }

        return celebrities;
    }

    private List<EntityDetails> detectEntities(ByteBuffer imageData) throws PredictionsException {
        DetectFacesRequest request = new DetectFacesRequest()
                .withImage(new Image().withBytes(imageData))
                .withAttributes(Attribute.ALL.toString());

        // Detect entities in the given image via Amazon Rekognition
        final DetectFacesResult result;
        try {
            result = rekognition.detectFaces(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "Amazon Rekognition encountered an error while detecting faces.",
                    serviceException, "See attached service exception for more details."
            );
        }

        List<EntityDetails> entities = new ArrayList<>();
        for (FaceDetail face : result.getFaceDetails()) {
            // Extract details from face detection
            RectF box = RekognitionResultTransformers.fromBoundingBox(face.getBoundingBox());
            AgeRange ageRange = RekognitionResultTransformers.fromRekognitionAgeRange(face.getAgeRange());
            Pose pose = RekognitionResultTransformers.fromRekognitionPose(face.getPose());
            List<Landmark> landmarks = RekognitionResultTransformers.fromLandmarks(face.getLandmarks());
            List<BinaryFeature> features = RekognitionResultTransformers.fromFaceDetail(face);

            // Gender detection
            com.amazonaws.services.rekognition.model.Gender rekognitionGender = face.getGender();
            Gender amplifyGender = Gender.builder()
                    .value(GenderBinaryTypeAdapter.fromRekognition(rekognitionGender.getValue()))
                    .confidence(rekognitionGender.getConfidence())
                    .build();

            // Emotion detection
            List<Emotion> emotions = new ArrayList<>();
            for (com.amazonaws.services.rekognition.model.Emotion rekognitionEmotion : face.getEmotions()) {
                EmotionType emotion = EmotionTypeAdapter.fromRekognition(rekognitionEmotion.getType());
                Emotion amplifyEmotion = Emotion.builder()
                        .value(emotion)
                        .confidence(rekognitionEmotion.getConfidence())
                        .build();
                emotions.add(amplifyEmotion);
            }
            Collections.sort(emotions, Collections.reverseOrder());

            EntityDetails entity = EntityDetails.builder()
                    .box(box)
                    .ageRange(ageRange)
                    .pose(pose)
                    .gender(amplifyGender)
                    .landmarks(landmarks)
                    .emotions(emotions)
                    .features(features)
                    .build();
            entities.add(entity);
        }
        return entities;
    }

    private List<EntityMatch> detectEntityMatches(
            ByteBuffer imageData,
            int maxEntities,
            String collectionId
    ) throws PredictionsException {
        SearchFacesByImageRequest request = new SearchFacesByImageRequest()
                .withImage(new Image().withBytes(imageData))
                .withMaxFaces(maxEntities)
                .withCollectionId(collectionId);

        // Detect entities in the given image by matching against a collection of images
        final SearchFacesByImageResult result;
        try {
            result = rekognition.searchFacesByImage(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "Amazon Rekognition encountered an error while searching for known faces.",
                    serviceException, "See attached service exception for more details."
            );
        }

        List<EntityMatch> matches = new ArrayList<>();
        for (FaceMatch rekognitionMatch : result.getFaceMatches()) {
            Face face = rekognitionMatch.getFace();
            RectF box = RekognitionResultTransformers.fromBoundingBox(face.getBoundingBox());
            EntityMatch amplifyMatch = EntityMatch.builder()
                    .externalImageId(face.getExternalImageId())
                    .confidence(rekognitionMatch.getSimilarity())
                    .box(box)
                    .build();
            matches.add(amplifyMatch);
        }
        return matches;
    }

    private IdentifyTextResult detectPlainText(ByteBuffer imageData) throws PredictionsException {
        DetectTextRequest request = new DetectTextRequest()
                .withImage(new Image().withBytes(imageData));

        // Read text in the given image via Amazon Rekognition
        final DetectTextResult result;
        try {
            result = rekognition.detectText(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "Amazon Rekognition encountered an error while detecting text.",
                    serviceException, "See attached service exception for more details."
            );
        }

        StringBuilder fullTextBuilder = new StringBuilder();
        List<String> rawLineText = new ArrayList<>();
        List<IdentifiedText> words = new ArrayList<>();
        List<IdentifiedText> lines = new ArrayList<>();

        for (TextDetection detection : result.getTextDetections()) {
            TextTypes type = TextTypes.fromValue(detection.getType());
            switch (type) {
                case LINE:
                    rawLineText.add(detection.getDetectedText());
                    lines.add(RekognitionResultTransformers.fromTextDetection(detection));
                    continue;
                case WORD:
                    fullTextBuilder.append(detection.getDetectedText()).append(" ");
                    words.add(RekognitionResultTransformers.fromTextDetection(detection));
                    continue;
                default:
            }
        }

        return IdentifyTextResult.builder()
                .fullText(fullTextBuilder.toString())
                .rawLineText(rawLineText)
                .lines(lines)
                .words(words)
                .build();
    }

    @NonNull
    AmazonRekognitionClient getClient() {
        return rekognition;
    }
}
