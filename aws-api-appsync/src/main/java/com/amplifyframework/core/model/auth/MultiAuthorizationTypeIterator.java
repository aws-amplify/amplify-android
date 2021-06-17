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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * An iterator of authorization types backed by a
 * list of auth rules. Auth rules should be sorted in which
 * they need to be evaluated.
 */
public final class MultiAuthorizationTypeIterator implements AuthorizationTypeIterator {
    private int currentPosition = 0;
    private AuthRule effectiveRule;
    private final List<AuthRule> authRules;
    private final Iterator<AuthRule> dedupedIterator;

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
        this.authRules = authRules;
        Set<AuthRule> dedupedRules = new TreeSet<>(authRuleComparator);
        dedupedRules.addAll(authRules);
        dedupedIterator = dedupedRules.iterator();
    }

    @Override
    public boolean hasNext() {
        return dedupedIterator.hasNext();
    }

    @Override
    public AuthorizationType next() {
        effectiveRule = dedupedIterator.next();
        AuthStrategy.Provider authProvider = effectiveRule.getAuthProvider();
        currentPosition++;
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
            "position:" + currentPosition;
    }

    /**
     * An implementation of the comparator interface which uses {@link AuthStrategy}
     * in the comparison logic.
     */
    private static final class PriorityBasedAuthRuleProviderComparator implements Comparator<AuthRule> {
        @Override
        public int compare(AuthRule authRule1, AuthRule authRule2) {
            int o1Priority = authRule1.getAuthStrategy().getPriority();
            int o2Priority = authRule2.getAuthStrategy().getPriority();
            int result = Integer.compare(o1Priority, o2Priority);
            // If strategies are the same, rank by provider precedence.
            if (result == 0) {
                o1Priority = authRule1.getAuthProvider().getPriority();
                o2Priority = authRule2.getAuthProvider().getPriority();
                result = Integer.compare(o1Priority, o2Priority);
            }
            return result;
        }
    }
}
