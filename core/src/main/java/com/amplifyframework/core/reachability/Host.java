/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.reachability;

/**
 * A host is a model of some remote system that we may or may not be able
 * to reach over a network. All we really care about is whether or not
 * we have some method to determine whether or not the host is reachable.
 */
public interface Host {

    /**
     * Checks if there a way to reach the host.
     * @return true if host is reachable, false otherwise
     */
    boolean isReachable();
}
