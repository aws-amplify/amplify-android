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

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.appsync.GsonVariablesSerializer;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.OperationType;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.util.Casing;
import com.amplifyframework.util.Wrap;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.amplifyframework.util.Casing.CaseType.CAMEL_CASE;
import static com.amplifyframework.util.Casing.CaseType.PASCAL_CASE;
import static com.amplifyframework.util.Casing.CaseType.SCREAMING_SNAKE_CASE;

/**
 * A request against an AppSync GraphQL endpoint.
 * @param <R> The type of data contained in the GraphQLResponse expected from this request.
 */
public final class AppSyncGraphQLRequest<R> extends GraphQLRequest<R> {
    private static final int DEFAULT_DEPTH = 2;

    private final ModelSchema modelSchema;
    private final OperationType operationType;
    private final SelectionSet.Node selectionSet;
    private final Map<String, Object> variables;
    private final Map<String, String> variableTypes;

    /**
     * Constructor for AppSyncGraphQLRequest.
     * @throw AmplifyException if a ModelSchema can't be derived from the provided model class
     */
    private AppSyncGraphQLRequest(Builder builder) throws AmplifyException {
        super(builder.responseType, new GsonVariablesSerializer());
        this.modelSchema = ModelSchema.fromModelClass(builder.modelClass);
        SelectionSet.Node set = SelectionSet.fromModelClass(builder.modelClass, builder.operationType, DEFAULT_DEPTH);
        this.selectionSet = set;
        this.operationType = builder.operationType;
        this.variables = builder.variables;
        this.variableTypes = builder.variableTypes;
    }

    /**
     * Copy constructor for an AppSyncGraphQLRequest.
     * @param request request to copy.
     * @param <R> response type.
     */
    public <R> AppSyncGraphQLRequest(AppSyncGraphQLRequest<R> request) {
        super(request);
        this.modelSchema = request.modelSchema;
        this.operationType = request.operationType;
        this.selectionSet = new SelectionSet.Node(request.selectionSet);
        this.variables = new HashMap<>(request.variables);
        this.variableTypes = new HashMap<>(request.variableTypes);
    }

    @Override
    public <R> AppSyncGraphQLRequest<R> copy() {
        return new AppSyncGraphQLRequest<R>(this);
    }

    @Override
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    /**
     * Used for setting a variable on the request.
     * @param key variable name
     * @param type type of variable value
     * @param value variable value
     */
    public void setVariable(String key, String type, String value) {
        this.variables.put(key, value);
        this.variableTypes.put(key, type);
    }

    /**
     * Used to add owner argument from authProvider if needed.
     * @param authProvider CognitoUserPoolsAuthProvider for obtaining the username to set as the owner field.
     * @throws ApiException if request requires owner argument and authProvider or authProvider.getUsername() is null.
     */
    public void setOwner(CognitoUserPoolsAuthProvider authProvider) throws ApiException {
        for (AuthRule authRule : modelSchema.getAuthRules()) {
            if (isOwnerArgumentRequired(authRule.getOperationsOrDefault())) {
                setVariable(authRule.getOwnerFieldOrDefault(), "String!", getUsername(authProvider));
            }
        }
    }

    private String getUsername(CognitoUserPoolsAuthProvider authProvider) throws ApiException {
        if (authProvider == null) {
            throw new ApiException(
                    "Attempted to subscribe to a model with owner based authorization without a Cognito provider",
                    "Did you add the AWSCognitoAuthPlugin to Amplify before configuring it?"
            );
        }
        String username = authProvider.getUsername();
        if (username == null) {
            throw new ApiException(
                    "Attempted to subscribe to a model with owner based authorization without a username",
                    "Make sure that a user is logged in before subscribing to a model with owner based auth"
            );
        }
        return username;
    }

    private boolean isOwnerArgumentRequired(List<ModelOperation> operations) {
        if (SubscriptionType.ON_CREATE.equals(operationType) && operations.contains(ModelOperation.CREATE)) {
            return true;
        }
        if (SubscriptionType.ON_UPDATE.equals(operationType) && operations.contains(ModelOperation.UPDATE)) {
            return true;
        }
        if (SubscriptionType.ON_DELETE.equals(operationType) && operations.contains(ModelOperation.DELETE)) {
            return true;
        }
        return false;
    }

