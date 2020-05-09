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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.util.Casing;
import com.amplifyframework.util.FieldFinder;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A factory to generate requests against an AppSync endpoint.
 *
 * This is an implementation detail of the {@link AppSyncClient}.
 *
 * AppSync requests are raw GraphQL document strings, which contain AppSync-specific details,
 * such as AppSync mutation names (create, update, delete, and associated subscription names),
 * and AppSync-specific field names (`_version`, `_deleted`, etc.)
 */
final class AppSyncRequestFactory {
    private static final int WALK_DEPTH = 1;
    private static final List<String> ITEM_SYNC_KEYS = Arrays.asList(
        "_version",
        "_deleted",
        "_lastChangedAt"
    );
    private static final List<String> PAGINATION_KEYS = Arrays.asList(
        "nextToken",
        "startedAt"
    );

    private AppSyncRequestFactory() {}

    /**
     * Builds the query document for base and delta sync.
     * If you provide lastSyncTime, it builds a delta sync, where the delta is computed
     * against the provided time. Otherwise, if you provide a null lastSyncTime, a
     * request doc is generated for a base sync.
     * @param modelClass Class for which we want to sync.
     * @param lastSync The last time synced. If not provided, do a base query.
     *                 If provided, do a delta query.
     * @param <T> The type of objects we are syncing
     * @return A string which contains a GraphQL query doc for an base/delta sync
     * @throws DataStoreException On Failure to inspect
     */
    @NonNull
    static <T extends Model> String buildSyncDoc(
            @NonNull final Class<T> modelClass,
            @Nullable final Long lastSync,
            @SuppressWarnings("SameParameterValue") @Nullable final String nextToken)
            throws DataStoreException {

        final StringBuilder doc = new StringBuilder();
        final String capitalizedModelName = Casing.capitalizeFirst(modelClass.getSimpleName());

        int indent = 0;
        // Outer container, e.g. query SyncBlogPost {
        doc.append("query Sync").append(capitalizedModelName).append("s {\n");

        // Inner directive, e.g. syncBlogPosts(
        doc.append(padBy(++indent))
            .append("sync").append(capitalizedModelName).append("s");

        // Optional param for inner directive, i.e. (lastSync: 123123)
        // (lastSync:11123123, nextToken: "asdfasdfaS")
        if (lastSync != null || nextToken != null) {
            doc.append("(");
            if (lastSync != null) {
                doc.append("lastSync: ").append(lastSync);
                if (nextToken != null) {
                    doc.append(", ");
                }
            }
            if (nextToken != null) {
                doc.append("nextToken: \"").append(nextToken).append("\"");
            }
            doc.append(")");
        }

        // Opening clause for selection set
        doc.append(" {\n");
        doc.append(padBy(++indent)).append("items {\n");

        ++indent;
        doc.append(buildSelectionPortion(modelClass, indent, WALK_DEPTH));
        for (final String itemSyncKey : ITEM_SYNC_KEYS) {
            doc.append(padBy(indent)).append(itemSyncKey).append("\n");
        }
        --indent;

        // end the selection set for the items of modelClass
        doc.append(padBy(indent)).append("}\n");

        // end the top-level of selection set (for query)
        for (String paginationKey : PAGINATION_KEYS) {
            doc.append(padBy(indent)).append(paginationKey).append("\n");
        }
        doc.append(padBy(--indent)).append("}\n");

        // End the container (that started as `query SyncBlogPost {`)
        doc.append("}\n");

        return doc.toString();
    }

    static <T extends Model> String buildSubscriptionDoc(
            Class<T> modelClass, SubscriptionType subscriptionType) throws DataStoreException {

        final String capitalizedModelName = Casing.capitalizeFirst(modelClass.getSimpleName());
        String verb;
        switch (subscriptionType) {
            case ON_CREATE:
                verb = "Create";
                break;
            case ON_DELETE:
                verb = "Delete";
                break;
            case ON_UPDATE:
                verb = "Update";
                break;
            default:
                throw new DataStoreException(
                    "Unknown subscription type.", "Check if a new subcription type has been added?"
                );
        }
        StringBuilder builder = new StringBuilder();
        int indent = 0;

        // subscription OnCreatePost {
        builder.append("subscription On").append(verb).append(capitalizedModelName).append(" {\n");

        //  onCreatePost {
        builder.append(padBy(++indent)).append("on").append(verb).append(capitalizedModelName).append(" {\n");

        ++indent;
        builder.append(buildSelectionPortion(modelClass, indent, WALK_DEPTH));
        for (final String itemSyncKey : ITEM_SYNC_KEYS) {
            builder.append(padBy(indent)).append(itemSyncKey).append("\n");
        }
        --indent;
        
        // end the the inner subscription directive
        builder.append(padBy(indent)).append("}\n");

        // End the container (that started as `subscription OnWhatever {`
        builder.append(padBy(--indent)).append("}\n");

        return builder.toString();
    }

