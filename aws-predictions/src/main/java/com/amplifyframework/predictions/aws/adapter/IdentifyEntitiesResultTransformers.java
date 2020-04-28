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

import com.amplifyframework.predictions.models.Landmark;
import com.amplifyframework.predictions.models.LandmarkType;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Pose;
import com.amplifyframework.util.CollectionUtils;

import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to transform Amazon service-specific
 * models to be compatible with AWS Amplify.
 */
public final class IdentifyEntitiesResultTransformers {
    private IdentifyEntitiesResultTransformers() {}

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
     * Converts {@link com.amazonaws.services.rekognition.model.Pose}
     * from Amazon Rekognition into Amplify-compatible pose data.
     * @param pose the pose provided by Amazon Rekognition
     * @return the Amplify pose with same orientation
     */
    @Nullable
    public static Pose fromRekognitionPose(@Nullable com.amazonaws.services.rekognition.model.Pose pose) {
        if (pose == null) {
            return null;
        }
        return new Pose(pose.getPitch(), pose.getRoll(), pose.getYaw());
    }

    /**
     * Converts a list of {@link com.amazonaws.services.rekognition.model.Landmark}
     * from Amazon Rekognition into Amplify-compatible list of
     * {@link Landmark} objects.
     * @param landmarks the list of Amazon Rekognition landmark objects
     * @return the list of Amplify Predictions landmark objects
     */
    @NonNull
    public static List<Landmark> fromLandmarks(
            @Nullable List<com.amazonaws.services.rekognition.model.Landmark> landmarks
    ) {
        List<Landmark> amplifyLandmarks = new ArrayList<>();
        if (CollectionUtils.isNullOrEmpty(landmarks)) {
            return amplifyLandmarks;
        }

        List<PointF> allPoints = new ArrayList<>();
        Map<LandmarkType, List<PointF>> landmarkMap = new HashMap<>();

        // Pre-process all of the landmarks into a map of type -> matching points
        for (com.amazonaws.services.rekognition.model.Landmark landmark : landmarks) {
            LandmarkType type = LandmarkTypeAdapter.fromRekognition(landmark.getType());
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
}
