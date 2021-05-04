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

package com.amplifyframework.core.model;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.datastore.appsync.SerializedModel;
import com.amplifyframework.testmodels.ownerauth.OwnerAuth;
import com.amplifyframework.testmodels.ownerauth.OwnerAuthNonDefaultProvider;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests auth rule functionality from {@link ModelSchema}.
 */
public final class ModelSchemaAuthRulesTest {

    /**
     * Given a model that has an auth rule with a non-default provider specified, that value specified
     * in the authProvider field should be returned.
     * @throws AmplifyException from model schema parsing.
     */
    @Test
    public void modelWithDefaultAuthProviderTest() throws AmplifyException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(OwnerAuth.class);
        assertEquals(1, modelSchema.getAuthRules().size());
        AuthRule authRule = modelSchema.getAuthRules().get(0);
        assertEquals(AuthorizationType.AMAZON_COGNITO_USER_POOLS, authRule.getAuthProvider());
    }

    /**
     * Given a model that has an auth rule with a non-default provider specified, that value specified
     * in the authProvider field should be returned.
     * @throws AmplifyException from model schema parsing.
     */
    @Test
    public void modelWithNonDefaultAuthProviderTest() throws AmplifyException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(OwnerAuthNonDefaultProvider.class);
        assertEquals(1, modelSchema.getAuthRules().size());
        AuthRule authRule = modelSchema.getAuthRules().get(0);
        assertEquals(AuthorizationType.OPENID_CONNECT, authRule.getAuthProvider());
    }

    /**
     * Verify construction of {@link AuthRule}s created via the builder.
     */
    @Test
    public void modelWithDefaultAuthProviderUsingBuilderTest() {
        ModelSchema schema = fromBuilder(AuthorizationType.DEFAULT);
        assertEquals(1, schema.getAuthRules().size());
        AuthRule authRule = schema.getAuthRules().get(0);
        assertEquals(AuthorizationType.AMAZON_COGNITO_USER_POOLS, authRule.getAuthProvider());
    }

    /**
     * Verify construction of {@link AuthRule}s created via the builder.
     */
    @Test
    public void modelWithNonDefaultAuthProviderUsingBuilderTest() {
        ModelSchema schema = fromBuilder(AuthorizationType.OPENID_CONNECT);
        assertEquals(1, schema.getAuthRules().size());
        AuthRule authRule = schema.getAuthRules().get(0);
        assertEquals(AuthorizationType.OPENID_CONNECT, authRule.getAuthProvider());
    }

    /**
     * Test the getApplicableRules method which filters auth rules based on a given {@link ModelOperation}.
     */
    @Test
    public void getApplicableRulesTest() {
        ModelSchema schema = fromBuilder(AuthorizationType.DEFAULT);
        List<AuthRule> rulesForCreateOperation = schema.getApplicableRules(ModelOperation.CREATE);
        List<AuthRule> rulesForDeleteOperation = schema.getApplicableRules(ModelOperation.DELETE);
        List<AuthRule> rulesForUpdateOperation = schema.getApplicableRules(ModelOperation.UPDATE);
        List<AuthRule> rulesForReadOperation = schema.getApplicableRules(ModelOperation.READ);

        assertEquals(1, rulesForCreateOperation.size());
        assertEquals(0, rulesForDeleteOperation.size());
        assertEquals(0, rulesForUpdateOperation.size());
        assertEquals(0, rulesForReadOperation.size());
    }

    private ModelSchema fromBuilder(AuthorizationType authProvider) {
        ModelField modelId = ModelField.builder()
                                       .isRequired(true)
                                       .targetType("ID")
                                       .javaClassForValue(String.class)
                                       .build();
        ModelField title = ModelField.builder()
                                     .isRequired(true)
                                     .targetType("String")
                                     .javaClassForValue(String.class)
                                     .build();
        Map<String, ModelField> fields = new HashMap<>();
        fields.put("id", modelId);
        fields.put("title", title);

        return ModelSchema.builder()
                          .name("AuthTest")
                          .modelClass(SerializedModel.class)
                          .fields(fields)
                          .authRules(Collections.singletonList(AuthRule.builder()
                                                                       .authStrategy(AuthStrategy.OWNER)
                                                                       .authProvider(authProvider)
                                                                       .ownerField("owner")
                                                                       .operations(Arrays.asList(ModelOperation.CREATE))
                                                                       .build()
                          ))
                          .build();
    }
}
