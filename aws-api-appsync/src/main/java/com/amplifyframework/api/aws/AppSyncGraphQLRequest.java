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
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.Operation;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.util.Casing;
import com.amplifyframework.util.Immutable;
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
    private final AuthorizationType authorizationType;
    private final AuthModeStrategyType authModeStrategyType;

    /**
     * Constructor for AppSyncGraphQLRequest.
     */
    private AppSyncGraphQLRequest(Builder builder) {
        super(builder.responseType, new GsonVariablesSerializer());
        this.modelSchema = builder.modelSchema;
        this.operation = builder.operation;
        this.selectionSet = builder.selectionSet;
        this.variables = Immutable.of(builder.variables);
        this.variableTypes = Immutable.of(builder.variableTypes);
        this.authorizationType = builder.authorizationType;
        this.authModeStrategyType = builder.authModeStrategyType;
    }

    /**
     * Returns the {@link ModelSchema} for this request.
     * @return the {@link ModelSchema} for this request.
     */
    public ModelSchema getModelSchema() {
        return modelSchema;
    }

    /**
     * Returns the {@link Operation} for this request.
     * @return the {@link Operation} for this request.
     */
    public Operation getOperation() {
        return operation;
    }

    @Override
    public Map<String, Object> getVariables() {
        return Immutable.of(variables);
    }

    /**
     * Returns the {@link AuthorizationType} for this request.
     * @return the {@link AuthorizationType} for this request.
     */
    public AuthorizationType getAuthorizationType() {
        return authorizationType;
    }

    /**
     * Returns the {@link AuthModeStrategyType} for this request.
     * @return the {@link AuthModeStrategyType} for this request.
     */
    public AuthModeStrategyType getAuthModeStrategyType() {
        return authModeStrategyType;
    }

    /**
     * Returns the {@link ModelOperation} for this request. This is used to during
     * auth rule evaluation.
     * @return the {@link ModelOperation} for this request.
     * @throws IllegalArgumentException if unable to map the request's {@link #getOperation()} to one of
     *          the enum values in {@link ModelOperation}
     */
    public ModelOperation getAuthRuleOperation() {
        switch (this.getOperation().getOperationType()) {
            case QUERY:
            case SUBSCRIPTION:
                return ModelOperation.READ;
            case MUTATION:
                MutationType mutationType = (MutationType) this.getOperation();
                return ModelOperation.valueOf(mutationType.name());
            default:
                throw new IllegalArgumentException("Invalid graphql operation type:"
                                                       + this.getOperation().getOperationType());
        }
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
        if (QueryType.LIST.equals(operation)) {
            modelName = modelSchema.getListPluralName() != null
                    && !modelSchema.getListPluralName().isEmpty()
                    ? modelSchema.getListPluralName()
                    : modelName + "s";
        } else if (QueryType.SYNC.equals(operation)) {
            // The sync operation name is pluralized using pluralize.js, which uses more complex pluralization rules
            // than simply adding an 's' at the end (e.g. baby > babies, person > people, etc).  This pluralized name
            // is an annotation on the codegen'd model class, so we will just grab it from the ModelSchema.
            modelName = modelSchema.getSyncPluralName() != null
                        && modelSchema.getSyncPluralName().isEmpty()
                    ? Casing.capitalizeFirst(modelSchema.getPluralName())
                    : Casing.capitalizeFirst(modelSchema.getSyncPluralName());
        }

        String operationString =
                Casing.from(Casing.CaseType.SCREAMING_SNAKE_CASE)
                    .to(Casing.CaseType.CAMEL_CASE)
                    .convert(operation.toString()) +
                modelName +
                inputParameterString +
                selectionSet.toString("  ");

        return operation.getOperationType().getName() +
                " " +
                Casing.from(Casing.CaseType.SCREAMING_SNAKE_CASE).to(Casing.CaseType.PASCAL_CASE)
                    .convert(operation.toString()) +
                modelName +
                inputTypeString +
                Wrap.inPrettyBraces(operationString, "", "  ") +
                "\n";
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
                ", operation=" + operation +
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
     * Creates an AppSyncGraphQLRequest builder from the current AppSyncGraphQLRequest instance.
     * @return an AppSyncGraphQLRequest builder from the current AppSyncGraphQLRequest instance.
     */
    public AppSyncGraphQLRequest.Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Builder for a AppSyncGraphQLRequest.
     */
    public static final class Builder {
        private AuthModeStrategyType authModeStrategyType;
        private Class<? extends Model> modelClass;
        private ModelSchema modelSchema;
        private Operation operation;
        private GraphQLRequestOptions requestOptions;
        private Type responseType;
        private SelectionSet selectionSet;
        private AuthorizationType authorizationType;
        private final Map<String, Object> variables;
        private final Map<String, String> variableTypes;

        Builder() {
            this.variables = new HashMap<>();
            this.variableTypes = new HashMap<>();
        }

        <R> Builder(AppSyncGraphQLRequest<R> request) {
            this.modelSchema = request.modelSchema;
            this.operation = request.operation;
            this.responseType = request.getResponseType();
            this.selectionSet = new SelectionSet(request.selectionSet);
            this.variables = new HashMap<>(request.variables);
            this.variableTypes = new HashMap<>(request.variableTypes);
            this.authorizationType = request.authorizationType;
            this.authModeStrategyType = request.authModeStrategyType;
        }

        /**
         * Sets the {@link ModelSchema} Class and returns this builder.
         * @param modelSchema the {@link ModelSchema} Class.
         * @return this builder instance.
         */
        public Builder modelSchema(@NonNull ModelSchema modelSchema) {
            this.modelSchema = Objects.requireNonNull(modelSchema);
            return Builder.this;
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
        public Builder operation(@NonNull Operation operation) {
            this.operation = Objects.requireNonNull(operation);
            return Builder.this;
        }

        /**
         * Sets the requestOptions and returns this builder.
         * @param requestOptions options defining how the request should be built.
         * @return this builder instance.
         */
        public Builder requestOptions(@NonNull GraphQLRequestOptions requestOptions) {
            this.requestOptions = Objects.requireNonNull(requestOptions);
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
         * Sets the selectionSet and returns this builder.
         * @param selectionSet the subset of model for this request to operate on.
         * @return this builder instance.
         */
        public Builder selectionSet(@NonNull SelectionSet selectionSet) {
            this.selectionSet = Objects.requireNonNull(selectionSet);
            return Builder.this;
        }

        /**
         * Sets the authorization type for the request. If this field is set,
         * {@link Builder#authModeStrategyType} will be ignored.
         * @param authorizationType the desired authorization type.
         * @return this builder instance.
         */
        public Builder authorizationType(@NonNull AuthorizationType authorizationType) {
            this.authorizationType = Objects.requireNonNull(authorizationType);
            return Builder.this;
        }

        /**
         * Sets the authorization type for the request.
         * @param strategyType the desired request authorization strategy type.
         * @return this builder instance.
         */
        public Builder requestAuthorizationStrategyType(@NonNull AuthModeStrategyType strategyType) {
            this.authModeStrategyType = Objects.requireNonNull(strategyType);
            return Builder.this;
        }

        /**
         * Sets a variable and returns this builder.
         * @param key the variable key.
         * @param type the variable type (e.g. ID! or String!).
         * @param value the variable value.
         * @return this builder instance.
         */
        public Builder variable(@NonNull String key, String type, Object value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(type);
            this.variables.put(key, value);
            this.variableTypes.put(key, type);
            return this;
        }

        /**
         * Builds an {@link AppSyncGraphQLRequest}.
         * @param <R> The type of data contained in the GraphQLResponse expected from this request.
         * @return the AppSyncGraphQLRequest
         * @throws AmplifyException if a ModelSchema cannot be created from the provided model class.
         */
        public <R> AppSyncGraphQLRequest<R> build() throws AmplifyException {
            Objects.requireNonNull(this.operation);
            Objects.requireNonNull(this.responseType);

            // TODO: if the modelClass is contained within the modelSchema,
            // why can't we just extract it from the ModelSchema, instead of
            // having it as an outside parameter?
            if (modelSchema == null) {
                if (modelClass == null) {
                    throw new AmplifyException(
                        "Both modelSchema and modelClass cannot be null",
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    );
                }
                // Derive modelSchema from modelClass if not available
                modelSchema = ModelSchema.fromModelClass(this.modelClass);
            }

            // if this Builder was created via newBuilder(),
            // selectionSet will already be set, so we can continue on.
            if (selectionSet == null) {
                selectionSet = SelectionSet.builder()
                        .modelSchema(this.modelSchema)
                        .modelClass(this.modelClass)
                        .operation(this.operation)
                        .requestOptions(Objects.requireNonNull(this.requestOptions))
                        .build();
            }

            if (authModeStrategyType == null || authorizationType != null) {
                authModeStrategyType = AuthModeStrategyType.DEFAULT;
            }

            return new AppSyncGraphQLRequest<>(this);
        }
    }
}
