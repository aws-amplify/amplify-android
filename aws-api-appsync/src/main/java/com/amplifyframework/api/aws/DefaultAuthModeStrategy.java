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

import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;

import java.util.Iterator;

/**
 * Default authorization strategy that only uses the value provided to the constructor.
 */
final class DefaultAuthModeStrategy implements AuthModeStrategy {

    private final AuthorizationType defaultAuthorizationType;

    DefaultAuthModeStrategy(AuthorizationType defaultAuthorizationType) {
        this.defaultAuthorizationType = defaultAuthorizationType;
    }

    @Override
    public Iterator<AuthorizationType> authTypesFor(ModelSchema modelSchema, ModelOperation operation) {
        return new DefaultAuthorizationStrategyIterator(defaultAuthorizationType);
    }

    @Override
    public Iterator<AuthorizationType> authTypesFor(AppSyncGraphQLRequest<?> appSyncGraphQLRequest) {
        return new DefaultAuthorizationStrategyIterator(defaultAuthorizationType);
    }

    @Override
    public AuthModeStrategyType getAuthorizationStrategyType() {
        return AuthModeStrategyType.DEFAULT;
    }

    static final class DefaultAuthorizationStrategyIterator implements Iterator<AuthorizationType> {
        private final AuthorizationType[] authorizationTypes;
        private int currentIdx = 0;

        DefaultAuthorizationStrategyIterator(AuthorizationType defaultAuthorizationType) {
            this.authorizationTypes = new AuthorizationType[] {defaultAuthorizationType};
        }

        @Override
        public boolean hasNext() {
            return currentIdx < authorizationTypes.length;
        }

        @Override
        public AuthorizationType next() {
            return authorizationTypes[currentIdx++];
        }
    }
}
