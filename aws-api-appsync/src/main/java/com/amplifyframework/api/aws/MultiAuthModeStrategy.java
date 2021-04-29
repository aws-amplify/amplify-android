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

import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Authorization strategy that handle's multi-auth scenarios.
 */
public final class MultiAuthModeStrategy implements AuthModeStrategy {
    private static final List<AuthStrategy> DEFAULT_STRATEGY_PRIORITY = Arrays.asList(AuthStrategy.OWNER,
                                                                                      AuthStrategy.GROUPS,
                                                                                      AuthStrategy.PRIVATE,
                                                                                      AuthStrategy.PUBLIC);
    private static final Map<AuthStrategy, AuthorizationType> DEFAULT_AUTH_TYPES = new HashMap<>();

    static {
        DEFAULT_AUTH_TYPES.put(AuthStrategy.OWNER, AuthorizationType.AMAZON_COGNITO_USER_POOLS);
        DEFAULT_AUTH_TYPES.put(AuthStrategy.GROUPS, AuthorizationType.AMAZON_COGNITO_USER_POOLS);
        DEFAULT_AUTH_TYPES.put(AuthStrategy.PRIVATE, AuthorizationType.AMAZON_COGNITO_USER_POOLS);
        DEFAULT_AUTH_TYPES.put(AuthStrategy.PUBLIC, AuthorizationType.API_KEY);
    }

    private static final PriorityBasedAuthRuleProviderComparator DEFAULT_COMPARATOR =
        new PriorityBasedAuthRuleProviderComparator(DEFAULT_STRATEGY_PRIORITY);

    @Override
    public PriorityBasedAuthRuleIterator authTypesFor(ModelSchema modelSchema, ModelOperation operation) {
        final List<AuthRule> applicableRules = modelSchema.getApplicableRules(operation);
        Collections.sort(applicableRules, DEFAULT_COMPARATOR);
        return new PriorityBasedAuthRuleIterator(applicableRules);
    }

    @Override
    public AuthModeStrategyType getAuthorizationStrategyType() {
        return AuthModeStrategyType.MULTIAUTH;
    }

    static final class PriorityBasedAuthRuleProviderComparator implements Comparator<AuthRule> {
        private final List<AuthStrategy> strategyPriority;

        PriorityBasedAuthRuleProviderComparator() {
            this.strategyPriority = DEFAULT_STRATEGY_PRIORITY;
        }

        PriorityBasedAuthRuleProviderComparator(List<AuthStrategy> strategyPriority) {
            this.strategyPriority = strategyPriority;
        }

        @Override
        public int compare(AuthRule authRule1, AuthRule authRule2) {
            Integer o1Priority = Integer.valueOf(strategyPriority.indexOf(authRule1.getAuthStrategy()));
            Integer o2Priority = Integer.valueOf(strategyPriority.indexOf(authRule2.getAuthStrategy()));
            return o1Priority.compareTo(o2Priority);
        }
    }

    static final class PriorityBasedAuthRuleIterator implements Iterator<AuthorizationType> {
        private int currentIdx = 0;
        private AuthRule effectiveRule;
        private final List<AuthRule> authRules;

        PriorityBasedAuthRuleIterator(List<AuthRule> authRules) {
            this.authRules = authRules;
        }

        @Override
        public boolean hasNext() {
            return currentIdx < authRules.size();
        }

        @Override
        public AuthorizationType next() {
            effectiveRule = authRules.get(currentIdx++);
            //TODO: provider needs to be added to the
            return DEFAULT_AUTH_TYPES.get(effectiveRule.getAuthStrategy());
        }

        public AuthStrategy getAuthRuleStrategy() {
            return effectiveRule.getAuthStrategy();
        }
    }
}
