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

import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.Operation;
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
public final class MultiAuthRequestAuthorizationStrategy implements RequestAuthorizationStrategy {
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
    public Iterator<AuthorizationType> authTypesFor(ModelSchema modelSchema, ModelOperation operation) {
        final List<AuthRule> applicableRules = modelSchema.getApplicableRules(operation);
        Collections.sort(applicableRules, DEFAULT_COMPARATOR);

        return new Iterator<AuthorizationType>() {
            private int currentIdx = 0;
            @Override
            public boolean hasNext() {
                return currentIdx < applicableRules.size();
            }

            @Override
            public AuthorizationType next() {
                AuthRule effectiveRule = applicableRules.get(currentIdx++);
                //TODO: provider needs to be added to the
                return DEFAULT_AUTH_TYPES.get(effectiveRule.getAuthStrategy());
            }
        };
    }

    @Override
    public Iterator<AuthorizationType> authTypesFor(AppSyncGraphQLRequest<?> appSyncGraphQLRequest) {
        Operation graphqlOperation = appSyncGraphQLRequest.getOperation();
        switch (graphqlOperation.getOperationType()) {
            case QUERY:
            case SUBSCRIPTION:
                return authTypesFor(appSyncGraphQLRequest.getModelSchema(), ModelOperation.READ);
            case MUTATION:
                MutationType mutationType = (MutationType) graphqlOperation;
                return authTypesFor(appSyncGraphQLRequest.getModelSchema(),
                                    ModelOperation.valueOf(mutationType.name()));
            default:
                throw new IllegalArgumentException("Invalid graphql operation type:"
                                                       + graphqlOperation.getOperationType());

        }
    }

    @Override
    public RequestAuthorizationStrategyType getAuthorizationStrategyType() {
        return RequestAuthorizationStrategyType.MULTIAUTH;
    }

    private static final class PriorityBasedAuthRuleProviderComparator implements Comparator<AuthRule> {
        private final List<AuthStrategy> strategyPriority;

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
}
