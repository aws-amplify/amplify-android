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

import com.amplifyframework.predictions.models.AgeRange;
import com.amplifyframework.predictions.models.BinaryFeature;
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Landmark;
import com.amplifyframework.predictions.models.LandmarkType;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Pose;
import com.amplifyframework.testutils.FeatureAssert;
import com.amplifyframework.testutils.random.RandomString;

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

import aws.sdk.kotlin.services.rekognition.model.Beard;
import aws.sdk.kotlin.services.rekognition.model.BoundingBox;
import aws.sdk.kotlin.services.rekognition.model.EyeOpen;
import aws.sdk.kotlin.services.rekognition.model.Eyeglasses;
import aws.sdk.kotlin.services.rekognition.model.FaceDetail;
import aws.sdk.kotlin.services.rekognition.model.Geometry;
import aws.sdk.kotlin.services.rekognition.model.MouthOpen;
import aws.sdk.kotlin.services.rekognition.model.Mustache;
import aws.sdk.kotlin.services.rekognition.model.Point;
import aws.sdk.kotlin.services.rekognition.model.Smile;
import aws.sdk.kotlin.services.rekognition.model.Sunglasses;
import aws.sdk.kotlin.services.rekognition.model.TextDetection;
import kotlin.Unit;

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
        aws.sdk.kotlin.services.rekognition.model.Pose rekognitionPose =
                aws.sdk.kotlin.services.rekognition.model.Pose.Companion.invoke((poseBuilder) -> {
                    poseBuilder.setPitch(random.nextFloat());
                    poseBuilder.setRoll(random.nextFloat());
                    poseBuilder.setYaw(random.nextFloat());
                    return Unit.INSTANCE;
                });

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
        aws.sdk.kotlin.services.rekognition.model.AgeRange rekognitionAgeRange =
                aws.sdk.kotlin.services.rekognition.model.AgeRange.Companion.invoke((ageRangeBuilder) -> {
                    int low = random.nextInt();
                    int high = random.nextInt();
                    // high cannot be lower than low
                    if (low > high) {
                        int temp = high;
                        high = low;
                        low = temp;
                    }
                    ageRangeBuilder.setHigh(high);
                    ageRangeBuilder.setLow(low);
                    return Unit.INSTANCE;
                });

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
        TextDetection detection = TextDetection.Companion.invoke((textDetectionBuilder) -> {
            textDetectionBuilder.setDetectedText(RandomString.string());
            textDetectionBuilder.setConfidence(random.nextFloat());
            textDetectionBuilder.setGeometry(randomGeometry());
            return Unit.INSTANCE;
        });

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
        aws.sdk.kotlin.services.rekognition.model.Landmark leftEyeDown =
                aws.sdk.kotlin.services.rekognition.model.Landmark.Companion.invoke((landmarkBuilder) -> {
                    landmarkBuilder.setType(
                            aws.sdk.kotlin.services.rekognition.model.LandmarkType.LeftEyeDown.INSTANCE
                    );
                    landmarkBuilder.setX(random.nextFloat());
                    landmarkBuilder.setY(random.nextFloat());
                    return Unit.INSTANCE;
                });
        aws.sdk.kotlin.services.rekognition.model.Landmark leftEyeRight =
                aws.sdk.kotlin.services.rekognition.model.Landmark.Companion.invoke((landmarkBuilder) -> {
                    landmarkBuilder.setType(
                            aws.sdk.kotlin.services.rekognition.model.LandmarkType.LeftEyeRight.INSTANCE
                    );
                    landmarkBuilder.setX(random.nextFloat());
                    landmarkBuilder.setY(random.nextFloat());
                    return Unit.INSTANCE;
                });
        aws.sdk.kotlin.services.rekognition.model.Landmark mouthDown =
                aws.sdk.kotlin.services.rekognition.model.Landmark.Companion.invoke((landmarkBuilder) -> {
                    landmarkBuilder.setType(aws.sdk.kotlin.services.rekognition.model.LandmarkType.MouthDown.INSTANCE);
                    landmarkBuilder.setX(random.nextFloat());
                    landmarkBuilder.setY(random.nextFloat());
                    return Unit.INSTANCE;
                });
        List<aws.sdk.kotlin.services.rekognition.model.Landmark> rekognitionLandmarks = Arrays.asList(
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
        FaceDetail faceDetail = FaceDetail.Companion.invoke((faceDetailBuilder) -> {
            faceDetailBuilder.setBeard(Beard.Companion.invoke((beardBuilder) -> {
                beardBuilder.setValue(random.nextBoolean());
                beardBuilder.setConfidence(random.nextFloat());
                return Unit.INSTANCE;
            }));
            faceDetailBuilder.setSunglasses(Sunglasses.Companion.invoke((sunglassesBuilder) -> {
                sunglassesBuilder.setValue(random.nextBoolean());
                sunglassesBuilder.setConfidence(random.nextFloat());
                return Unit.INSTANCE;
            }));
            faceDetailBuilder.setSmile(Smile.Companion.invoke((smileBuilder) -> {
                smileBuilder.setValue(random.nextBoolean());
                smileBuilder.setConfidence(random.nextFloat());
                return Unit.INSTANCE;
            }));
            faceDetailBuilder.setEyeglasses(Eyeglasses.Companion.invoke((eyeglassesBuilder) -> {
                eyeglassesBuilder.setValue(random.nextBoolean());
                eyeglassesBuilder.setConfidence(random.nextFloat());
                return Unit.INSTANCE;
            }));
            faceDetailBuilder.setMustache(Mustache.Companion.invoke((mustacheBuilder) -> {
                mustacheBuilder.setValue(random.nextBoolean());
                mustacheBuilder.setConfidence(random.nextFloat());
                return Unit.INSTANCE;
            }));
            faceDetailBuilder.setMouthOpen(MouthOpen.Companion.invoke((mouthOpenBuilder) -> {
                mouthOpenBuilder.setValue(random.nextBoolean());
                mouthOpenBuilder.setConfidence(random.nextFloat());
                return Unit.INSTANCE;
            }));
            faceDetailBuilder.setEyesOpen(EyeOpen.Companion.invoke((eyesOpenBuilder) -> {
                eyesOpenBuilder.setValue(random.nextBoolean());
                eyesOpenBuilder.setConfidence(random.nextFloat());
                return Unit.INSTANCE;
            }));
            return Unit.INSTANCE;
        });

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
        return BoundingBox.Companion.invoke((boundingBoxBuilder) -> {
            boundingBoxBuilder.setHeight(random.nextFloat());
            boundingBoxBuilder.setLeft(random.nextFloat());
            boundingBoxBuilder.setTop(random.nextFloat());
            boundingBoxBuilder.setWidth(random.nextFloat());
            return Unit.INSTANCE;
        });
    }

    private List<Point> randomPolygon() {
        final int minPoints = 3;
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < minPoints; i++) {
            points.add(Point.Companion.invoke((pointBuilder) -> {
                pointBuilder.setX(random.nextFloat());
                pointBuilder.setY(random.nextFloat());
                return Unit.INSTANCE;
            }));
        }
        return points;
    }

    private Geometry randomGeometry() {
        return Geometry.Companion.invoke((geometryBuilder) -> {
            geometryBuilder.setBoundingBox(randomBox());
            geometryBuilder.setPolygon(randomPolygon());
            return Unit.INSTANCE;
        });
    }
}
