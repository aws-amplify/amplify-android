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

package com.amplifyframework.datastore.syncengine;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

/**
 * Maintains a topological ordering of a collection of ModelSchema.
 * A ModelSchema A is "less than" ModelSchema B, if there is a dependency lineage
 * from A to B. If a ModelSchema has no dependencies, than it is greater than or
 * equal to all other ModelSchema.
 *
 * For example, if a Blog has n Posts, and each Post has n Comment:
 *   input := [Comment, Post, Blog]
 * than the topological ordering is:
 *   output := [Blog, Post, Comment]
 */
@SuppressWarnings("unused")
final class TopologicalOrdering {
    private final List<ModelSchema> modelSchema;

    private TopologicalOrdering(List<ModelSchema> modelSchema) {
        this.modelSchema = modelSchema;
    }

    /**
     * Gets a TopologicalOrdering of the ModelSchema in the ModelSchemaRegistry.
     * The set of ModelSchema in that registry is not expected to change during runtime,
     * so the results of this TopologicalOrdering should likewise be stable at runtime.
     * @param modelSchemaRegistry A registry of ModelSchema
     * @param modelProvider A ModelProvider
     * @return A topological ordering of the model schema in the registry
     */
    @SuppressLint("SyntheticAccessor")
    static TopologicalOrdering forRegisteredModels(
            @NonNull ModelSchemaRegistry modelSchemaRegistry,
            @NonNull ModelProvider modelProvider) {
        Objects.requireNonNull(modelProvider);
        final List<ModelSchema> schemaForModels = new ArrayList<>();
        for (Class<? extends Model> modelClass : modelProvider.models()) {
            final String modelClassName = modelClass.getSimpleName();
            final ModelSchema schemaForModelClass = modelSchemaRegistry.getModelSchemaForModelClass(modelClassName);
            schemaForModels.add(schemaForModelClass);
        }
        return new TopologicalOrdering(new TopologicalSort(schemaForModels).result());
    }

    /**
     * Compares two ModelSchema to determine if one comes before the other.
     * For example, compare(blogSchema, postSchema) == -2.
     * @param one A ModelSchema
     * @param two A ModelSchema
     * @return A number less than 0 if one comes before two. A number greater than 0 is one comes
     *         after two. Returns 0 in the case where one and two are the same schema.
     */
    int compare(@NonNull ModelSchema one, @NonNull ModelSchema two) {
        Objects.requireNonNull(one);
        Objects.requireNonNull(two);
        int onePosition = -1;
        int twoPosition = -1;
        for (int index = 0; index < modelSchema.size(); index++) {
            if (modelSchema.get(index).equals(one)) {
                onePosition = index;
            }
            if (modelSchema.get(index).equals(two)) {
                twoPosition = index;
            }
        }
        return onePosition - twoPosition;
    }

    /**
     * Check the ordering of a ModelSchema.
     * @param modelSchema A model schema
     * @return A collection of checks that can be made on the ordering of the schema
     */
    @SuppressWarnings("unused")
    @NonNull
    @SuppressLint("SyntheticAccessor")
    OrderingCheck check(@NonNull ModelSchema modelSchema) {
        Objects.requireNonNull(modelSchema);
        if (!this.modelSchema.contains(modelSchema)) {
            throw new NoSuchElementException("No model schema matching " + modelSchema.getName());
        }
        return new OrderingCheck(modelSchema);
    }

    /**
     * Exercises a Topological Sort on a list of ModelSchema.
     */
    private static final class TopologicalSort {
        private final Stack<ModelSchema> result;
        private final Set<ModelSchema> unvisited;
        private final List<ModelSchema> input;

        private TopologicalSort(List<ModelSchema> modelSchema) {
            this.input = modelSchema;
            this.unvisited = new HashSet<>(modelSchema);
            this.result = new Stack<>();
        }

        private List<ModelSchema> result() {
            while (!unvisited.isEmpty()) { // While there are model schema we haven't visited,
                ModelSchema randomlyPicked = unvisited.iterator().next(); // Pick one at random.
                visit(randomlyPicked); // Visit it.
            }
            return result;
        }

        private void visit(ModelSchema node) {
            unvisited.remove(node);
            for (ModelSchema unvisitedAssociationOwner : findUnvisitedAssociationOwners(node)) {
                visit(unvisitedAssociationOwner);
            }
            result.push(node);
        }

        private Set<ModelSchema> findUnvisitedAssociationOwners(ModelSchema node) {
            final Set<ModelSchema> unvisitedAssociationOwners = new HashSet<>();
            for (ModelSchema associationOwner : findAssociationOwners(node)) {
                if (unvisited.contains(associationOwner)) {
                    unvisitedAssociationOwners.add(associationOwner);
                }
            }
            return unvisitedAssociationOwners;
        }

        private Set<ModelSchema> findAssociationOwners(ModelSchema node) {
            final Set<ModelSchema> associationOwners = new HashSet<>();
            for (ModelAssociation association : node.getAssociations().values()) {
                if (association.isOwner()) {
                    associationOwners.add(findInputSchemaByName(association.getAssociatedType()));
                }
            }
            return associationOwners;
        }

        private ModelSchema findInputSchemaByName(String name) {
            for (ModelSchema schema : input) {
                if (schema.getName().equals(name)) {
                    return schema;
                }
            }
            throw new NoSuchElementException("No model schema provided with name = " + name);
        }
    }

    @SuppressWarnings("unused")
    final class OrderingCheck {
        private final ModelSchema modelSchema;

        private OrderingCheck(@NonNull ModelSchema modelSchema) {
            this.modelSchema = Objects.requireNonNull(modelSchema);
        }

        boolean isAfter(@NonNull ModelSchema someOther) {
            Objects.requireNonNull(someOther);
            return compare(someOther, modelSchema) < 0;
        }

        boolean isBefore(@NonNull ModelSchema someOther) {
            Objects.requireNonNull(someOther);
            return compare(someOther, modelSchema) > 0;
        }
    }

    /**
     * Any component which provides a means to create an instance of {@link TopologicalOrdering}.
     */
    interface Factory {
        TopologicalOrdering create();
    }
}
