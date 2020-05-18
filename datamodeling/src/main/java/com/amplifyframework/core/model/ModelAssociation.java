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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.annotations.BelongsTo;

import java.util.Objects;

/**
 * Represents an association of the {@link Model} class.
 * This class encapsulates the information provided by the
 * following annotations on model fields:
 *
 *  - {@link com.amplifyframework.core.model.annotations.BelongsTo}
 *  - {@link com.amplifyframework.core.model.annotations.HasOne}
 *  - {@link com.amplifyframework.core.model.annotations.HasMany}
 *
 *  Only @BelongsTo tag implies that the field is a foreign key.
 */
public final class ModelAssociation {
    // Name of the association to identify the relationship.
    private final String name;

    // Name of the field within same model that references foreign key
    private final String targetName;

    // Name of the field in the associated model
    private final String associatedName;

    // Name of the model associated with this field
    private final String associatedType;

    /**
     * Construct the ModelAssociation object from the builder.
     */
    private ModelAssociation(@NonNull Builder builder) {
        this.name = Objects.requireNonNull(builder.name);
        this.targetName = builder.targetName;
        this.associatedName = builder.associatedName;
        this.associatedType = builder.associatedType;
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the name of the association to identify the relationship.
     * @return The name of the association to identify the relationship.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the target name of foreign key.
     * Null if field holding this association is not a foreign key.
     * @return The name of field within same model that references foreign key.
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * Gets the name of associated field.
     * @return The name of associated field.
     */
    public String getAssociatedName() {
        return associatedName;
    }

    /**
     * Gets the name of model associated with this field.
     * @return The name of model associated with this field.
     */
    public String getAssociatedType() {
        return associatedType;
    }

    /**
     * Returns true if this field "owns" the identity of
     * another model.
     * For example, a foreign key is an owner.
     * @return True if this field owns the identity of another model
     */
    public boolean isOwner() {
        return BelongsTo.class.getSimpleName().equals(getName());
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ModelAssociation that = (ModelAssociation) thatObject;
        if (!ObjectsCompat.equals(name, that.name)) {
            return false;
        }
        if (!ObjectsCompat.equals(targetName, that.targetName)) {
            return false;
        }
        if (!ObjectsCompat.equals(associatedName, that.associatedName)) {
            return false;
        }
        return ObjectsCompat.equals(associatedType, that.associatedType);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (targetName != null ? targetName.hashCode() : 0);
        result = 31 * result + (associatedName != null ? associatedName.hashCode() : 0);
        result = 31 * result + (associatedType != null ? associatedType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelAssociation{" +
                "name=\'" + name + "\'" +
                ", targetName=\'" + targetName + "\'" +
                ", associatedName=\'" + associatedName + "\'" +
                ", associatedType=\'" + associatedType + "\'" +
                '}';
    }

    /**
     * Builder class for {@link ModelAssociation}.
     */
    public static final class Builder {
        private String name;
        private String targetName;
        private String associatedName;
        private String associatedType;

        /**
         * Sets the name of association model.
         * @param name name of the association model
         * @return the association model with given name
         */
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * Sets the name of the field referring to the foreign key.
         * @param targetName name of the field referring to the foreign key.
         * @return the association model with give target name
         */
        public Builder targetName(String targetName) {
            this.targetName = targetName;
            return this;
        }

        /**
         * Sets the name of associated model.
         * @param associatedName name of the associated model
         * @return the association model with given associated name
         */
        public Builder associatedName(String associatedName) {
            this.associatedName = associatedName;
            return this;
        }

        /**
         * Sets the type of associated model.
         * @param associatedType type of the associated model
         * @return the association model with given associated type
         */
        public Builder associatedType(String associatedType) {
            this.associatedType = associatedType;
            return this;
        }

        /**
         * Builds an immutable ModelAssociation instance using
         * builder object.
         * @return ModelAssociation instance
         */
        public ModelAssociation build() {
            return new ModelAssociation(this);
        }
    }
}
