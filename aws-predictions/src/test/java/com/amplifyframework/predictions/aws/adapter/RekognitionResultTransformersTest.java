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

import com.amplifyframework.predictions.models.AgeRange;
import com.amplifyframework.predictions.models.BinaryFeature;
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Landmark;
import com.amplifyframework.predictions.models.LandmarkType;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Pose;
import com.amplifyframework.testutils.FeatureAssert;
import com.amplifyframework.testutils.random.RandomString;

import com.amazonaws.services.rekognition.model.Beard;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.EyeOpen;
import com.amazonaws.services.rekognition.model.Eyeglasses;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Geometry;
import com.amazonaws.services.rekognition.model.MouthOpen;
import com.amazonaws.services.rekognition.model.Mustache;
import com.amazonaws.services.rekognition.model.Point;
import com.amazonaws.services.rekognition.model.Smile;
import com.amazonaws.services.rekognition.model.Sunglasses;
import com.amazonaws.services.rekognition.model.TextDetection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the result transformer utility methods work
 * as intended.
 */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("ConstantConditions") // NullPointerException will not be thrown
public final class RekognitionResultTransformersTest {
    private static final double DELTA = 1E-5;

    private Random random;

    /**
     * Sets up test dependencies.
     */
    @Before
    public void setUp() {
        random = new Random();
    }

    /**
     * Tests that the rectangular boundary from Rekognition
     * is converted to an equivalent Android rectangle object.
     */
    @Test
    public void testBoundingBoxConversion() {
        BoundingBox box = randomBox();
        RectF rect = RekognitionResultTransformers.fromBoundingBox(box);
        assertEquals(box.getHeight(), rect.height(), DELTA);
        assertEquals(box.getWidth(), rect.width(), DELTA);
        assertEquals(box.getLeft(), rect.left, DELTA);
        assertEquals(box.getTop(), rect.top, DELTA);
    }

    /**
     * Tests that the polygonal boundary from Textract in the form
     * of list of points is converted to an Amplify shape for polygons.
     */
    @Test
    public void testPolygonConversion() {
        List<Point> randomPolygon = randomPolygon();
        Polygon polygon = RekognitionResultTransformers.fromPoints(randomPolygon);
        List<PointF> actualPoints = polygon.getPoints();
        List<PointF> expectedPoints = new ArrayList<>();
        for (Point point : randomPolygon) {
            expectedPoints.add(new PointF(point.getX(), point.getY()));
        }
        assertEquals(expectedPoints, actualPoints);
    }

    /**
     * Tests that the orientation pose from Rekognition is
     * converted to an equivalent Amplify pose object.
     */
    @Test
    public void testPoseConversion() {
        com.amazonaws.services.rekognition.model.Pose rekognitionPose =
                new com.amazonaws.services.rekognition.model.Pose()
                .withPitch(random.nextFloat())
                .withRoll(random.nextFloat())
                .withYaw(random.nextFloat());

        Pose amplifyPose = RekognitionResultTransformers.fromRekognitionPose(rekognitionPose);
        assertEquals(rekognitionPose.getPitch(), amplifyPose.getPitch(), DELTA);
        assertEquals(rekognitionPose.getRoll(), amplifyPose.getRoll(), DELTA);
        assertEquals(rekognitionPose.getYaw(), amplifyPose.getYaw(), DELTA);
    }

    /**
     * Tests that the age range from Rekognition is converted
     * to an equivalent Amplify age range.
     */
    @Test
    public void testAgeRangeConversion() {
        int low = random.nextInt();
        int high = random.nextInt();
        // high cannot be lower than low
        if (low > high) {
            int temp = high;
            high = low;
            low = temp;
        }
        com.amazonaws.services.rekognition.model.AgeRange rekognitionAgeRange =
                new com.amazonaws.services.rekognition.model.AgeRange()
                .withHigh(high)
                .withLow(low);

        AgeRange amplifyAgeRange = RekognitionResultTransformers.fromRekognitionAgeRange(rekognitionAgeRange);
        assertEquals(rekognitionAgeRange.getHigh().intValue(), amplifyAgeRange.getHigh());
        assertEquals(rekognitionAgeRange.getLow().intValue(), amplifyAgeRange.getLow());
    }

    /**
     * Tests that the text detection from Rekognition is converted
     * to an Amplify image text feature.
     */
    @Test
    public void testTextDetectionConversion() {
        TextDetection detection = new TextDetection()
                .withDetectedText(RandomString.string())
                .withConfidence(random.nextFloat())
                .withGeometry(randomGeometry());

        // Test text detection conversion
        IdentifiedText text = RekognitionResultTransformers.fromTextDetection(detection);
        assertEquals(detection.getDetectedText(), text.getText());
        assertEquals(detection.getConfidence(), text.getConfidence(), DELTA);
    }

