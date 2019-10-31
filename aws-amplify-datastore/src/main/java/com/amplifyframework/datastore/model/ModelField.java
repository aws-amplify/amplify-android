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

package com.amplifyframework.datastore.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents a field of the {@link Model} class.
 * Encapsulates all the information of a field.
 */
public class ModelField {
    // Name of the field is the name of the instance variable
    // of the Model class.
    private final String name;

    // Name of the field in the target. For example: name of the
    // field in the GraphQL type.
    private final String targetName;

    // The data type of the field.
    private final String type;

    // If the field is a required or an optional field
    private final boolean isRequired;

    // If the field is an array type. False if it is a primitive
    // type and True if it is an array type.
    private final boolean isArray;

    // True if the field is a primary key in the Model.
    private final boolean isPrimaryKey;

    // Name of the Model that this field is connecting to.
    private final String connectedTo;

    /**
     * Construct the ModelField object.
     *
     * @param name Name of the field is the name of the instance variable of the Model class.
     * @param targetName Name of the field in the target. For example: name of the field in the
     *                   GraphQL type.
     * @param type The data type of the field.
     * @param isRequired If the field is a required or an optional field.
     * @param isArray If the field is an array type. False if it is a primitive type and True
     *                if it is an array type.
     * @param isPrimaryKey True if the field is a primary key in the Model.
     * @param connectedTo Name of the Model that this field is connecting to.
     */
    public ModelField(@NonNull String name,
                      @NonNull String targetName,
                      @NonNull String type,
                      boolean isRequired,
                      boolean isArray,
                      boolean isPrimaryKey,
                      @Nullable String connectedTo) {
        this.name = name;
        this.targetName = targetName;
        this.type = type;
        this.isRequired = isRequired;
        this.isArray = isArray;
        this.isPrimaryKey = isPrimaryKey;
        this.connectedTo = connectedTo;
    }

    /**
     * @return Name of the field is the name of the instance variable of the Model class.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Name of the field in the target. For example: name of the field in the GraphQL type.
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * @return The data type of the field.
     */
    public String getType() {
        return type;
    }

    /**
     * @return If the field is a required or an optional field.
     */
    public boolean isRequired() {
        return isRequired;
    }

    /**
     * @return If the field is an array type. False if it is a primitive type and True if it
     *         is an array type.
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * @return True if the field is a primary key in the Model.
     */
    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    /**
     * @return  Name of the Model that this field is connecting to.
     */
    public String getConnectedTo() {
        return connectedTo;
    }

    /**
     * @return True if this ModelField is connected to an other Model.
     */
    public boolean isConnected() {
        return connectedTo != null;
    }
}
