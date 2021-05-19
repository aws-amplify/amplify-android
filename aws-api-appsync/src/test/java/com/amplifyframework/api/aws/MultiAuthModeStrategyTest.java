/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests for multi auth strategy.
 */
public class MultiAuthModeStrategyTest {
    private ModelSchema postModelSchema;
    private ModelSchema carModelSchema;
    private MultiAuthModeStrategy strategy;

    /**
     * Test setup.
     * @throws AmplifyException Not expected.
     */
    @Before
    public void setup() throws AmplifyException {
        postModelSchema = ModelSchema.fromModelClass(Post.class);
        carModelSchema = ModelSchema.fromModelClass(Car.class);
    }

    /**
     * Basic multiauth test.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testMultiAuthBasic() throws AmplifyException {
        Iterator<AuthorizationType> results =
            MultiAuthModeStrategy.getInstance().authTypesFor(postModelSchema, ModelOperation.READ);

        assertEquals(AuthorizationType.AMAZON_COGNITO_USER_POOLS, results.next());
        assertEquals(AuthorizationType.API_KEY, results.next());
    }

    /**
     * Test that the correct auth types are returned for subscriptions. In this case, both auth types
     * configured on the model will be returned because both have READ permission.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testMultiAuthForSubscription() throws AmplifyException {
        Iterator<AuthorizationType> results =
            MultiAuthModeStrategy.getInstance().authTypesFor(postModelSchema, ModelOperation.READ);

        assertEquals(AuthorizationType.AMAZON_COGNITO_USER_POOLS, results.next());
        assertEquals(AuthorizationType.API_KEY, results.next());
    }

    /**
     * Test that the correct auth types are returned for mutations. In this case,
     * only user pools should be returned because only owners have permissions to
     * perform mutations.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testMultiAuthForMutations() throws AmplifyException {
        Iterator<AuthorizationType> results =
            MultiAuthModeStrategy.getInstance().authTypesFor(postModelSchema, ModelOperation.CREATE);

        assertEquals(AuthorizationType.AMAZON_COGNITO_USER_POOLS, results.next());
        assertFalse(results.hasNext());
    }

    /**
     * Test that no auth types are returned if no rules are set on the model.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testMultiAuthNoRules() throws AmplifyException {

        Iterator<AuthorizationType> results =
            MultiAuthModeStrategy.getInstance().authTypesFor(carModelSchema, ModelOperation.READ);

        assertFalse(results.hasNext());
    }

    @ModelConfig(pluralName = "Posts", authRules = {
        @AuthRule(allow = AuthStrategy.OWNER,
            ownerField = "owner",
            identityClaim = "cognito:username",
            operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ }),
        @AuthRule(allow = AuthStrategy.PUBLIC,
            operations = { ModelOperation.READ })
    })
    private abstract static class Post implements Model {}

    @ModelConfig
    @Index(fields = {"vehicle_model"}, name = "model_based_index")
    private abstract static class Car implements Model {}
}