    /**
     * Tests that the landmarks from Rekognition are converted
     * from a list of individual points to a list of Amplify
     * landmarks that are mapped by their types.
     */
    @Test
    public void testLandmarksConversion() {
        com.amazonaws.services.rekognition.model.Landmark leftEyeDown =
                new com.amazonaws.services.rekognition.model.Landmark()
                        .withType(com.amazonaws.services.rekognition.model.LandmarkType.LeftEyeDown)
                        .withX(random.nextFloat())
                        .withY(random.nextFloat());
        com.amazonaws.services.rekognition.model.Landmark leftEyeRight =
                new com.amazonaws.services.rekognition.model.Landmark()
                        .withType(com.amazonaws.services.rekognition.model.LandmarkType.LeftEyeRight)
                        .withX(random.nextFloat())
                        .withY(random.nextFloat());
        com.amazonaws.services.rekognition.model.Landmark mouthDown =
                new com.amazonaws.services.rekognition.model.Landmark()
                        .withType(com.amazonaws.services.rekognition.model.LandmarkType.MouthDown)
                        .withX(random.nextFloat())
                        .withY(random.nextFloat());
        List<com.amazonaws.services.rekognition.model.Landmark> rekognitionLandmarks = Arrays.asList(
                leftEyeDown,
                leftEyeRight,
                mouthDown
        );

        List<Landmark> amplifyLandmarks = RekognitionResultTransformers.fromLandmarks(rekognitionLandmarks);
        Map<LandmarkType, List<PointF>> map = new HashMap<>();
        for (Landmark landmark : amplifyLandmarks) {
            map.put(landmark.getType(), landmark.getPoints());
        }

        assertEquals(map.keySet(), new HashSet<>(Arrays.asList(
                LandmarkType.ALL_POINTS,
                LandmarkType.LEFT_EYE,
                LandmarkType.OUTER_LIPS
        )));
        assertTrue(map.get(LandmarkType.ALL_POINTS).containsAll(Arrays.asList(
                new PointF(leftEyeDown.getX(), leftEyeDown.getY()),
                new PointF(leftEyeRight.getX(), leftEyeRight.getY()),
                new PointF(mouthDown.getX(), mouthDown.getY())
        )));
        assertTrue(map.get(LandmarkType.LEFT_EYE).containsAll(Arrays.asList(
                new PointF(leftEyeDown.getX(), leftEyeDown.getY()),
                new PointF(leftEyeRight.getX(), leftEyeRight.getY())
        )));
        assertTrue(map.get(LandmarkType.OUTER_LIPS).contains(
                new PointF(mouthDown.getX(), mouthDown.getY())
        ));
    }

    /**
     * Tests that the individual attributes from Rekognition
     * face detail are properly converted to a list of binary
     * features that are compatible with Amplify's entity
     * details.
     */
    @Test
    public void testFaceDetailConversion() {
        FaceDetail faceDetail = new FaceDetail()
                .withBeard(new Beard()
                        .withValue(random.nextBoolean())
                        .withConfidence(random.nextFloat()))
                .withSunglasses(new Sunglasses()
                        .withValue(random.nextBoolean())
                        .withConfidence(random.nextFloat()))
                .withSmile(new Smile()
                        .withValue(random.nextBoolean())
                        .withConfidence(random.nextFloat()))
                .withEyeglasses(new Eyeglasses()
                        .withValue(random.nextBoolean())
                        .withConfidence(random.nextFloat()))
                .withMustache(new Mustache()
                        .withValue(random.nextBoolean())
                        .withConfidence(random.nextFloat()))
                .withMouthOpen(new MouthOpen()
                        .withValue(random.nextBoolean())
                        .withConfidence(random.nextFloat()))
                .withEyesOpen(new EyeOpen()
                        .withValue(random.nextBoolean())
                        .withConfidence(random.nextFloat()));

        List<BinaryFeature> features = RekognitionResultTransformers.fromFaceDetail(faceDetail);
        FeatureAssert.assertMatches(
                Arrays.asList(
                        faceDetail.getBeard().getValue(),
                        faceDetail.getSunglasses().getValue(),
                        faceDetail.getSmile().getValue(),
                        faceDetail.getEyeglasses().getValue(),
                        faceDetail.getMustache().getValue(),
                        faceDetail.getMouthOpen().getValue(),
                        faceDetail.getEyesOpen().getValue()
                ),
                features
        );
    }

    private BoundingBox randomBox() {
        return new BoundingBox()
                .withHeight(random.nextFloat())
                .withLeft(random.nextFloat())
                .withTop(random.nextFloat())
                .withWidth(random.nextFloat());
    }

    private List<Point> randomPolygon() {
        final int minPoints = 3;
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < minPoints; i++) {
            points.add(new Point()
                    .withX(random.nextFloat())
                    .withY(random.nextFloat())
            );
        }
        return points;
    }

    private Geometry randomGeometry() {
        return new Geometry()
                .withBoundingBox(randomBox())
                .withPolygon(randomPolygon());
    }
}
