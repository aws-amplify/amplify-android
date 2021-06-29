/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.util;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.ModelConverter;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.testmodels.ratingsblog.User;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Tests the range {@link FieldFinder} class.
 */
public class FieldFinderTest {

    /**
     * Extracts the field value for a serialized model.
     * @throws AmplifyException On failure to derive ModelSchema or to convert Java model to Map
     * @throws NoSuchFieldException If field name does not exist for model
     */
    @Test
    public void extractsSerializedModelFieldValue() throws AmplifyException, NoSuchFieldException {
        String username = "foo";
        User user = User.builder().username(username).build();
        ModelSchema modelSchema = ModelSchema.fromModelClass(User.class);
        Map<String, Object> map = ModelConverter.toMap(user, modelSchema);
        SerializedModel serializedModel = SerializedModel.builder()
                .serializedData(map)
                .modelSchema(modelSchema)
                .build();
        Object extractedValue = FieldFinder.extractFieldValue(serializedModel, "username");
        Assert.assertEquals(username, extractedValue);
    }

    /**
     * Extracts the field value for a model.
     * @throws NoSuchFieldException If field name does not exist for model
     */
    @Test
    public void extractsModelFieldValue() throws NoSuchFieldException {
        String username = "foo";
        User user = User.builder().username(username).build();
        Object extractedValue = FieldFinder.extractFieldValue(user, "username");
        Assert.assertEquals(username, extractedValue);
    }
}
