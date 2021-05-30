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

package com.amplifyframework.core.model.auth;

import androidx.annotation.NonNull;

import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An iterator of authorization types backed by a
 * list of auth rules. Auth rules should be sorted in which
 * they need to be evaluated.
 */
public final class MultiAuthorizationTypeIterator implements AuthorizationTypeIterator {
    private int currentIdx = 0;
    private AuthRule effectiveRule;
    private final List<AuthRule> authRules;

    /**
     * Constructor that takes a list of auth rules and uses the default comparator.
     * @param authRules The list of auth rules.
     */
    public MultiAuthorizationTypeIterator(List<AuthRule> authRules) {
        this(authRules, new PriorityBasedAuthRuleProviderComparator());
    }

    /**
     * Constructor that take a list of auth rules and an implementation of
     * the {@link Comparator} interface.
     * @param authRules The list of auth rules.
     * @param authRuleComparator The implementation of {@link Comparator} for sorting.
     */
    public MultiAuthorizationTypeIterator(List<AuthRule> authRules,
                                          Comparator<AuthRule> authRuleComparator) {
        Collections.sort(authRules, authRuleComparator);
        this.authRules = authRules;
    }

    @Override
    public boolean hasNext() {
        return currentIdx < authRules.size();
    }

    @Override
    public AuthorizationType next() {
        effectiveRule = authRules.get(currentIdx++);
        AuthStrategy.Provider authProvider = effectiveRule.getAuthProvider();
        return AuthorizationType.from(authProvider);
    }

    /**
     * Retrieves the {@link AuthStrategy} for the iterator's current rule.
     * @return The {@link AuthStrategy} of the currently selected {@link AuthRule}.
     * @throws IllegalStateException if attempting to access the current element before calling next.
     * for the first time.
     */
    public AuthStrategy getAuthRuleStrategy() {
        if (effectiveRule == null) {
            throw new IllegalStateException("No current item selected for the iterator.");
        }
        return effectiveRule.getAuthStrategy();
    }

    @Override
    public boolean isOwnerBasedRule() {
        if (effectiveRule == null) {
            return false;
        }
        return AuthStrategy.OWNER.equals(effectiveRule.getAuthStrategy());
    }

    @NonNull
    @Override
    public String toString() {
        return "PriorityBasedAuthRuleIterator - " +
            "items(" + authRules.size() + ") - " +
            "[" + authRules.toString() + "] - " +
            "position:" + currentIdx;
    }

    /**
     * An implementation of the comparator interface which uses {@link AuthStrategy}
     * in the comparison logic.
     */
    public static final class PriorityBasedAuthRuleProviderComparator implements Comparator<AuthRule> {
        private static final List<AuthStrategy> DEFAULT_STRATEGY_PRIORITY = Arrays.asList(AuthStrategy.OWNER,
                                                                                          AuthStrategy.GROUPS,
                                                                                          AuthStrategy.PRIVATE,
                                                                                          AuthStrategy.PUBLIC);
        private final List<AuthStrategy> strategyPriority;

        /**
         * Constructor that uses a default auth strategy order of owner, groups, private and public.
         */
        public PriorityBasedAuthRuleProviderComparator() {
            this(DEFAULT_STRATEGY_PRIORITY);
        }

        /**
         * Constructor that accepts a list of {@link AuthStrategy} which will be used to
         * sort auth rules.
         * @param strategyPriority A list of {@link AuthStrategy} used to sort the auth rules.
         */
        public PriorityBasedAuthRuleProviderComparator(List<AuthStrategy> strategyPriority) {
            this.strategyPriority = strategyPriority;
        }

        @Override
        public int compare(AuthRule authRule1, AuthRule authRule2) {
            Integer o1Priority = strategyPriority.indexOf(authRule1.getAuthStrategy());
            Integer o2Priority = strategyPriority.indexOf(authRule2.getAuthStrategy());
            return o1Priority.compareTo(o2Priority);
        }
    }
}
