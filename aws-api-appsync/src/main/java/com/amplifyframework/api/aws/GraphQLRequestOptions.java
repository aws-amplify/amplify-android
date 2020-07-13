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

package com.amplifyframework.api.aws;

import com.amplifyframework.core.model.Model;

import java.util.List;

/**
 * Options to be provided to a {@link AppSyncGraphQLRequest.Builder}.  When provisioning an AppSync API, there are some
 * key differences in the way the GraphQL operations are generated depending on whether DataStore is enabled.  For
 * example, _deleted, _lastChangedAt, and _version fields are added to each {@link Model} when using DataStore, but not
 * when using API, which means a GraphQLRequest should request these fields only if DataStore is enabled.  This options
 * object encapsulates these differences to help build the {@link AppSyncGraphQLRequest}.
 */
public interface GraphQLRequestOptions {

    /**
     * Returns list of fields to be requested when the response type is a list of {@link Model} objects.
     * Examples: "nextToken",  "startedAt".
     *
     * @return list of pagination fields.
     */
    List<String> paginationFields();

    /**
     * Returns list of metadata fields that should be requested for a {@link Model}.
     * Examples:  "_deleted", "_lastChangedAt", "_version".
     *
     * @return list of metadata fields
     */
    List<String> modelMetaFields();

    /**
     * Returns name of field that should wrap {@link Model} fields.
     * Example: "items".
     *
     * @return name of field that should wrap {@link Model} fields.
     */
    String listField();

    /**
     * Returns the maximum depth to traverse a {@link Model} when building the {@link SelectionSet}.
     * @return the maximum depth to traverse a {@link Model} when building the {@link SelectionSet}.
     */
    int maxDepth();

    /**
     * Returns whether to request all fields or only the id field for leaf nodes when building the {@link SelectionSet}.
     * @return whether to request all fields or only the id field for leaf nodes when building the {@link SelectionSet}.
     */
    boolean onlyRequestIdForLeafSelectionSetNodes();
}
