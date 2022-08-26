package com.amplifyframework.testmodels.transformerV2.schemadrift;

import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the SchemaDrift type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "SchemaDrifts")
public final class SchemaDrift implements Model {
    public static final QueryField ID = field("SchemaDrift", "id");
    public static final QueryField CREATED_AT = field("SchemaDrift", "createdAt");
    public static final QueryField ENUM_VALUE = field("SchemaDrift", "enumValue");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime createdAt;
    private final @ModelField(targetType="EnumDrift") EnumDrift enumValue;
    private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
    public String getId() {
        return id;
    }

    public Temporal.DateTime getCreatedAt() {
        return createdAt;
    }

    public EnumDrift getEnumValue() {
        return enumValue;
    }

    public Temporal.DateTime getUpdatedAt() {
        return updatedAt;
    }

    private SchemaDrift(String id, Temporal.DateTime createdAt, EnumDrift enumValue) {
        this.id = id;
        this.createdAt = createdAt;
        this.enumValue = enumValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            SchemaDrift schemaDrift = (SchemaDrift) obj;
            return ObjectsCompat.equals(getId(), schemaDrift.getId()) &&
                    ObjectsCompat.equals(getCreatedAt(), schemaDrift.getCreatedAt()) &&
                    ObjectsCompat.equals(getEnumValue(), schemaDrift.getEnumValue()) &&
                    ObjectsCompat.equals(getUpdatedAt(), schemaDrift.getUpdatedAt());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getCreatedAt())
                .append(getEnumValue())
                .append(getUpdatedAt())
                .toString()
                .hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("SchemaDrift {")
                .append("id=" + String.valueOf(getId()) + ", ")
                .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
                .append("enumValue=" + String.valueOf(getEnumValue()) + ", ")
                .append("updatedAt=" + String.valueOf(getUpdatedAt()))
                .append("}")
                .toString();
    }

    public static CreatedAtStep builder() {
        return new Builder();
    }

    /**
     * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
     * This is a convenience method to return an instance of the object with only its ID populated
     * to be used in the context of a parameter in a delete mutation or referencing a foreign key
     * in a relationship.
     * @param id the id of the existing item this instance will represent
     * @return an instance of this model with only ID populated
     */
    public static SchemaDrift justId(String id) {
        return new SchemaDrift(
                id,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                createdAt,
                enumValue);
    }
    public interface CreatedAtStep {
        BuildStep createdAt(Temporal.DateTime createdAt);
    }


    public interface BuildStep {
        SchemaDrift build();
        BuildStep id(String id);
        BuildStep enumValue(EnumDrift enumValue);
    }


    public static class Builder implements CreatedAtStep, BuildStep {
        private String id;
        private Temporal.DateTime createdAt;
        private EnumDrift enumValue;
        @Override
        public SchemaDrift build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new SchemaDrift(
                    id,
                    createdAt,
                    enumValue);
        }

        @Override
        public BuildStep createdAt(Temporal.DateTime createdAt) {
            Objects.requireNonNull(createdAt);
            this.createdAt = createdAt;
            return this;
        }

        @Override
        public BuildStep enumValue(EnumDrift enumValue) {
            this.enumValue = enumValue;
            return this;
        }

        /**
         * @param id id
         * @return Current Builder instance, for fluent method chaining
         */
        public BuildStep id(String id) {
            this.id = id;
            return this;
        }
    }


    public final class CopyOfBuilder extends Builder {
        private CopyOfBuilder(String id, Temporal.DateTime createdAt, EnumDrift enumValue) {
            super.id(id);
            super.createdAt(createdAt)
                    .enumValue(enumValue);
        }

        @Override
        public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
            return (CopyOfBuilder) super.createdAt(createdAt);
        }

        @Override
        public CopyOfBuilder enumValue(EnumDrift enumValue) {
            return (CopyOfBuilder) super.enumValue(enumValue);
        }
    }

}
