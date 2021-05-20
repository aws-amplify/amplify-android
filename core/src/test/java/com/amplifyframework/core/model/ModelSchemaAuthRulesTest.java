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
import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.ModelConfig;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies that the provider string values from {@link AuthRule} annotations are mapped
 * to the appropriate {@link AuthStrategy.Provider} and subsequently {@link AuthorizationType}.
 */
public class ModelSchemaAuthRulesTest {

    /**
     * Verifies that the string values from the AuthRule annotation map to a valid enum item
     * from AuthStrategy.Provider
     */
    @Test
    public void authProviderMappingTest() throws AmplifyException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(Post.class);
        assertNotNull(modelSchema);
        List<com.amplifyframework.core.model.AuthRule> authRules = modelSchema.getAuthRules();
        assertEquals(4, authRules.size());
        assertEquals(AuthStrategy.Provider.USER_POOLS, authRules.get(0).getAuthProvider());
        assertEquals(AuthStrategy.Provider.OIDC, authRules.get(1).getAuthProvider());
        assertEquals(AuthStrategy.Provider.IAM, authRules.get(2).getAuthProvider());
        assertEquals(AuthStrategy.Provider.API_KEY, authRules.get(3).getAuthProvider());
    }

    /**
     * Verifies that an invalid provider value in the auth rule annotation causes an exception.
     * @throws AmplifyException Expected because "invalid" is not a valid {@link AuthStrategy.Provider}.
     */
    @Test(expected = AmplifyException.class)
    public void authProviderMappingInvalidRuleTest() throws AmplifyException {
        ModelSchema.fromModelClass(PostWithInvalidRule.class);
    }

    /**
     * Verifies the mapping of {@link AuthStrategy.Provider} to {@link AuthorizationType}.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void authorizationTypeMappingTest() throws AmplifyException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(Post.class);
        assertNotNull(modelSchema);
        List<com.amplifyframework.core.model.AuthRule> authRules = modelSchema.getAuthRules();
        assertEquals(4, authRules.size());
        assertEquals(AuthorizationType.AMAZON_COGNITO_USER_POOLS, AuthorizationType.from(authRules.get(0)
                                                                                                  .getAuthProvider()));
        assertEquals(AuthorizationType.OPENID_CONNECT, AuthorizationType.from(authRules.get(1)
                                                                                       .getAuthProvider()));
        assertEquals(AuthorizationType.AWS_IAM, AuthorizationType.from(authRules.get(2)
                                                                                .getAuthProvider()));
        assertEquals(AuthorizationType.API_KEY, AuthorizationType.from(authRules.get(3)
                                                                                .getAuthProvider()));
    }

    @ModelConfig(authRules = {
        @AuthRule(allow = AuthStrategy.OWNER, provider = "userPools"),
        @AuthRule(allow = AuthStrategy.GROUPS, provider = "oidc"),
        @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam"),
        @AuthRule(allow = AuthStrategy.PUBLIC, provider = "apiKey")

    })
    private abstract static class Post implements Model {}

    @ModelConfig(authRules = {
        @AuthRule(allow = AuthStrategy.PUBLIC, provider = "invalid")
    })
    private abstract static class PostWithInvalidRule implements Model {}
}
