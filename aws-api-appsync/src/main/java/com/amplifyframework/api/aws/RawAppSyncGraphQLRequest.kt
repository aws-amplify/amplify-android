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
package com.amplifyframework.api.aws

import androidx.core.util.ObjectsCompat
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import java.lang.reflect.Type

/**
 * A [GraphQLRequest] built from a raw GraphQL document, which can specify the [AuthorizationType]
 * used to authorize it.
 *
 * This is the equivalent of [SimpleGraphQLRequest] for APIs that define more than one authorization
 * mode. [SimpleGraphQLRequest] always uses the API's default authorization type; this request type
 * lets you select any of the modes configured for the API, per request:
 *
 * ```kotlin
 * val request = RawAppSyncGraphQLRequest<String>(
 *     "query GetByUser { getByUser }",
 *     String::class.java,
 *     serializer,
 *     AuthorizationType.AMAZON_COGNITO_USER_POOLS
 * )
 * ```
 *
 * Because a raw document has no associated model schema, a request of this type does not participate
 * in automatic multi-auth resolution from `@auth` rules. The authorization type given here is used
 * as-is.
 *
 * @param R Type of R, the data contained in the GraphQLResponse expected from this request
 * @param document query document to process
 * @param variables variables to be added
 * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
 * @param variablesSerializer an object which can take a map of variables and serialize it properly
 * @param authorizationType the authorization type to use for this request, or null for the API default
 */
class RawAppSyncGraphQLRequest<R>(
    document: String,
    variables: Map<String, Any>,
    responseType: Type,
    variablesSerializer: VariablesSerializer,
    override val authorizationType: AuthorizationType?
) : GraphQLRequest<R>(responseType, variablesSerializer), AuthorizedGraphQLRequest {

    private val document: String = document
    private val variables: Map<String, Any> = variables

    /**
     * Constructor for a request with no variables.
     * @param document document String for request
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     * @param authorizationType the authorization type to use for this request, or null for the API default
     */
    constructor(
        document: String,
        responseType: Type,
        variablesSerializer: VariablesSerializer,
        authorizationType: AuthorizationType?
    ) : this(document, emptyMap(), responseType, variablesSerializer, authorizationType)

    override fun getQuery(): String = document

    override fun getVariables(): Map<String, Any> = variables

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        if (!super.equals(other)) {
            return false
        }
        val that = other as RawAppSyncGraphQLRequest<*>
        return document == that.document &&
            variables == that.variables &&
            authorizationType == that.authorizationType
    }

    override fun hashCode(): Int = ObjectsCompat.hash(super.hashCode(), document, variables, authorizationType)

    override fun toString(): String = "RawAppSyncGraphQLRequest{" +
        "document='$document'" +
        ", variables='$variables'" +
        ", authorizationType='$authorizationType'" +
        "}"
}
