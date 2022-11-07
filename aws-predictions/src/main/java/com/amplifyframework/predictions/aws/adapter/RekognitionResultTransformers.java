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

import com.amplifyframework.predictions.aws.models.BinaryFeatureType;
import com.amplifyframework.predictions.models.AgeRange;
import com.amplifyframework.predictions.models.BinaryFeature;
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Landmark;
import com.amplifyframework.predictions.models.LandmarkType;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Pose;
import com.amplifyframework.util.Empty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aws.sdk.kotlin.services.rekognition.model.BoundingBox;
import aws.sdk.kotlin.services.rekognition.model.FaceDetail;
import aws.sdk.kotlin.services.rekognition.model.Point;
import aws.sdk.kotlin.services.rekognition.model.TextDetection;

/**
 * Utility class to transform Amazon Rekognition service-specific
 * models to be compatible with AWS Amplify.
 */
public final class RekognitionResultTransformers {
    private RekognitionResultTransformers() {}

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
     * Converts geometric polygon from Amazon Rekognition into
     * Amplify-compatible polygon object.
     * @param polygon the polygon object by Amazon Rekognition
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
     * Converts {@link aws.sdk.kotlin.services.rekognition.model.Pose}
     * from Amazon Rekognition into Amplify-compatible pose data.
     * @param pose the pose provided by Amazon Rekognition
     * @return the Amplify pose with same orientation
     */
    @Nullable
    public static Pose fromRekognitionPose(@Nullable aws.sdk.kotlin.services.rekognition.model.Pose pose) {
        if (pose == null) {
            return null;
        }
        return new Pose(pose.getPitch(), pose.getRoll(), pose.getYaw());
    }

    /**
     * Converts {@link aws.sdk.kotlin.services.rekognition.model.AgeRange}
     * from Amazon Rekognition into Amplify-compatible age range data.
     * @param range the age range provided by Amazon Rekognition
     * @return the Amplify age range with same low and high
     */
    @Nullable
    public static AgeRange fromRekognitionAgeRange(@Nullable aws.sdk.kotlin.services.rekognition.model.AgeRange range) {
        if (range == null) {
            return null;
        }
        return new AgeRange(range.getLow(), range.getHigh());
    }

    /**
     * Converts {@link TextDetection} from Amazon Rekognition into Amplify-
     * compatible text identification data.
     * @param text the detected text data
     * @return the Amplify feature with detected text
     */
    @Nullable
    public static IdentifiedText fromTextDetection(@Nullable TextDetection text) {
        if (text == null) {
            return null;
        }
        return IdentifiedText.builder()
                .text(text.getDetectedText())
                .confidence(text.getConfidence())
                .box(fromBoundingBox(text.getGeometry().getBoundingBox()))
                .polygon(fromPoints(text.getGeometry().getPolygon()))
                .build();
    }

    /**
     * Converts a list of {@link aws.sdk.kotlin.services.rekognition.model.Landmark}
     * from Amazon Rekognition into Amplify-compatible list of
     * {@link Landmark} objects.
     * @param landmarks the list of Amazon Rekognition landmark objects
     * @return the list of Amplify Predictions landmark objects
     */
    @NonNull
    public static List<Landmark> fromLandmarks(
            @Nullable List<aws.sdk.kotlin.services.rekognition.model.Landmark> landmarks
    ) {
        List<Landmark> amplifyLandmarks = new ArrayList<>();
        if (Empty.check(landmarks)) {
            return amplifyLandmarks;
        }

        List<PointF> allPoints = new ArrayList<>();
        Map<LandmarkType, List<PointF>> landmarkMap = new HashMap<>();

        // Pre-process all of the landmarks into a map of type -> matching points
        for (aws.sdk.kotlin.services.rekognition.model.Landmark landmark : landmarks) {
            LandmarkType type = LandmarkTypeAdapter.fromRekognition(landmark.getType().getValue());
            PointF point = new PointF(landmark.getX(), landmark.getY());
            List<PointF> points = landmarkMap.get(type);
            if (points == null) {
                points = new ArrayList<>();
                landmarkMap.put(type, points);
            }
            points.add(point);
            allPoints.add(point);
        }

        // Construct Amplify landmarks for each entry in the map + ALL_POINTS
        for (Map.Entry<LandmarkType, List<PointF>> entry : landmarkMap.entrySet()) {
            Landmark landmark = new Landmark(entry.getKey(), entry.getValue());
            amplifyLandmarks.add(landmark);
        }
        Landmark allPointsLandmark = new Landmark(LandmarkType.ALL_POINTS, allPoints);
        amplifyLandmarks.add(allPointsLandmark);

        return amplifyLandmarks;
    }

    /**
     * Gets all the binary features from AWS Rekognition's face
     * details and compiles them into a single list.
     * @param face the Rekognition face detail object
     * @return the list of Amplify {@link BinaryFeature}
     */
    public static List<BinaryFeature> fromFaceDetail(FaceDetail face) {
        return Arrays.asList(
                BinaryFeature.builder()
                        .type(BinaryFeatureType.BEARD.getAlias())
                        .value(face.getBeard().getValue())
                        .confidence(face.getBeard().getConfidence())
                        .build(),
                BinaryFeature.builder()
                        .type(BinaryFeatureType.SUNGLASSES.getAlias())
                        .value(face.getSunglasses().getValue())
                        .confidence(face.getSunglasses().getConfidence())
                        .build(),
                BinaryFeature.builder()
                        .type(BinaryFeatureType.SMILE.getAlias())
                        .value(face.getSmile().getValue())
                        .confidence(face.getSmile().getConfidence())
                        .build(),
                BinaryFeature.builder()
                        .type(BinaryFeatureType.EYE_GLASSES.getAlias())
                        .value(face.getEyeglasses().getValue())
                        .confidence(face.getEyeglasses().getConfidence())
                        .build(),
                BinaryFeature.builder()
                        .type(BinaryFeatureType.MUSTACHE.getAlias())
                        .value(face.getMustache().getValue())
                        .confidence(face.getMustache().getConfidence())
                        .build(),
                BinaryFeature.builder()
                        .type(BinaryFeatureType.MOUTH_OPEN.getAlias())
                        .value(face.getMouthOpen().getValue())
                        .confidence(face.getMouthOpen().getConfidence())
                        .build(),
                BinaryFeature.builder()
                        .type(BinaryFeatureType.EYES_OPEN.getAlias())
                        .value(face.getEyesOpen().getValue())
                        .confidence(face.getEyesOpen().getConfidence())
                        .build()
        );
    }
}
