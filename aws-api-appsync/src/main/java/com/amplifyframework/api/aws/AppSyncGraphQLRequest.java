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
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.Operation;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
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

/**
 * A request against an AppSync GraphQL endpoint.
 * @param <R> The type of data contained in the GraphQLResponse expected from this request.
 */
public final class AppSyncGraphQLRequest<R> extends GraphQLRequest<R> {
    private final ModelSchema modelSchema;
    private final Operation operation;
    private final SelectionSet selectionSet;
    private final Map<String, Object> variables;
    private final Map<String, String> variableTypes;

    /**
     * Constructor for AppSyncGraphQLRequest.
     * @throw AmplifyException if a ModelSchema can't be derived from the provided model class
     */
    private AppSyncGraphQLRequest(Builder builder) throws AmplifyException {
        super(builder.responseType, new GsonVariablesSerializer());
        this.modelSchema = ModelSchema.fromModelClass(builder.modelClass);
        this.selectionSet = SelectionSet.builder()
                .modelClass(builder.modelClass)
                .operation(builder.operation)
                .requestOptions(builder.requestOptions)
                .build();
        this.operation = builder.operation;
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
        this.operation = request.operation;
        this.selectionSet = new SelectionSet(request.selectionSet);
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
     * Returns whether owner argument is required for this request based on the model's {@link AuthRule}s.
     * @return whether owner argument is required for this request based on the model's {@link AuthRule}s.
     */
    public boolean isOwnerArgumentRequired() {
        for (AuthRule authRule : modelSchema.getAuthRules()) {
            if (isOwnerArgumentRequired(authRule)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Used to add owner argument from authProvider if needed.
     * @param owner username from CognitoUserPoolsAuthProvider to set as the owner field.
     * @throws ApiException if request requires owner argument and authProvider or authProvider.getUsername() is null.
     */
    public void setOwner(String owner) {
        for (AuthRule authRule : modelSchema.getAuthRules()) {
            if (isOwnerArgumentRequired(authRule)) {
                setVariable(authRule.getOwnerFieldOrDefault(), "String!", owner);
            }
        }
    }

    private boolean isOwnerArgumentRequired(AuthRule authRule) {
        if (!AuthStrategy.OWNER.equals(authRule.getAuthStrategy())) {
            return false;
        }
        List<ModelOperation> operations = authRule.getOperationsOrDefault();
        if (SubscriptionType.ON_CREATE.equals(operation) && operations.contains(ModelOperation.CREATE)) {
            return true;
        }
        if (SubscriptionType.ON_UPDATE.equals(operation) && operations.contains(ModelOperation.UPDATE)) {
            return true;
        }
        if (SubscriptionType.ON_DELETE.equals(operation) && operations.contains(ModelOperation.DELETE)) {
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

        String modelName = Casing.capitalizeFirst(modelSchema.getName());

        String operationString = new StringBuilder()
                .append(Casing.from(Casing.CaseType.SCREAMING_SNAKE_CASE).to(Casing.CaseType.CAMEL_CASE)
                        .convert(operation.toString()))
                .append(modelName)
                .append(QueryType.LIST.equals(operation) ? "s" : "")
                .append(inputParameterString)
                .append(selectionSet.toString("  "))
                .toString();

        String queryString = new StringBuilder()
                .append(operation.getOperationType().getName())
                .append(" ")
                .append(Casing.from(Casing.CaseType.SCREAMING_SNAKE_CASE).to(Casing.CaseType.PASCAL_CASE)
                        .convert(operation.toString()))
                .append(modelName)
                .append(inputTypeString)
                .append(Wrap.inPrettyBraces(operationString, "", "  "))
                .append("\n")
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
                ObjectsCompat.equals(operation, that.operation) &&
                ObjectsCompat.equals(selectionSet, that.selectionSet) &&
                ObjectsCompat.equals(variables, that.variables) &&
                ObjectsCompat.equals(variableTypes, that.variableTypes);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(super.hashCode(), modelSchema, operation, selectionSet, variables, variableTypes);
    }

    @Override
    public String toString() {
        return "AppSyncGraphQLRequest{" +
                "modelSchema=" + modelSchema +
                ", operationType=" + operation +
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

    /**
     * Builder for a AppSyncGraphQLRequest.
     */
    public static final class Builder {
        private Class<? extends Model> modelClass;
        private GraphQLRequestOptions requestOptions;
        private Operation operation;
        private Type responseType;
        private final Map<String, Object> variables;
        private final Map<String, String> variableTypes;

        Builder() {
            this.variables = new HashMap<>();
            this.variableTypes = new HashMap<>();
        }

        /**
         * Sets the {@link Model} Class and returns this builder.
         * @param modelClass the {@link Model} Class.
         * @return this builder instance.
         */
        public Builder modelClass(@NonNull Class<? extends Model> modelClass) {
            this.modelClass = Objects.requireNonNull(modelClass);
            return Builder.this;
        }

        /**
         * Sets the operation and returns this builder.
         * @param operation the Operation.
         * @return this builder instance.
         */
        Builder operation(@NonNull Operation operation) {
            this.operation = Objects.requireNonNull(operation);
            return Builder.this;
        }

        /**
         * Sets the responseType and returns this builder.
         * @param responseType the expected object Type of the response generated by this request.
         * @return this builder instance.
         */
        public Builder responseType(@NonNull Type responseType) {
            this.responseType = Objects.requireNonNull(responseType);
            return Builder.this;
        }

        /**
         * Sets the requestOptions and returns this builder.
         * @param requestOptions the requestOptions describing how to build the {@link AppSyncGraphQLRequest}.
         * @return this builder instance.
         */
        public Builder requestOptions(@NonNull GraphQLRequestOptions requestOptions) {
            this.requestOptions = Objects.requireNonNull(requestOptions);
            return Builder.this;
        }

        /**
         * Sets a variable and returns this builder.
         * @param key the variable key.
         * @param type the variable type (e.g. ID! or String!).
         * @param value the variable value.
         * @return this builder instance.
         */
        public Builder setVariable(@NonNull String key, String type, Object value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(type);
            this.variables.put(key, value);
            this.variableTypes.put(key, type);
            return this;
        }

        /**
         * Builds the {@link AppSyncGraphQLRequest} containing all of the fields of the provided model class.
         * @param <R> The type of data contained in the GraphQLResponse expected from this request.
         * @return the AppSyncGraphQLRequest
         * @throws AmplifyException if a ModelSchema cannot be created from the provided model class.
         */
        <R> AppSyncGraphQLRequest<R> build() throws AmplifyException {
            Objects.requireNonNull(this.operation);
            Objects.requireNonNull(this.modelClass);
            Objects.requireNonNull(this.responseType);
            Objects.requireNonNull(this.requestOptions);
            return new AppSyncGraphQLRequest<>(this);
        }
    }
}
