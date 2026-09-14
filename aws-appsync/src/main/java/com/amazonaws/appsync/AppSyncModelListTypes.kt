/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelPage
import com.amplifyframework.core.model.PaginationToken

internal class AppSyncLoadedModelList<out M : Model>(override val items: List<M>) : LoadedModelList<M>

/** A null [nextToken] is what makes [ModelPage.hasNextPage] false. */
internal class AppSyncModelPage<out M : Model>(
    override val items: List<M>,
    override val nextToken: AppSyncPaginationToken?
) : ModelPage<M>

/** An opaque AppSync cursor, meaningful only to the service, that a caller hands back unchanged. */
internal class AppSyncPaginationToken(val nextToken: String) : PaginationToken
