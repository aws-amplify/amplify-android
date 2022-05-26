/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Asserts that SQLite model tree can traverse model relationship accurately.
 */
@RunWith(RobolectricTestRunner.class)
public final class SQLiteModelTreeTest {
    private SQLiteModelTree modelTree;
    private SchemaRegistry registry;
    private SQLiteDatabase database;

    /**
     * Sets up model registry and in-memory database.
     * @throws AmplifyException if model fails to register.
     */
    @Before
    public void setUp() throws AmplifyException {
        SQLiteDatabase.OpenParams openParams = new SQLiteDatabase.OpenParams.Builder().build();
        registry = SchemaRegistry.instance();
        database = SQLiteDatabase.createInMemory(openParams);
        modelTree = new SQLiteModelTree(registry, database);

        // Create tables for in-memory database
        registry.register(new HashSet<>(Arrays.asList(A.class, B.class, C.class, D.class)));
        database.execSQL("CREATE TABLE 'A' (id String);");
        database.execSQL("CREATE TABLE 'B' (id String, aId String);");
        database.execSQL("CREATE TABLE 'C' (id String, bId String);");
        database.execSQL("CREATE TABLE 'D' ('@@primaryKey' String, name String, title String,cId String);");
    }

    /**
     * Clears up model registry and closes in-memory database.
     */
    @After
    public void clear() {
        registry.clear();
        database.close();
    }

    /**
     * Tests that SQLite model tree can get the descendants of model
     * up to two levels deep.
     */
    @Test
    public void testDescendantsOfTwoLevelsDeepRelationship() {
        // Insert items into database
        //      A1
        //     /  \
        //    B2  B3
        //   /  \   \
        //  C4  C5  C6
        database.execSQL("INSERT INTO 'A' (id) values (1)");
        database.execSQL("INSERT INTO 'B' (id, aId) values (2, 1)");
        database.execSQL("INSERT INTO 'B' (id, aId) values (3, 1)");
        database.execSQL("INSERT INTO 'C' (id, bId) values (4, 2)");
        database.execSQL("INSERT INTO 'C' (id, bId) values (5, 2)");
        database.execSQL("INSERT INTO 'C' (id, bId) values (6, 3)");
        database.execSQL("INSERT INTO 'D' ('@@primaryKey', name, title, cId) " +
                "values ('\"Barack\"#\"POTUS\"', 'Barack', 'POTUS', 6)");

        // Assert that descendants of A is everything else in the database
        List<Model> expectedDescendantsOfA = Arrays.asList(new B(2), new B(3), new C(4),
                new C(5), new C(6),
                new D("Barack", "POTUS"));
        List<Model> descendantsOfA = modelTree.descendantsOf(Collections.singleton(new A(1)));
        for (Model expectedModel : expectedDescendantsOfA) {
            assertTrue(descendantsOfA.contains(expectedModel));
        }

        // Assert that descendants of B2 is just C4 and C5
        List<Model> expectedDescendantsOfB = Arrays.asList(new C(4), new C(5));
        List<Model> descendantsOfB = modelTree.descendantsOf(Collections.singleton(new B(2)));
        for (Model expectedModel : expectedDescendantsOfB) {
            assertTrue(descendantsOfB.contains(expectedModel));
        }

        // Assert that descendants of B2 & B3 is every C
        List<Model> expectedDescendantsOfB2B3 = Arrays.asList(new C(4), new C(5), new C(6));
        List<Model> descendantsOfB2B3 = modelTree.descendantsOf(Arrays.asList(new B(2), new B(3)));
        for (Model expectedModel : expectedDescendantsOfB2B3) {
            assertTrue(descendantsOfB2B3.contains(expectedModel));
        }

        // Assert that C has 1 child
        List<Model> expectedDescendantsOfC = Collections.singletonList(new D("Barack", "POTUS"));
        List<Model> descendantsOfC = modelTree.descendantsOf(Collections.singleton(new C(6)));
        for (Model expectedModel : expectedDescendantsOfC) {
            assertTrue(descendantsOfC.contains(expectedModel));
        }

        // Assert that D does not have any children
        assertTrue(modelTree.descendantsOf(Collections.singleton(new D("Barack", "POTUS"))).isEmpty());
    }

    // Test models only care about ID when comparing
    @SuppressWarnings("all")
    private abstract static class TestModel implements Model {
        @Override
        public boolean equals(Object obj) {
            return getPrimaryKeyString().equals(((Model) obj).getPrimaryKeyString());
        }

        @Override
        public int hashCode() {
           return new StringBuilder()
                    .append(getPrimaryKeyString()).hashCode();
        }
    }

    // A has one-to-many relationship with B
    @SuppressWarnings("checkstyle:all")
    @ModelConfig(type = Model.Type.USER, version = 1)
    private class A extends TestModel {
        @ModelField(targetType = "ID") private final String id;
        @ModelField(targetType = "B") @HasMany(associatedWith = "a", type = B.class) private List<B> b;
        @NonNull public String resolveIdentifier() { return id; }
        private A(int id) { this.id = Integer.toString(id); }
    }

    // B has one-to-many relationship with C
    // B belongs to A
    @SuppressWarnings("checkstyle:all")
    @ModelConfig(type = Model.Type.USER, version = 1)
    private class B extends TestModel {
        @ModelField(targetType = "ID") private final String id;
        @ModelField(targetType = "C") @HasMany(associatedWith = "b", type = C.class) private List<C> c;
        @ModelField(targetType = "A") @BelongsTo(targetNames = {"aId"}, type = A.class) private A a;
        @NonNull public String resolveIdentifier() { return id; }
        private B(int id) { this.id = Integer.toString(id); }
    }

    // C belongs to B
    @SuppressWarnings("checkstyle:all")
    @ModelConfig(type = Model.Type.USER, version = 1)
    private class C extends TestModel {
        @ModelField(targetType = "ID") private final String id;
        @ModelField(targetType = "B") @BelongsTo(targetNames = {"bId"}, type = B.class) private B b;
        @ModelField(targetType = "D") @HasMany(associatedWith = "c", type = D.class) private List<D> d;
        @NonNull public String resolveIdentifier() { return id; }
        private C(int id) { this.id = Integer.toString(id); }
    }

    // C belongs to B
    @SuppressWarnings("checkstyle:all")
    @ModelConfig(type = Model.Type.USER, version = 1)
    @Index(name = "undefined", fields = {"name","title"})
    private class D extends TestModel {
        @ModelField(targetType = "String") private final String name;
        @ModelField(targetType = "String") private final String title;
        @ModelField(targetType = "C") @BelongsTo(targetNames = {"cId"}, type = C.class) private C c;
        @NonNull public DIdentifier resolveIdentifier() { return new DIdentifier(name, title); }
        private D(String name, String title) {
            this.name = name;
            this.title = title;
        }
    }

    private class DIdentifier extends ModelIdentifier<D> {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for Model Primary key class. Takes in partition key and an array of sort keys as parameters.
         *
         * @param name        Partition key.
         * @param title Array of sort keys.
         */
        DIdentifier(String name, String title) {
            super(name, title);
        }
    }
}
