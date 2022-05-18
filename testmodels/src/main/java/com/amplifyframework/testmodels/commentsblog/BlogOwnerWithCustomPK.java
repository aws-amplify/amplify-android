package com.amplifyframework.testmodels.commentsblog;

import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the BlogOwnerWithCustomPK type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogOwnerWithCustomPK", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"name","wea"})
public final class BlogOwnerWithCustomPK implements Model {
    public static final QueryField ID = field("BlogOwnerWithCustomPK", "id");
    public static final QueryField NAME = field("BlogOwnerWithCustomPK", "name");
    public static final QueryField WEA = field("BlogOwnerWithCustomPK", "wea");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String", isRequired = true) String name;
    private final @ModelField(targetType="String", isRequired = true) String wea;
    private final @ModelField(targetType="Blog") @HasMany(associatedWith = "blogOwner", type = Blog.class) List<Blog> blogs = null;
    private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
    private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
    private BlogOwnerPrimaryKey blogOwnerPrimaryKey;


    @NonNull
    public BlogOwnerPrimaryKey resolveIdentifier() {
        if (blogOwnerPrimaryKey == null){
            this.blogOwnerPrimaryKey = new BlogOwnerPrimaryKey(name, wea);
        }
        return blogOwnerPrimaryKey;
    }
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWea() {
        return wea;
    }

    public List<Blog> getBlogs() {
        return blogs;
    }

    public Temporal.DateTime getCreatedAt() {
        return createdAt;
    }

    public Temporal.DateTime getUpdatedAt() {
        return updatedAt;
    }

    private BlogOwnerWithCustomPK(String id, String name, String wea) {
        this.id = id;
        this.name = name;
        this.wea = wea;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            BlogOwnerWithCustomPK BlogOwnerWithCustomPK = (BlogOwnerWithCustomPK) obj;
            return ObjectsCompat.equals(getId(), BlogOwnerWithCustomPK.getId()) &&
                    ObjectsCompat.equals(getName(), BlogOwnerWithCustomPK.getName()) &&
                    ObjectsCompat.equals(getWea(), BlogOwnerWithCustomPK.getWea()) &&
                    ObjectsCompat.equals(getCreatedAt(), BlogOwnerWithCustomPK.getCreatedAt()) &&
                    ObjectsCompat.equals(getUpdatedAt(), BlogOwnerWithCustomPK.getUpdatedAt());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getName())
                .append(getWea())
                .append(getCreatedAt())
                .append(getUpdatedAt())
                .toString()
                .hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("BlogOwnerWithCustomPK {")
                .append("id=" + String.valueOf(getId()) + ", ")
                .append("name=" + String.valueOf(getName()) + ", ")
                .append("wea=" + String.valueOf(getWea()) + ", ")
                .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
                .append("updatedAt=" + String.valueOf(getUpdatedAt()))
                .append("}")
                .toString();
    }

    public static NameStep builder() {
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
    public static BlogOwnerWithCustomPK justId(String id) {
        return new BlogOwnerWithCustomPK(
                id,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                name,
                wea);
    }
    public interface NameStep {
        WeaStep name(String name);
    }


    public interface WeaStep {
        BuildStep wea(String wea);
    }


    public interface BuildStep {
        BlogOwnerWithCustomPK build();
        BuildStep id(String id);
    }


    public static class Builder implements NameStep, WeaStep, BuildStep {
        private String id;
        private String name;
        private String wea;
        @Override
        public BlogOwnerWithCustomPK build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new BlogOwnerWithCustomPK(
                    id,
                    name,
                    wea);
        }

        @Override
        public WeaStep name(String name) {
            Objects.requireNonNull(name);
            this.name = name;
            return this;
        }

        @Override
        public BuildStep wea(String wea) {
            Objects.requireNonNull(wea);
            this.wea = wea;
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
        private CopyOfBuilder(String id, String name, String wea) {
            super.id(id);
            super.name(name)
                    .wea(wea);
        }

        @Override
        public CopyOfBuilder name(String name) {
            return (CopyOfBuilder) super.name(name);
        }

        @Override
        public CopyOfBuilder wea(String wea) {
            return (CopyOfBuilder) super.wea(wea);
        }
    }

    public class BlogOwnerPrimaryKey extends com.amplifyframework.core.model.ModelPrimaryKey<BlogOwnerWithCustomPK> {
        private static final long serialVersionUID = 1L;

        protected BlogOwnerPrimaryKey(String name, String wea) {
            super(name, wea);
        }
    }

}