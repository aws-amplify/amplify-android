package com.amplifyframework.testmodels.ownerauth;


import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import java.util.Objects;
import java.util.UUID;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the OwnerAuthReadUpdateOnly type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "OwnerAuthReadUpdateOnlies", authRules = {
        @AuthRule(allow = AuthStrategy.OWNER, operations = { ModelOperation.CREATE, ModelOperation.DELETE })
})
public final class OwnerAuthReadUpdateOnly implements Model {
    public static final QueryField ID = field("id");
    public static final QueryField TITLE = field("title");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String", isRequired = true) String title;
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    private OwnerAuthReadUpdateOnly(String id, String title) {
        this.id = id;
        this.title = title;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            OwnerAuthReadUpdateOnly ownerAuthReadUpdateOnly = (OwnerAuthReadUpdateOnly) obj;
            return ObjectsCompat.equals(getId(), ownerAuthReadUpdateOnly.getId()) &&
                    ObjectsCompat.equals(getTitle(), ownerAuthReadUpdateOnly.getTitle());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getTitle())
                .toString()
                .hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("OwnerAuthReadUpdateOnly {")
                .append("id=" + String.valueOf(getId()) + ", ")
                .append("title=" + String.valueOf(getTitle()))
                .append("}")
                .toString();
    }

    public static TitleStep builder() {
        return new Builder();
    }

    /**
     * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
     * This is a convenience method to return an instance of the object with only its ID populated
     * to be used in the context of a parameter in a delete mutation or referencing a foreign key
     * in a relationship.
     * @param id the id of the existing item this instance will represent
     * @return an instance of this model with only ID populated
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public static OwnerAuthReadUpdateOnly justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }
        return new OwnerAuthReadUpdateOnly(
                id,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                title);
    }
    public interface TitleStep {
        BuildStep title(String title);
    }


    public interface BuildStep {
        OwnerAuthReadUpdateOnly build();
        BuildStep id(String id) throws IllegalArgumentException;
    }


    public static class Builder implements TitleStep, BuildStep {
        private String id;
        private String title;
        @Override
        public OwnerAuthReadUpdateOnly build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new OwnerAuthReadUpdateOnly(
                    id,
                    title);
        }

        @Override
        public BuildStep title(String title) {
            Objects.requireNonNull(title);
            this.title = title;
            return this;
        }

        /**
         * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
         * This should only be set when referring to an already existing object.
         * @param id id
         * @return Current Builder instance, for fluent method chaining
         * @throws IllegalArgumentException Checks that ID is in the proper format
         */
        public BuildStep id(String id) throws IllegalArgumentException {
            this.id = id;

            try {
                UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
            } catch (Exception exception) {
                throw new IllegalArgumentException("Model IDs must be unique in the format of UUID.",
                        exception);
            }

            return this;
        }
    }


    public final class CopyOfBuilder extends Builder {
        private CopyOfBuilder(String id, String title) {
            super.id(id);
            super.title(title);
        }

        @Override
        public CopyOfBuilder title(String title) {
            return (CopyOfBuilder) super.title(title);
        }
    }

}
