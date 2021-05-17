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

package com.amplifyframework.core.model.types;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.auth.AuthorizationTypeIterator;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AuthorizationTypeIteratorTest {
    private AuthorizationTypeIterator.PriorityBasedAuthRuleProviderComparator compWithDefaultPriority;

    /**
     * Setup test fixtures.
     */
    @Before
    public void setup() {
        compWithDefaultPriority = new AuthorizationTypeIterator.PriorityBasedAuthRuleProviderComparator();
    }

    /**
     * Basic test for the comparator.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testAuthStrategyComparatorBasic() throws AmplifyException {
        List<AuthRule> expected = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.OWNER).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build()
        );
        sortAndCompare(PublicOwner.class, expected);
    }

    /**
     * Verify that comparison of lists of different sizes fails.
     * @throws AmplifyException Not expected.
     */
    @Test (expected = AssertionError.class)
    public void testAuthStrategyComparatorDiffSize() throws AmplifyException {
        List<AuthRule> expected = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.OWNER).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PRIVATE).build()
        );

        /*List<AuthRule> authRules = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build()
        );*/
        sortAndCompare(PublicOwner.class, expected);
    }

    /**
     * Test using all 4 types in random order.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void testAuthStrategyComparatorScrambled() throws AmplifyException {
        List<AuthRule> expected = Arrays.asList(
            AuthRule.builder().authStrategy(AuthStrategy.OWNER).build(),
            AuthRule.builder().authStrategy(AuthStrategy.GROUPS).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PRIVATE).build(),
            AuthRule.builder().authStrategy(AuthStrategy.PUBLIC).build()
        );
        sortAndCompare(Scrambled.class, expected);
    }

    private void sortAndCompare(Class<? extends Model> clazz, List<AuthRule> expected) throws AmplifyException {
        ModelSchema modelSchema = ModelSchema.fromModelClass(clazz);
        List<AuthRule> rulesFromModelSchema = modelSchema.getAuthRules();
        if (rulesFromModelSchema.size() != expected.size()) {
            fail("Tried to compare two auth rule lists of different sizes.");
            return;
        }
        Collections.sort(rulesFromModelSchema, compWithDefaultPriority);
        int idx = 0;
        for (AuthRule rule : rulesFromModelSchema) {
            assertEquals(expected.get(idx).getAuthStrategy(), rule.getAuthStrategy());
            idx++;
        }
    }

    @ModelConfig(authRules = {
        @com.amplifyframework.core.model.annotations.AuthRule(allow = AuthStrategy.PUBLIC),
        @com.amplifyframework.core.model.annotations.AuthRule(allow = AuthStrategy.OWNER)
    })
    abstract class PublicOwner implements Model {}

    @ModelConfig(authRules = {
        @com.amplifyframework.core.model.annotations.AuthRule(allow = AuthStrategy.PUBLIC),
        @com.amplifyframework.core.model.annotations.AuthRule(allow = AuthStrategy.OWNER),
        @com.amplifyframework.core.model.annotations.AuthRule(allow = AuthStrategy.PRIVATE),
        @com.amplifyframework.core.model.annotations.AuthRule(allow = AuthStrategy.GROUPS)
    })
    abstract class Scrambled implements Model {}

}
