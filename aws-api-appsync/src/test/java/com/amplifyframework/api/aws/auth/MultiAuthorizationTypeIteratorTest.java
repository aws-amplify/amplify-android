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

package com.amplifyframework.api.aws.auth;

import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.auth.MultiAuthorizationTypeIterator;
import com.amplifyframework.util.Empty;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MultiAuthorizationTypeIteratorTest {
    private static final String DEFAULT_ID_CLAIM = "cognito:username";
    private static final String DEFAULT_OWNER_FIELD = "owner";
    private static final String DEFAULT_GROUP_CLAIM = "cognito:groups";
    private static final String DEFAULT_GROUP_FIELD = "groups";
    private static final List<String> DEFAULT_GROUPS = Arrays.asList("Admins");

    private static final List<ModelOperation> DEFAULT_OPERATIONS = Arrays.asList(
        ModelOperation.CREATE,
        ModelOperation.UPDATE,
        ModelOperation.DELETE,
        ModelOperation.READ
    );

    private static final List<ModelOperation> CREATE_DELETE_OPERATIONS = Arrays.asList(
        ModelOperation.CREATE,
        ModelOperation.DELETE
    );

    /**
     * Test a schema with auth rules for each strategy is returned in the expected order.
     */
    @Test
    public void testAllRules() {
        Iterator<AuthorizationType> expectedAuthTypes = Arrays.asList(
            AuthorizationType.AMAZON_COGNITO_USER_POOLS,
            AuthorizationType.OPENID_CONNECT,
            AuthorizationType.AWS_IAM,
            AuthorizationType.API_KEY
        ).iterator();
        Iterator<Boolean> expectedIsOwnerFlags = Arrays.asList(true, false, false, false).iterator();

        List<AuthRule> authRules = Arrays.asList(
            buildGroupRule(AuthStrategy.Provider.OIDC, null, null, null, null),
            buildPrivateRule(AuthStrategy.Provider.IAM, null),
            buildOwnerRule(null, null, null, null),
            buildPublicRule(null, null)
        );
        MultiAuthorizationTypeIterator actualAuthTypeIterator = new MultiAuthorizationTypeIterator(authRules);
        assertIteratorState(expectedAuthTypes, expectedIsOwnerFlags, actualAuthTypeIterator);
    }

    /**
     * If there are multiple owner based rules (a couple using userPools and one using oidc),
     * it should only return 2 auth types (one for userPools and one for oidc).
     */
    @Test
    public void testMultiOwnerRules() {
        Iterator<AuthorizationType> expectedAuthTypes = Arrays.asList(
            AuthorizationType.AMAZON_COGNITO_USER_POOLS,
            AuthorizationType.OPENID_CONNECT
        ).iterator();
        Iterator<Boolean> expectedIsOwnerFlags = Arrays.asList(true, true).iterator();

        List<AuthRule> authRules = Arrays.asList(
            buildOwnerRule(AuthStrategy.Provider.OIDC,
                           "differentOwner",
                           "myClaim",
                           Arrays.asList(ModelOperation.CREATE, ModelOperation.DELETE)),
            buildOwnerRule(null, null, null, null),
            buildOwnerRule(AuthStrategy.Provider.USER_POOLS,
                           "differentOwnerField",
                           null,
                           Arrays.asList(ModelOperation.CREATE, ModelOperation.DELETE))

        );
        MultiAuthorizationTypeIterator actualAuthTypeIterator = new MultiAuthorizationTypeIterator(authRules);

        assertIteratorState(expectedAuthTypes, expectedIsOwnerFlags, actualAuthTypeIterator);
    }

    /**
     * Verify that if there are mixed owner and group rules, we return the auth types in the correct order.
     * We're verifying that:
     * - both owner rules are processed first (userPools and oidc) with isOwner = true.
     * - both group rules are processed next (userPools and oidc) withg isOwner = false.
     *
     * isOwner is used in the code to determine whether a request may need the owner parameter added.
     */
    @Test
    public void testOwnerAndGroupRules() {
        Iterator<AuthorizationType> expectedAuthTypes = Arrays.asList(
            AuthorizationType.AMAZON_COGNITO_USER_POOLS,
            AuthorizationType.OPENID_CONNECT,
            AuthorizationType.AMAZON_COGNITO_USER_POOLS,
            AuthorizationType.OPENID_CONNECT
        ).iterator();
        Iterator<Boolean> expectedIsOwnerFlags = Arrays.asList(true, true, false, false).iterator();

        List<AuthRule> authRules = Arrays.asList(
            buildOwnerRule(null, null, null, null),
            buildOwnerRule(AuthStrategy.Provider.USER_POOLS,
                           "differentOwnerField",
                           null,
                           CREATE_DELETE_OPERATIONS),
            buildOwnerRule(AuthStrategy.Provider.OIDC,
                           "differentOwner",
                           "myClaim",
                           null),
            buildGroupRule(null, null, null, null, null),
            buildGroupRule(AuthStrategy.Provider.OIDC,
                           "myGroupField",
                           "someClaim",
                           Collections.singletonList("group1"),
                           null)
        );
        MultiAuthorizationTypeIterator actualAuthTypeIterator = new MultiAuthorizationTypeIterator(authRules);
        assertIteratorState(expectedAuthTypes, expectedIsOwnerFlags, actualAuthTypeIterator);
    }

    private void assertIteratorState(Iterator<AuthorizationType> expectedAuthTypes,
                                     Iterator<Boolean> expectedIsOwnerFlag,
                                     MultiAuthorizationTypeIterator actualResults) {
        while (expectedAuthTypes.hasNext() && actualResults.hasNext() && expectedIsOwnerFlag.hasNext()) {
            assertEquals(expectedAuthTypes.next(), actualResults.next());
            assertEquals(expectedIsOwnerFlag.next(), actualResults.isOwnerBasedRule());
            assertEquals(expectedAuthTypes.hasNext(), actualResults.hasNext());
            assertEquals(expectedIsOwnerFlag.hasNext(), actualResults.hasNext());
        }
    }

    private AuthRule buildGroupRule(AuthStrategy.Provider authProvider,
                                    String groupField,
                                    String groupClaim,
                                    List<String> groups,
                                    List<ModelOperation> operations) {
        return AuthRule.builder()
                       .authStrategy(AuthStrategy.GROUPS)
                       .authProvider(authProvider == null ? AuthStrategy.GROUPS.getDefaultAuthProvider() : authProvider)
                       .identityClaim(groupClaim == null ? DEFAULT_GROUP_CLAIM : groupClaim)
                       .ownerField(groupField == null ? DEFAULT_GROUP_FIELD : groupField)
                       .groups(Empty.check(groups) ? DEFAULT_GROUPS : groups)
                       .operations(Empty.check(operations) ? DEFAULT_OPERATIONS : operations)
                       .build();
    }

    private AuthRule buildOwnerRule(AuthStrategy.Provider authProvider,
                                    String ownerField,
                                    String idClaim,
                                    List<ModelOperation> operations) {
        return AuthRule.builder()
                       .authStrategy(AuthStrategy.OWNER)
                       .authProvider(authProvider == null ? AuthStrategy.OWNER.getDefaultAuthProvider() : authProvider)
                       .identityClaim(idClaim == null ? DEFAULT_ID_CLAIM : idClaim)
                       .ownerField(ownerField == null ? DEFAULT_OWNER_FIELD : ownerField)
                       .operations(Empty.check(operations) ? DEFAULT_OPERATIONS : operations)
                       .build();
    }

    private AuthRule buildPrivateRule(AuthStrategy.Provider authProvider,
                                      List<ModelOperation> operations) {
        return AuthRule.builder()
            .authStrategy(AuthStrategy.PRIVATE)
            .authProvider(authProvider == null ? AuthStrategy.PRIVATE.getDefaultAuthProvider() : authProvider)
            .operations(Empty.check(operations) ? DEFAULT_OPERATIONS : operations)
            .build();
    }

    private AuthRule buildPublicRule(AuthStrategy.Provider authProvider,
                                     List<ModelOperation> operations) {
        return AuthRule.builder()
                       .authStrategy(AuthStrategy.PUBLIC)
                       .authProvider(authProvider == null ? AuthStrategy.PUBLIC.getDefaultAuthProvider() : authProvider)
                       .operations(Empty.check(operations) ? DEFAULT_OPERATIONS : operations)
                       .build();
    }
}
