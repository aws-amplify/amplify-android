package com.amplifyframework.testmodels.todo;


import java.util.Objects;

import androidx.core.util.ObjectsCompat;

/**
 * This is an auto generated class representing the TodoOwner type in your schema.
 */
public final class TodoOwner {
    private final String name;
    private final String email;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    private TodoOwner(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            TodoOwner todoOwner = (TodoOwner) obj;
            return ObjectsCompat.equals(getName(), todoOwner.getName()) &&
                    ObjectsCompat.equals(getEmail(), todoOwner.getEmail());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getName())
                .append(getEmail())
                .toString()
                .hashCode();
    }

    public static NameStep builder() {
        return new Builder();
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(name,
                email);
    }

    public interface NameStep {
        BuildStep name(String name);
    }


    public interface BuildStep {
        TodoOwner build();

        BuildStep email(String email);
    }


    public static class Builder implements NameStep, BuildStep {
        private String name;
        private String email;

        @Override
        public TodoOwner build() {

            return new TodoOwner(
                    name,
                    email);
        }

        @Override
        public BuildStep name(String name) {
            Objects.requireNonNull(name);
            this.name = name;
            return this;
        }

        @Override
        public BuildStep email(String email) {
            this.email = email;
            return this;
        }
    }


    public final class CopyOfBuilder extends Builder {
        private CopyOfBuilder(String name, String email) {
            super.name(name)
                    .email(email);
        }

        @Override
        public CopyOfBuilder name(String name) {
            return (CopyOfBuilder) super.name(name);
        }

        @Override
        public CopyOfBuilder email(String email) {
            return (CopyOfBuilder) super.email(email);
        }
    }

}
