package com.amplifyframework.testmodels.todo;


import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import androidx.core.util.ObjectsCompat;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/**
 * This is an auto generated class representing the Todo type in your schema.
 */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Todos")
public final class Todo implements Model {
    public static final QueryField ID = field("id");
    public static final QueryField TITLE = field("title");
    public static final QueryField CONTENT = field("content");
    public static final QueryField STATUS = field("status");
    public static final QueryField CREATED_AT = field("createdAt");
    public static final QueryField LAST_UPDATED = field("lastUpdated");
    public static final QueryField DUE_DATE = field("dueDate");
    public static final QueryField PRIORITY = field("priority");
    public static final QueryField HOURS_SPENT = field("hoursSpent");
    public static final QueryField DUPLICATE = field("duplicate");
    public static final QueryField OWNER = field("owner");
    public static final QueryField TAGS = field("tags");
    private final @ModelField(targetType = "ID", isRequired = true)
    String id;
    private final @ModelField(targetType = "String", isRequired = true)
    String title;
    private final @ModelField(targetType = "String", isRequired = true)
    String content;
    private final @ModelField(targetType = "TodoStatus", isRequired = true)
    TodoStatus status;
    private final @ModelField(targetType = "AWSDateTime", isRequired = true)
    Temporal.DateTime createdAt;
    private final @ModelField(targetType = "AWSTimestamp")
    Long lastUpdated;
    private final @ModelField(targetType = "AWSDate")
    Temporal.Date dueDate;
    private final @ModelField(targetType = "Int")
    Integer priority;
    private final @ModelField(targetType = "Float")
    Float hoursSpent;
    private final @ModelField(targetType = "Boolean", isRequired = true)
    Boolean duplicate;
    private final @ModelField(targetType = "TodoOwner", isRequired = true)
    TodoOwner owner;
    private final @ModelField(targetType = "String")
    List<String> tags;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public TodoStatus getStatus() {
        return status;
    }

