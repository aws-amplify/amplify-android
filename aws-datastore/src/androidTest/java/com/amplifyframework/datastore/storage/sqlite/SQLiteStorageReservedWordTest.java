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

package com.amplifyframework.datastore.storage.sqlite;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.StrictMode;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.util.Immutable;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test that reserved SQLite words can be safely used in Model names and fields.
 */
public final class SQLiteStorageReservedWordTest {
    private SynchronousStorageAdapter adapter;

    /**
     * Enables Android Strict Mode, to help catch common errors while using
     * SQLite, such as forgetting to close the database.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.enable();
    }

    /**
     * Remove any old database files, and the re-provision a new storage adapter.
     */
    @Before
    public void setup() {
        TestStorageAdapter.cleanup();
        this.adapter = TestStorageAdapter.create(AmplifyModelProvider.getInstance());
    }

    /**
     * Close the open database, and cleanup any database files that it left.
     */
    @After
    public void teardown() {
        TestStorageAdapter.cleanup(adapter);
    }

    /**
     * Assert that save stores data in the SQLite database correctly.
     * @throws DataStoreException from possible underlying DataStore exceptions
     */
    @Test
    public void saveModelInsertsData() throws DataStoreException {
        final Group group = Group.builder()
                .abort("aborting")
                .build();
        adapter.save(group);
    }

    /**
     * Test querying the saved item in the SQLite database.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedData() throws DataStoreException {
        final Group group = Group.builder()
                .abort("aborting")
                .build();
        adapter.save(group);

        // Get the Group from the database
        final List<Group> groups = adapter.query(Group.class);
        assertEquals(1, groups.size());
        assertTrue(groups.contains(group));
    }

    /**
     * Test querying the saved item in the SQLite database with predicate that includes our sqlite keyword.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataByColumn() throws DataStoreException {
        final Group group = Group.builder()
                .abort("aborting")
                .build();
        adapter.save(group);

        // Get the Group from the database
        final List<Group> groups = adapter.query(
                Group.class,
                Where.matches(Group.ABORT.eq("aborting"))
        );
        assertEquals(1, groups.size());
        assertTrue(groups.contains(group));
    }

    /**
     * Test querying with predicate condition on connected model with id.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithPredicatesOnForeignKeyID() throws DataStoreException {
        final Group group = Group.builder()
                .abort("aborting")
                .build();
        adapter.save(group);

        final Alter alter = Alter.builder()
                .index("index")
                .group(group)
                .build();
        adapter.save(alter);

        final List<Alter> altersByGroup = adapter.query(
                Alter.class,
                Where.matches(field("Group_id").eq(group.id))
        );
        assertTrue(altersByGroup.contains(alter));
    }

    /**
     * Test querying with predicate condition on connected model.
     * @throws DataStoreException On unexpected failure manipulating items in/out of DataStore
     */
    @Test
    public void querySavedDataWithPredicatesOnForeignKeyColumn() throws DataStoreException {
        final Group group = Group.builder()
                .abort("aborting")
                .build();
        adapter.save(group);

        final Alter alter = Alter.builder()
                .index("index")
                .group(group)
                .build();
        adapter.save(alter);

        final List<Alter> altersByGroup = adapter.query(
                Alter.class,
                Where.matches(Group.ABORT.eq("aborting"))
        );
        assertTrue(altersByGroup.contains(alter));
    }

    @SuppressWarnings("all")
    @ModelConfig(pluralName = "Groups")
    public static final class Group implements Model {
        public static final QueryField ID = field("id");
        public static final QueryField ABORT = field("abort");
        private final @ModelField(targetType="ID", isRequired = true) String id;
        private final @ModelField(targetType="String", isRequired = true) String abort;
        private final @ModelField(targetType="Alter") @HasMany(associatedWith = "group", type = Alter.class) List<Alter> alters = null;
        public String getId() {
            return id;
        }

        public String getAbort() {
            return abort;
        }

        public List<Alter> getAlters() {
            return alters;
        }

        private Group(String id, String abort) {
            this.id = id;
            this.abort = abort;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if(obj == null || getClass() != obj.getClass()) {
                return false;
            } else {
                Group group = (Group) obj;
                return ObjectsCompat.equals(getId(), group.getId()) &&
                        ObjectsCompat.equals(getAbort(), group.getAbort());
            }
        }

        @Override
        public int hashCode() {
            return new StringBuilder()
                    .append(getId())
                    .append(getAbort())
                    .toString()
                    .hashCode();
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("Group {")
                    .append("id=" + String.valueOf(getId()) + ", ")
                    .append("abort=" + String.valueOf(getAbort()))
                    .append("}")
                    .toString();
        }

