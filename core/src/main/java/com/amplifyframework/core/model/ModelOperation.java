/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model;

/**
 * Used as a property of {@link AuthRule} to specify which operations the rule applies to.
 * @see <a href="https://docs.amplify.aws/cli/graphql-transformer/directives#auth">GraphQL Transformer @auth directive
 * documentation.</a>
 */
public enum ModelOperation {

    /**
     * Relates to a create mutation.
     */
    CREATE,

    /**
     * Relates to an update mutation.
     */
    UPDATE,

    /**
     * Relates to a delete mutation.
     */
    DELETE,

    /**
     * Relates to API queries (all read operations).
     */
    READ,

    /**
     * Relates to `get` API query.
     */
    GET,

    /**
     * Relates to `list` API query.
     */
    LIST,

    /**
     * Relates to `sync` API query.
     */
    SYNC,

    /**
     * Relates to `subscribe` API query.
     */
    LISTEN,

    /**
     * Relates to `search` API query.
     */
    SEARCH
}
