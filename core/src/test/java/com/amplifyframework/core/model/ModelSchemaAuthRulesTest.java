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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests auth rule functionality from {@link ModelSchema}.
 */
public final class ModelSchemaAuthRulesTest {
    private static final AuthorizationType DEFAULT_AUTH_PROVIDER = AuthorizationType.AMAZON_COGNITO_USER_POOLS;
    private static final AuthorizationType NON_DEFAULT_AUTH_PROVIDER = AuthorizationType.OPENID_CONNECT;
    private static final List<ModelOperation> CREATE_ONLY = Collections.singletonList(ModelOperation.CREATE);

    private static final AuthRule RULE_NON_DEFAULT_PROVIDER = AuthRule.builder()
                                                                      .authStrategy(AuthStrategy.OWNER)
                                                                      .authProvider(NON_DEFAULT_AUTH_PROVIDER)
                                                                      .ownerField("owner")
                                                                      .operations(CREATE_ONLY)
                                                                      .build();
    private static final ModelField MODEL_ID_FIELD = ModelField.builder()
                                                               .isRequired(true)
                                                               .targetType("ID")
                                                               .javaClassForValue(String.class)
                                                               .build();
    private static final ModelField TITLE_NO_AUTH = ModelField.builder()
                                                              .isRequired(true)
                                                              .targetType("String")
                                                              .javaClassForValue(String.class)
                                                              .build();

    private static final ModelField TITLE_WITH_NON_DEFAULT_PROVIDER = ModelField.builder()
                                                                                .isRequired(true)
                                                                                .targetType("String")
                                                                                .javaClassForValue(String.class)
                                                                                .authRules(Collections.singletonList(
                                                                                    RULE_NON_DEFAULT_PROVIDER
                                                                                ))
                                                                                .build();

    /**
     * Given a model that has an auth rule with a non-default provider specified, that value specified
     * in the authProvider field should be returned.
     * @throws AmplifyException from model schema parsing.
     */
    @Test
    public void modelWithDefaultAuthProviderTest() throws AmplifyException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(OwnerAuth.class);
        List<AuthRule> rules = modelSchema.getApplicableRules(ModelOperation.CREATE);
        assertTrue(modelSchema.hasModelLevelRules());
        assertEquals(1, rules.size());
        AuthRule authRule = rules.get(0);
        assertEquals(DEFAULT_AUTH_PROVIDER, authRule.getAuthProvider());
    }

    /**
     * Given a model that has an auth rule with a non-default provider specified, that value specified
     * in the authProvider field should be returned.
     * @throws AmplifyException from model schema parsing.
     */
    @Test
    public void modelWithNonDefaultAuthProviderTest() throws AmplifyException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(OwnerAuthNonDefaultProvider.class);
        List<AuthRule> rules = modelSchema.getApplicableRules(ModelOperation.CREATE);
        assertTrue(modelSchema.hasModelLevelRules());
        assertEquals(1, rules.size());
        AuthRule authRule = rules.get(0);
        assertEquals(NON_DEFAULT_AUTH_PROVIDER, authRule.getAuthProvider());
    }

    /**
     * Verify construction of {@link AuthRule}s created via the builder.
     */
    @Test
    public void modelWithDefaultAuthProviderUsingBuilderTest() {
        ModelSchema modelSchema = modelAuthOnly(DEFAULT_AUTH_PROVIDER);
        List<AuthRule> rules = modelSchema.getApplicableRules(ModelOperation.CREATE);
        assertTrue(modelSchema.hasModelLevelRules());
        assertEquals(1, rules.size());
        AuthRule authRule = rules.get(0);
        assertEquals(DEFAULT_AUTH_PROVIDER, authRule.getAuthProvider());
    }

    /**
     * Verify construction of {@link AuthRule}s created via the builder.
     */
    @Test
    public void modelWithNonDefaultAuthProviderUsingBuilderTest() {
        ModelSchema modelSchema = modelAuthOnly(NON_DEFAULT_AUTH_PROVIDER);
        List<AuthRule> rules = modelSchema.getApplicableRules(ModelOperation.CREATE);
        assertTrue(modelSchema.hasModelLevelRules());
        assertEquals(1, rules.size());
        AuthRule authRule = rules.get(0);
        assertEquals(NON_DEFAULT_AUTH_PROVIDER, authRule.getAuthProvider());
    }

    /**
     * Verify construction of {@link AuthRule}s created via the builder.
     */
    @Test
    public void modelWithNonDefaultAuthProviderForFieldOnlyUsingBuilderTest() {
        ModelSchema modelSchema = fieldLevelAuthOnly();
        List<AuthRule> rules = modelSchema.getApplicableRules(ModelOperation.CREATE);
        // Model schema doesn't have model level rules, so this should be false.
        assertFalse(modelSchema.hasModelLevelRules());
        assertEquals(1, rules.size());
        AuthRule authRule = rules.get(0);
        assertEquals(NON_DEFAULT_AUTH_PROVIDER, authRule.getAuthProvider());
    }

    /**
     * Test the getApplicableRules method which filters auth rules based on a given {@link ModelOperation}.
     */
    @Test
    public void getApplicableRulesTest() {
        ModelSchema schema = modelAuthOnly(NON_DEFAULT_AUTH_PROVIDER);
        List<AuthRule> rulesForCreateOperation = schema.getApplicableRules(ModelOperation.CREATE);
        List<AuthRule> rulesForDeleteOperation = schema.getApplicableRules(ModelOperation.DELETE);
        List<AuthRule> rulesForUpdateOperation = schema.getApplicableRules(ModelOperation.UPDATE);
        List<AuthRule> rulesForReadOperation = schema.getApplicableRules(ModelOperation.READ);

        assertEquals(1, rulesForCreateOperation.size());
        assertEquals(0, rulesForDeleteOperation.size());
        assertEquals(0, rulesForUpdateOperation.size());
        assertEquals(0, rulesForReadOperation.size());
    }

    private ModelSchema modelAuthOnly(AuthorizationType authProvider) {
        Map<String, ModelField> fields = new HashMap<>();
        fields.put("id", MODEL_ID_FIELD);
        fields.put("title", TITLE_NO_AUTH);

        return ModelSchema.builder()
                   .name("AuthTest")
                   .modelClass(SerializedModel.class)
                   .fields(new HashMap<>())
                   .authRules(Collections.singletonList(AuthRule.builder()
                                                                .authStrategy(AuthStrategy.OWNER)
                                                                .authProvider(authProvider)
                                                                .ownerField("owner")
                                                                .operations(CREATE_ONLY)
                                                                .build()
                   ))
                   .build();
    }

    private ModelSchema fieldLevelAuthOnly() {
        List<ModelOperation> createOnly = Collections.singletonList(ModelOperation.CREATE);
        Map<String, ModelField> fields = new HashMap<>();
        fields.put("id", MODEL_ID_FIELD);
        fields.put("title", TITLE_WITH_NON_DEFAULT_PROVIDER);

        return ModelSchema.builder()
                          .name("AuthTest")
                          .modelClass(SerializedModel.class)
                          .fields(fields)
                          .build();
    }
}
