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
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.testmodels.multiauth.Post;
import com.amplifyframework.testmodels.personcar.Car;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MultiAuthModeStrategyTest {
    private MultiAuthModeStrategy.PriorityBasedAuthRuleProviderComparator compWithDefaultPriority;
    private MultiAuthModeStrategy strategy;

    /**
     * Setup test fixtures.
     */
    @Before
    public void setup() {
        compWithDefaultPriority = new MultiAuthModeStrategy.PriorityBasedAuthRuleProviderComparator();
        strategy = new MultiAuthModeStrategy();
    }

    /**
     * Basic test for the comparator.
     */
    @Test
    public void testAuthStrategyComparatorBasic() {
        List<AuthRule> expected = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.OWNER).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build()
        );

        List<AuthRule> authRules = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build(),
            AuthRule.builder().authStrategy(AuthStrategy.OWNER).build()
        );
        sortAndCompare(authRules, expected);
    }

    /**
     * Verify that comparison of lists of different sizes fails.
     */
    @Test (expected = AssertionError.class)
    public void testAuthStrategyComparatorDiffSize() {
        List<AuthRule> expected = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.OWNER).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build()
        );

        List<AuthRule> authRules = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build()
        );
        sortAndCompare(authRules, expected);
    }

    /**
     * Test using all 4 types in random order.
     */
    @Test
    public void testAuthStrategyComparatorScrambled() {
        List<AuthRule> expected = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.OWNER).build(),
            AuthRule.builder().authStrategy(AuthStrategy.GROUPS).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PRIVATE).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build()
        );

        List<AuthRule> authRules = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.PRIVATE).build(),
            AuthRule.builder().authStrategy(AuthStrategy.GROUPS).build(),
            AuthRule.builder().authStrategy(AuthStrategy.OWNER).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build()
        );
        sortAndCompare(authRules, expected);
    }

    /**
     * Basic multiauth test.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testMultiAuthBasic() throws AmplifyException {
        Iterator<AuthorizationType> results =
            strategy.authTypesFor(ModelSchema.fromModelClass(Post.class), ModelOperation.READ);

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
        AppSyncGraphQLRequest<Object> request = AppSyncGraphQLRequest.builder()
                                                                     .modelClass(Post.class)
                                                                     .responseType(String.class)
                                                                     .operation(SubscriptionType.ON_CREATE)
                                                                     .requestOptions(new DefaultGraphQLRequestOptions())
                                                                     .build();

        Iterator<AuthorizationType> results =
            strategy.authTypesFor(request.getModelSchema(), request.getAuthRuleOperation());

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
        AppSyncGraphQLRequest<Object> request = AppSyncGraphQLRequest.builder()
                                                                     .modelClass(Post.class)
                                                                     .responseType(String.class)
                                                                     .operation(MutationType.CREATE)
                                                                     .requestOptions(new DefaultGraphQLRequestOptions())
                                                                     .build();

        Iterator<AuthorizationType> results =
            strategy.authTypesFor(request.getModelSchema(), request.getAuthRuleOperation());

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
            strategy.authTypesFor(ModelSchema.fromModelClass(Car.class), ModelOperation.READ);

        assertFalse(results.hasNext());
    }

    private void sortAndCompare(List<AuthRule> testSubject, List<AuthRule> expected) {
        Collections.sort(testSubject, compWithDefaultPriority);
        assertEquals(expected, testSubject);
    }
}