    public Temporal.DateTime getCreatedAt() {
        return createdAt;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public Temporal.Date getDueDate() {
        return dueDate;
    }

    public Integer getPriority() {
        return priority;
    }

    public Float getHoursSpent() {
        return hoursSpent;
    }

    public Boolean getDuplicate() {
        return duplicate;
    }

    public TodoOwner getOwner() {
        return owner;
    }

    public List<String> getTags() {
        return tags;
    }

    private Todo(String id, String title, String content, TodoStatus status, Temporal.DateTime createdAt, Long lastUpdated, Temporal.Date dueDate, Integer priority, Float hoursSpent, Boolean duplicate, TodoOwner owner, List<String> tags) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
        this.dueDate = dueDate;
        this.priority = priority;
        this.hoursSpent = hoursSpent;
        this.duplicate = duplicate;
        this.owner = owner;
        this.tags = tags;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            Todo todo = (Todo) obj;
            return ObjectsCompat.equals(getId(), todo.getId()) &&
                    ObjectsCompat.equals(getTitle(), todo.getTitle()) &&
                    ObjectsCompat.equals(getContent(), todo.getContent()) &&
                    ObjectsCompat.equals(getStatus(), todo.getStatus()) &&
                    ObjectsCompat.equals(getCreatedAt(), todo.getCreatedAt()) &&
                    ObjectsCompat.equals(getLastUpdated(), todo.getLastUpdated()) &&
                    ObjectsCompat.equals(getDueDate(), todo.getDueDate()) &&
                    ObjectsCompat.equals(getPriority(), todo.getPriority()) &&
                    ObjectsCompat.equals(getHoursSpent(), todo.getHoursSpent()) &&
                    ObjectsCompat.equals(getDuplicate(), todo.getDuplicate()) &&
                    ObjectsCompat.equals(getOwner(), todo.getOwner()) &&
                    ObjectsCompat.equals(getTags(), todo.getTags());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getTitle())
                .append(getContent())
                .append(getStatus())
                .append(getCreatedAt())
                .append(getLastUpdated())
                .append(getDueDate())
                .append(getPriority())
                .append(getHoursSpent())
                .append(getDuplicate())
                .append(getOwner())
                .append(getTags())
                .toString()
                .hashCode();
    }

    public static TitleStep builder() {
        return new Builder();
    }

    /**
     * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
     * This is a convenience method to return an instance of the object with only its ID populated
     * to be used in the context of a parameter in a delete mutation or referencing a foreign key
     * in a relationship.
     *
     * @param id the id of the existing item this instance will represent
     * @return an instance of this model with only ID populated
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public static Todo justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }
        return new Todo(
                id,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                title,
                content,
                status,
                createdAt,
                lastUpdated,
                dueDate,
                priority,
                hoursSpent,
                duplicate,
                owner,
                tags);
    }

    public interface TitleStep {
        ContentStep title(String title);
    }


    public interface ContentStep {
        StatusStep content(String content);
    }


    public interface StatusStep {
        CreatedAtStep status(TodoStatus status);
    }


    public interface CreatedAtStep {
        DuplicateStep createdAt(Temporal.DateTime createdAt);
    }


    public interface DuplicateStep {
        OwnerStep duplicate(Boolean duplicate);
    }


    public interface OwnerStep {
        BuildStep owner(TodoOwner owner);
    }


    public interface BuildStep {
        Todo build();

        BuildStep id(String id) throws IllegalArgumentException;

        BuildStep lastUpdated(Long lastUpdated);

        BuildStep dueDate(Temporal.Date dueDate);

        BuildStep priority(Integer priority);

        BuildStep hoursSpent(Float hoursSpent);

        BuildStep tags(List<String> tags);
    }


    public static class Builder implements TitleStep, ContentStep, StatusStep, CreatedAtStep, DuplicateStep, OwnerStep, BuildStep {
        private String id;
        private String title;
        private String content;
        private TodoStatus status;
        private Temporal.DateTime createdAt;
        private Boolean duplicate;
        private TodoOwner owner;
        private Long lastUpdated;
        private Temporal.Date dueDate;
        private Integer priority;
        private Float hoursSpent;
        private List<String> tags;

        @Override
        public Todo build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new Todo(
                    id,
                    title,
                    content,
                    status,
                    createdAt,
                    lastUpdated,
                    dueDate,
                    priority,
                    hoursSpent,
                    duplicate,
                    owner,
                    tags);
        }

        @Override
        public ContentStep title(String title) {
            Objects.requireNonNull(title);
            this.title = title;
            return this;
        }

        @Override
        public StatusStep content(String content) {
            Objects.requireNonNull(content);
            this.content = content;
            return this;
        }

        @Override
        public CreatedAtStep status(TodoStatus status) {
            Objects.requireNonNull(status);
            this.status = status;
            return this;
        }

        @Override
        public DuplicateStep createdAt(Temporal.DateTime createdAt) {
            Objects.requireNonNull(createdAt);
            this.createdAt = createdAt;
            return this;
        }

        @Override
        public OwnerStep duplicate(Boolean duplicate) {
            Objects.requireNonNull(duplicate);
            this.duplicate = duplicate;
            return this;
        }

        @Override
        public BuildStep owner(TodoOwner owner) {
            Objects.requireNonNull(owner);
            this.owner = owner;
            return this;
        }

        @Override
        public BuildStep lastUpdated(Long lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        @Override
        public BuildStep dueDate(Temporal.Date dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        @Override
        public BuildStep priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public BuildStep hoursSpent(Float hoursSpent) {
            this.hoursSpent = hoursSpent;
            return this;
        }

        @Override
        public BuildStep tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
         * This should only be set when referring to an already existing object.
         *
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
        private CopyOfBuilder(String id, String title, String content, TodoStatus status, Temporal.DateTime createdAt, Long lastUpdated, Temporal.Date dueDate, Integer priority, Float hoursSpent, Boolean duplicate, TodoOwner owner, List<String> tags) {
            super.id(id);
            super.title(title)
                    .content(content)
                    .status(status)
                    .createdAt(createdAt)
                    .duplicate(duplicate)
                    .owner(owner)
                    .lastUpdated(lastUpdated)
                    .dueDate(dueDate)
                    .priority(priority)
                    .hoursSpent(hoursSpent)
                    .tags(tags);
        }

        @Override
        public CopyOfBuilder title(String title) {
            return (CopyOfBuilder) super.title(title);
        }

        @Override
        public CopyOfBuilder content(String content) {
            return (CopyOfBuilder) super.content(content);
        }

        @Override
        public CopyOfBuilder status(TodoStatus status) {
            return (CopyOfBuilder) super.status(status);
        }

        @Override
        public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
            return (CopyOfBuilder) super.createdAt(createdAt);
        }

        @Override
        public CopyOfBuilder duplicate(Boolean duplicate) {
            return (CopyOfBuilder) super.duplicate(duplicate);
        }

        @Override
        public CopyOfBuilder owner(TodoOwner owner) {
            return (CopyOfBuilder) super.owner(owner);
        }

        @Override
        public CopyOfBuilder lastUpdated(Long lastUpdated) {
            return (CopyOfBuilder) super.lastUpdated(lastUpdated);
        }

        @Override
        public CopyOfBuilder dueDate(Temporal.Date dueDate) {
            return (CopyOfBuilder) super.dueDate(dueDate);
        }

        @Override
        public CopyOfBuilder priority(Integer priority) {
            return (CopyOfBuilder) super.priority(priority);
        }

        @Override
        public CopyOfBuilder hoursSpent(Float hoursSpent) {
            return (CopyOfBuilder) super.hoursSpent(hoursSpent);
        }

        @Override
        public CopyOfBuilder tags(List<String> tags) {
            return (CopyOfBuilder) super.tags(tags);
        }
    }

}
