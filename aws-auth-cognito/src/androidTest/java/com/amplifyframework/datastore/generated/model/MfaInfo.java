package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.core.model.ModelIdentifier;

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

/** This is an auto generated class representing the MfaInfo type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "MfaInfos", type = Model.Type.USER, version = 1)
public final class MfaInfo implements Model {
    public static final QueryField ID = field("MfaInfo", "id");
    public static final QueryField USERNAME = field("MfaInfo", "username");
    public static final QueryField CODE = field("MfaInfo", "code");
    public static final QueryField EXPIRATION_TIME = field("MfaInfo", "expirationTime");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String", isRequired = true) String username;
    private final @ModelField(targetType="String", isRequired = true) String code;
    private final @ModelField(targetType="AWSTimestamp", isRequired = true) Temporal.Timestamp expirationTime;
    private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
    private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
    /** @deprecated This API is internal to Amplify and should not be used. */
    @Deprecated
    public String resolveIdentifier() {
        return id;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getCode() {
        return code;
    }

    public Temporal.Timestamp getExpirationTime() {
        return expirationTime;
    }

    public Temporal.DateTime getCreatedAt() {
        return createdAt;
    }

    public Temporal.DateTime getUpdatedAt() {
        return updatedAt;
    }

    private MfaInfo(String id, String username, String code, Temporal.Timestamp expirationTime) {
        this.id = id;
        this.username = username;
        this.code = code;
        this.expirationTime = expirationTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            MfaInfo mfaInfo = (MfaInfo) obj;
            return ObjectsCompat.equals(getId(), mfaInfo.getId()) &&
                    ObjectsCompat.equals(getUsername(), mfaInfo.getUsername()) &&
                    ObjectsCompat.equals(getCode(), mfaInfo.getCode()) &&
                    ObjectsCompat.equals(getExpirationTime(), mfaInfo.getExpirationTime()) &&
                    ObjectsCompat.equals(getCreatedAt(), mfaInfo.getCreatedAt()) &&
                    ObjectsCompat.equals(getUpdatedAt(), mfaInfo.getUpdatedAt());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getUsername())
                .append(getCode())
                .append(getExpirationTime())
                .append(getCreatedAt())
                .append(getUpdatedAt())
                .toString()
                .hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("MfaInfo {")
                .append("id=" + String.valueOf(getId()) + ", ")
                .append("username=" + String.valueOf(getUsername()) + ", ")
                .append("code=" + String.valueOf(getCode()) + ", ")
                .append("expirationTime=" + String.valueOf(getExpirationTime()) + ", ")
                .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
                .append("updatedAt=" + String.valueOf(getUpdatedAt()))
                .append("}")
                .toString();
    }

    public static UsernameStep builder() {
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
    public static MfaInfo justId(String id) {
        return new MfaInfo(
                id,
                null,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                username,
                code,
                expirationTime);
    }
    public interface UsernameStep {
        CodeStep username(String username);
    }


    public interface CodeStep {
        ExpirationTimeStep code(String code);
    }


    public interface ExpirationTimeStep {
        BuildStep expirationTime(Temporal.Timestamp expirationTime);
    }


    public interface BuildStep {
        MfaInfo build();
        BuildStep id(String id);
    }


    public static class Builder implements UsernameStep, CodeStep, ExpirationTimeStep, BuildStep {
        private String id;
        private String username;
        private String code;
        private Temporal.Timestamp expirationTime;
        public Builder() {

        }

        private Builder(String id, String username, String code, Temporal.Timestamp expirationTime) {
            this.id = id;
            this.username = username;
            this.code = code;
            this.expirationTime = expirationTime;
        }

        @Override
        public MfaInfo build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new MfaInfo(
                    id,
                    username,
                    code,
                    expirationTime);
        }

        @Override
        public CodeStep username(String username) {
            Objects.requireNonNull(username);
            this.username = username;
            return this;
        }

        @Override
        public ExpirationTimeStep code(String code) {
            Objects.requireNonNull(code);
            this.code = code;
            return this;
        }

        @Override
        public BuildStep expirationTime(Temporal.Timestamp expirationTime) {
            Objects.requireNonNull(expirationTime);
            this.expirationTime = expirationTime;
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
        private CopyOfBuilder(String id, String username, String code, Temporal.Timestamp expirationTime) {
            super(id, username, code, expirationTime);
            Objects.requireNonNull(username);
            Objects.requireNonNull(code);
            Objects.requireNonNull(expirationTime);
        }

        @Override
        public CopyOfBuilder username(String username) {
            return (CopyOfBuilder) super.username(username);
        }

        @Override
        public CopyOfBuilder code(String code) {
            return (CopyOfBuilder) super.code(code);
        }

        @Override
        public CopyOfBuilder expirationTime(Temporal.Timestamp expirationTime) {
            return (CopyOfBuilder) super.expirationTime(expirationTime);
        }
    }


    public static class MfaInfoIdentifier extends ModelIdentifier<MfaInfo> {
        private static final long serialVersionUID = 1L;
        public MfaInfoIdentifier(String id) {
            super(id);
        }
    }

}
