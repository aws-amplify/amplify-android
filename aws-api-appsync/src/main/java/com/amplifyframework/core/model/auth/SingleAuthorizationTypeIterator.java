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
import java.util.Iterator;
import java.util.List;

/**
 * An iterator of authorization types backed by a single
 * authorization type. Essentially an implementation of an iterator to allow
 * for consistent invocations from the calling sites. It will be used for requests
 * that will be tried with a single authorization type.
 */
public class SingleAuthorizationTypeIterator implements Iterator<AuthorizationType> {
    private int currentIdx = 0;
    private final List<AuthorizationType> authorizationTypes;
    private AuthorizationType currentAuthorizationType;

    public SingleAuthorizationTypeIterator(AuthorizationType authorizationType) {
        this.authorizationTypes = Collections.singletonList(authorizationType);
    }

    @Override
    public boolean hasNext() {
        return currentIdx < authorizationTypes.size();
    }

    @Override
    public AuthorizationType next() {
        return authorizationTypes.get(currentIdx++);
    }

    @NonNull
    @Override
    public String toString() {
        return "SingleAuthorizationTypeIterator - " +
            "items(" + authorizationTypes.size() + ") - " +
            "[" + authorizationTypes.toString() + "] - " +
            "position:" + currentIdx;
    }
}