    static <T extends Model> String buildDeletionDoc(Class<T> modelClass) throws DataStoreException {
        return buildMutation(modelClass, MutationType.DELETE);
    }

    static <T extends Model> String buildUpdateDoc(Class<T> modelClass) throws DataStoreException {
        return buildMutation(modelClass, MutationType.UPDATE);
    }

    static <T extends Model> String buildCreationDoc(Class<T> modelClass) throws DataStoreException {
        return buildMutation(modelClass, MutationType.CREATE);
    }

    /**
     * Builds a mutation.
     * @param modelClass class of the model
     * @param mutationType Type of mutation, e.g. {@link MutationType#CREATE}
     * @param <T> Type of model being mutated
     * @return Mutation doc
     */
    private static <T extends Model> String buildMutation(Class<T> modelClass, MutationType mutationType)
            throws DataStoreException {

        final String capitalizedModelName = Casing.capitalizeFirst(modelClass.getSimpleName());
        int indent = 0;
        StringBuilder doc = new StringBuilder();

        final String verb;
        switch (mutationType) {
            case CREATE:
                verb = "Create";
                break;
            case UPDATE:
                verb = "Update";
                break;
            case DELETE:
                verb = "Delete";
                break;
            default:
                throw new DataStoreException(
                    "Unknown mutation type = " + mutationType,
                    "Check if support has been added in the sync engine."
                );
        }

        // mutation CreateBlogOwner($input:CreateBlogOwnerInput!) {
        doc.append("mutation ").append(verb).append(capitalizedModelName)
            .append("($input:").append(verb).append(capitalizedModelName).append("Input!) {\n");

        //   createBlogOwner(input:$input)
        doc.append(padBy(++indent)).append(verb.toLowerCase(Locale.US)).append(capitalizedModelName)
            .append("(input:$input) {\n");

        ++indent;
        doc.append(buildSelectionPortion(modelClass, indent, WALK_DEPTH));
        for (final String itemSyncKey : ITEM_SYNC_KEYS) {
            doc.append(padBy(indent)).append(itemSyncKey).append("\n");
        }
        --indent;

        // end the the inner createWhatever directive
        doc.append(padBy(indent)).append("}\n");

        // End the container (that started as `mutation CreateBlogOwner(` etc.
        doc.append(padBy(--indent)).append("}\n");

        return doc.toString();
    }

    private static String padBy(int indent) {
        if (indent <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < indent; index++) {
            builder.append("  "); // 2 spaces per level
        }
        return builder.toString();
    }

    private static <T extends Model> String buildSelectionPortion(
            final Class<T> modelClass,
            final int indentationLevel,
            @SuppressWarnings("SameParameterValue") final int levelsToGo)
            throws DataStoreException {

        if (levelsToGo < 0) {
            return "\n";
        }

        int indents = indentationLevel;
        final StringBuilder result = new StringBuilder();
        ModelSchema schema;
        try {
            schema = ModelSchema.fromModelClass(modelClass);
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                amplifyException, "Validate your model file.");
        }

        for (Field field : FieldFinder.findFieldsIn(modelClass)) {
            if (schema.getAssociations().containsKey(field.getName())) {
                if (List.class.isAssignableFrom(field.getType()) && levelsToGo > 0) {
                    result
                        .append(padBy(indents)).append(field.getName()).append(" {\n")
                        .append(padBy(++indents)).append("items {\n")
                        .append(padBy(++indents)).append("id\n")
                        .append(padBy(--indents)).append("}\n")
                        .append(padBy(indents)).append("nextToken\n")
                        .append(padBy(indents)).append("startedAt\n")
                        .append(padBy(--indents)).append("}\n");
                } else if (levelsToGo > 0) {
                    result
                        .append(padBy(indents)).append(field.getName()).append(" {\n")
                        .append(padBy(++indents)).append("id\n")
                        .append(padBy(--indents)).append("}\n");
                }
            } else {
                result.append(padBy(indents)).append(field.getName()).append("\n");
            }
        }
        return result.toString();
    }
}
