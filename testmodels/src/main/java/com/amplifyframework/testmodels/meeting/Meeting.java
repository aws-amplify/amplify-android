package com.amplifyframework.testmodels.meeting;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.AWSDate;
import com.amplifyframework.core.model.AWSDateTime;
import com.amplifyframework.core.model.AWSTime;
import com.amplifyframework.core.model.AWSTimestamp;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import java.util.Objects;
import java.util.UUID;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the Meeting type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Meetings")
public final class Meeting implements Model {
    public static final QueryField ID = field("id");
    public static final QueryField NAME = field("name");
    public static final QueryField DATE = field("date");
    public static final QueryField DATE_TIME = field("dateTime");
    public static final QueryField TIME = field("time");
    public static final QueryField TIMESTAMP = field("timestamp");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String", isRequired = true) String name;
    private final @ModelField(targetType="AWSDate") AWSDate date;
    private final @ModelField(targetType="AWSDateTime") AWSDateTime dateTime;
    private final @ModelField(targetType="AWSTime") AWSTime time;
    private final @ModelField(targetType="AWSTimestamp") AWSTimestamp timestamp;
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AWSDate getDate() {
        return date;
    }

    public AWSDateTime getDateTime() {
        return dateTime;
    }

    public AWSTime getTime() {
        return time;
    }

    public AWSTimestamp getTimestamp() {
        return timestamp;
    }

    private Meeting(String id, String name, AWSDate date, AWSDateTime dateTime, AWSTime time, AWSTimestamp timestamp) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.dateTime = dateTime;
        this.time = time;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            Meeting meeting = (Meeting) obj;
            return ObjectsCompat.equals(getId(), meeting.getId()) &&
                    ObjectsCompat.equals(getName(), meeting.getName()) &&
                    ObjectsCompat.equals(getDate(), meeting.getDate()) &&
                    ObjectsCompat.equals(getDateTime(), meeting.getDateTime()) &&
                    ObjectsCompat.equals(getTime(), meeting.getTime()) &&
                    ObjectsCompat.equals(getTimestamp(), meeting.getTimestamp());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getName())
                .append(getDate())
                .append(getDateTime())
                .append(getTime())
                .append(getTimestamp())
                .toString()
                .hashCode();
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
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public static Meeting justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }
        return new Meeting(
                id,
                null,
                null,
                null,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                name,
                date,
                dateTime,
                time,
                timestamp);
    }
    public interface NameStep {
        BuildStep name(String name);
    }


    public interface BuildStep {
        Meeting build();
        BuildStep id(String id) throws IllegalArgumentException;
        BuildStep date(AWSDate date);
        BuildStep dateTime(AWSDateTime dateTime);
        BuildStep time(AWSTime time);
        BuildStep timestamp(AWSTimestamp timestamp);
    }


    public static class Builder implements NameStep, BuildStep {
        private String id;
        private String name;
        private AWSDate date;
        private AWSDateTime dateTime;
        private AWSTime time;
        private AWSTimestamp timestamp;
        @Override
        public Meeting build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new Meeting(
                    id,
                    name,
                    date,
                    dateTime,
                    time,
                    timestamp);
        }

        @Override
        public BuildStep name(String name) {
            Objects.requireNonNull(name);
            this.name = name;
            return this;
        }

        @Override
        public BuildStep date(AWSDate date) {
            this.date = date;
            return this;
        }

        @Override
        public BuildStep dateTime(AWSDateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        @Override
        public BuildStep time(AWSTime time) {
            this.time = time;
            return this;
        }

        @Override
        public BuildStep timestamp(AWSTimestamp timestamp) {
            this.timestamp = timestamp;
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
        private CopyOfBuilder(String id, String name, AWSDate date, AWSDateTime dateTime, AWSTime time, AWSTimestamp timestamp) {
            super.id(id);
            super.name(name)
                    .date(date)
                    .dateTime(dateTime)
                    .time(time)
                    .timestamp(timestamp);
        }

        @Override
        public CopyOfBuilder name(String name) {
            return (CopyOfBuilder) super.name(name);
        }

        @Override
        public CopyOfBuilder date(AWSDate date) {
            return (CopyOfBuilder) super.date(date);
        }

        @Override
        public CopyOfBuilder dateTime(AWSDateTime dateTime) {
            return (CopyOfBuilder) super.dateTime(dateTime);
        }

        @Override
        public CopyOfBuilder time(AWSTime time) {
            return (CopyOfBuilder) super.time(time);
        }

        @Override
        public CopyOfBuilder timestamp(AWSTimestamp timestamp) {
            return (CopyOfBuilder) super.timestamp(timestamp);
        }
    }

}