    /**
     *  Returns String value used for GraphQL "query" in HTTP request body.
     *
     *  Sample return value:
     *      subscription OnCreatePerson(owner: String!, nextToken: String) {
     *            onCreatePerson(owner: $owner, nextToken: $nextToken) {
     *               age dob first_name id last_name relationship owner
     *            }
     *       }
     *
     * @return String value used for GraphQL "query" in HTTP request body
     */
    @Override
    public String getQuery() {
        String inputTypeString = "";
        String inputParameterString = "";
        if (variableTypes.size() > 0) {
            List<String> inputKeys = new ArrayList<>(variableTypes.keySet());
            Collections.sort(inputKeys);

            List<String> inputTypes = new ArrayList<>();
            List<String> inputParameters = new ArrayList<>();
            for (String key : inputKeys) {
                inputTypes.add("$" + key + ": " + variableTypes.get(key));
                inputParameters.add(key + ": $" + key);
            }

            inputTypeString = Wrap.inParentheses(TextUtils.join(", ", inputTypes));
            inputParameterString = Wrap.inParentheses(TextUtils.join(", ", inputParameters));
        }

        String modelName = modelSchema.getName();
        String operationString = new StringBuilder()
                .append(Casing.from(SCREAMING_SNAKE_CASE).to(CAMEL_CASE).convert(operationType.toString()))
                .append(modelName)
                .append(QueryType.LIST.equals(operationType) ? "s" : "")
                .append(inputParameterString)
                .append(selectionSet.toString())
                .toString();

        String queryString = new StringBuilder()
                .append(operationType.getOperationName())
                .append(" ")
                .append(Casing.from(SCREAMING_SNAKE_CASE).to(PASCAL_CASE).convert(operationType.toString()))
                .append(modelName)
                .append(inputTypeString)
                .append(" ")
                .append(Wrap.inBraces(operationString))
                .toString();

        return queryString;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        if (!super.equals(object)) {
            return false;
        }
        AppSyncGraphQLRequest<?> that = (AppSyncGraphQLRequest<?>) object;
        return ObjectsCompat.equals(modelSchema, that.modelSchema) &&
                ObjectsCompat.equals(operationType, that.operationType) &&
                ObjectsCompat.equals(selectionSet, that.selectionSet) &&
                ObjectsCompat.equals(variables, that.variables) &&
                ObjectsCompat.equals(variableTypes, that.variableTypes);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(super.hashCode(), modelSchema, operationType, selectionSet, variables, variableTypes);
    }

    @Override
    public String toString() {
        return "AppSyncGraphQLRequest{" +
                "modelSchema=" + modelSchema +
                ", operationType=" + operationType +
                ", selectionSet=" + selectionSet +
                ", variables=" + variables +
                ", variableTypes=" + variableTypes +
                ", responseType=" + getResponseType() +
                ", variablesSerializer=" + getVariablesSerializer() +
                '}';
    }

    /**
     * Create a new AppSyncGraphQLRequest builder.
     * @return a new AppSyncGraphQLRequest builder.
     */
    public static AppSyncGraphQLRequest.Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private Class<? extends Model> modelClass;
        private OperationType operationType;
        private Type responseType;
        private final Map<String, Object> variables;
        private final Map<String, String> variableTypes;

        Builder() {
            this.variables = new HashMap<>();
            this.variableTypes = new HashMap<>();
        }

        Builder modelClass(@NonNull Class<? extends Model> modelClass) {
            this.modelClass = Objects.requireNonNull(modelClass);
            return Builder.this;
        }

        Builder operationType(@NonNull OperationType operationType) {
            this.operationType = Objects.requireNonNull(operationType);
            return Builder.this;
        }

        Builder responseType(@NonNull Type responseType) {
            this.responseType = Objects.requireNonNull(responseType);
            return Builder.this;
        }

        Builder setVariable(@NonNull String key, String type, Object value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(type);
            this.variables.put(key, value);
            this.variableTypes.put(key, type);
            return this;
        }

        <R> AppSyncGraphQLRequest<R> build() throws AmplifyException {
            Objects.requireNonNull(this.operationType);
            Objects.requireNonNull(this.modelClass);
            Objects.requireNonNull(this.responseType);
            return new AppSyncGraphQLRequest<>(this);
        }
    }
}
