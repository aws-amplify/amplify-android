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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.auth.MultiAuthorizationTypeIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Authorization strategy that handle's multi-auth scenarios. It derives its
 * results @auth rule metadata from model schemas provided via the constructor.
 */
public final class MultiAuthModeStrategy implements AuthModeStrategy {
    private static MultiAuthModeStrategy instance;

    private MultiAuthModeStrategy() {}

    /**
     * Retrieve the singleton instance of the multi-auth strategy implementation class.
     * @return A reference to the multi-auth strategy singleton component.
     */
    public static synchronized MultiAuthModeStrategy getInstance() {
        if (instance == null) {
            instance = new MultiAuthModeStrategy();
        }
        return instance;
    }

    @Override
    public Iterator<AuthorizationType> authTypesFor(@NonNull ModelSchema modelSchema,
                                 @NonNull ModelOperation operation) {
        final List<AuthRule> applicableRules = new ArrayList<>();
        Consumer<List<AuthRule>> filterAuthRules = authRules -> {
            for (AuthRule rule : authRules) {
                if (rule.getOperationsOrDefault().contains(operation)) {
                    applicableRules.add(rule);
                }
            }
        };
        filterAuthRules.accept(modelSchema.getAuthRules());
        for (ModelField field : modelSchema.getFields().values()) {
            filterAuthRules.accept(field.getAuthRules());
        }
        return new MultiAuthorizationTypeIterator(applicableRules);
    }
}