        public static AbortStep builder() {
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
        public static Group justId(String id) {
            try {
                UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
            } catch (Exception exception) {
                throw new IllegalArgumentException(
                        "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                                "of an existing object with only its ID field for sending as a mutation parameter. When " +
                                "creating a new object, use the standard builder method and leave the ID field blank."
                );
            }
            return new Group(
                    id,
                    null
            );
        }

        public CopyOfBuilder copyOfBuilder() {
            return new CopyOfBuilder(id,
                    abort);
        }
        public interface AbortStep {
            BuildStep abort(String abort);
        }


        public interface BuildStep {
            Group build();
            BuildStep id(String id) throws IllegalArgumentException;
        }


        public static class Builder implements AbortStep, BuildStep {
            private String id;
            private String abort;
            @Override
            public Group build() {
                String id = this.id != null ? this.id : UUID.randomUUID().toString();

                return new Group(
                        id,
                        abort);
            }

            @Override
            public BuildStep abort(String abort) {
                Objects.requireNonNull(abort);
                this.abort = abort;
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
            private CopyOfBuilder(String id, String abort) {
                super.id(id);
                super.abort(abort);
            }

            @Override
            public CopyOfBuilder abort(String abort) {
                return (CopyOfBuilder) super.abort(abort);
            }
        }

    }

    @SuppressWarnings("all")
    @ModelConfig(pluralName = "Alters")
    @Index(name = "byGroup", fields = {"groupID"})
    public static final class Alter implements Model {
        public static final QueryField ID = field("id");
        public static final QueryField INDEX = field("index");
        public static final QueryField GROUP = field("groupID");
        private final @ModelField(targetType="ID", isRequired = true) String id;
        private final @ModelField(targetType="String", isRequired = true) String index;
        private final @ModelField(targetType="Group") @BelongsTo(targetName = "groupID", type = Group.class) Group group;
        public String getId() {
            return id;
        }

        public String getIndex() {
            return index;
        }

        public Group getGroup() {
            return group;
        }

        private Alter(String id, String index, Group group) {
            this.id = id;
            this.index = index;
            this.group = group;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if(obj == null || getClass() != obj.getClass()) {
                return false;
            } else {
                Alter alter = (Alter) obj;
                return ObjectsCompat.equals(getId(), alter.getId()) &&
                        ObjectsCompat.equals(getIndex(), alter.getIndex()) &&
                        ObjectsCompat.equals(getGroup(), alter.getGroup());
            }
        }

        @Override
        public int hashCode() {
            return new StringBuilder()
                    .append(getId())
                    .append(getIndex())
                    .append(getGroup())
                    .toString()
                    .hashCode();
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("Alter {")
                    .append("id=" + String.valueOf(getId()) + ", ")
                    .append("index=" + String.valueOf(getIndex()) + ", ")
                    .append("group=" + String.valueOf(getGroup()))
                    .append("}")
                    .toString();
        }

        public static IndexStep builder() {
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
        public static Alter justId(String id) {
            try {
                UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
            } catch (Exception exception) {
                throw new IllegalArgumentException(
                        "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                                "of an existing object with only its ID field for sending as a mutation parameter. When " +
                                "creating a new object, use the standard builder method and leave the ID field blank."
                );
            }
            return new Alter(
                    id,
                    null,
                    null
            );
        }

        public CopyOfBuilder copyOfBuilder() {
            return new CopyOfBuilder(id,
                    index,
                    group);
        }
        public interface IndexStep {
            BuildStep index(String index);
        }


        public interface BuildStep {
            Alter build();
            BuildStep id(String id) throws IllegalArgumentException;
            BuildStep group(Group group);
        }


        public static class Builder implements IndexStep, BuildStep {
            private String id;
            private String index;
            private Group group;
            @Override
            public Alter build() {
                String id = this.id != null ? this.id : UUID.randomUUID().toString();

                return new Alter(
                        id,
                        index,
                        group);
            }

            @Override
            public BuildStep index(String index) {
                Objects.requireNonNull(index);
                this.index = index;
                return this;
            }

            @Override
            public BuildStep group(Group group) {
                this.group = group;
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
            private CopyOfBuilder(String id, String index, Group group) {
                super.id(id);
                super.index(index)
                        .group(group);
            }

            @Override
            public CopyOfBuilder index(String index) {
                return (CopyOfBuilder) super.index(index);
            }

            @Override
            public CopyOfBuilder group(Group group) {
                return (CopyOfBuilder) super.group(group);
            }
        }

    }

    public static final class AmplifyModelProvider implements ModelProvider {
        private static final String AMPLIFY_MODEL_VERSION = "305719dace2b972243b439fbd551fe1a";
        private static AmplifyModelProvider amplifyGeneratedModelInstance;

        private AmplifyModelProvider() {}

        /**
         * Instance of AmplifyModelProvider.
         *
         * @return Instance of AmplifyModelProvider
         */
        public static AmplifyModelProvider getInstance() {
            if (amplifyGeneratedModelInstance == null) {
                amplifyGeneratedModelInstance = new AmplifyModelProvider();
            }
            return amplifyGeneratedModelInstance;
        }

        /**
         * Get a set of the model classes.
         *
         * @return a set of the model classes
         */
        @Override
        public Set<Class<? extends Model>> models() {
            final Set<Class<? extends Model>> modifiableSet = new HashSet<>(
                    Arrays.<Class<? extends Model>>asList(Group.class, Alter.class)
            );

            return Immutable.of(modifiableSet);
        }

        /**
         * Get the version of the models.
         *
         * @return the version string of the models.
         */
        @Override
        public String version() {
            return AMPLIFY_MODEL_VERSION;
        }
    }

}
