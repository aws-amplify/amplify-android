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

import com.amplifyframework.api.aws.AuthorizationType;

import java.util.Iterator;

/**
 * An interface that extends {@link Iterator} with additional capabilities
 * needed to determine whether to execute certain actions at run time. For instance,
 * there has to be a way to determine whether the {@link AuthorizationType} the iterator
 * is currently returning is from an owner-based rule.
 */
public interface AuthorizationTypeIterator extends Iterator<AuthorizationType> {
    /**
     * Returns true if the rule associated with the current element of the iterator
     * is associated with an owner-based rule.
     * @return True, if associated with an owner rule, false otherwise.
     */
    boolean isOwnerBasedRule();
}
