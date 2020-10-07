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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.TypeMaker;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests the {@link ConflictUnhandledError} class.
 */
public final class ConflictUnhandledErrorTest {
    private Gson gson;

    /**
     * Obtain a Gson instance to use while arranging cooked GraphQL responses.
     */
    @Before
    public void setup() {
        this.gson = GsonFactory.instance();
    }

    /**
     * When a null error list is provided, the {@link ConflictUnhandledError#findFirst(Class, List)}
     * factory should return null, meaning that there was no conflict error found in the
     * provided list.
     */
    @Test
    public void noConflictErrorFoundInNullErrorList() {
        ConflictUnhandledError<BlogOwner> conflictUnhandledError =
            ConflictUnhandledError.findFirst(BlogOwner.class, null);
        assertNull(conflictUnhandledError);
    }

    /**
     * When an empty list is provided, the {@link ConflictUnhandledError#findFirst(Class, List)}
     * factory should return null, indicating that there was no conflict error found.
     */
    @Test
    public void noConflictErrorFoundInEmptyErrorList() {
        ConflictUnhandledError<BlogOwner> conflictUnhandledError =
            ConflictUnhandledError.findFirst(BlogOwner.class, Collections.emptyList());
        assertNull(conflictUnhandledError);
    }

    /**
     * When a non-empty list is provided to the {@link ConflictUnhandledError#findFirst(Class, List)}
     * factory, but there are no relevant errors in the list, then null should be returned,
     * indicating that no conflict error was found in the list.
     */
    @Test
    public void noConflictErrorFoundInUnrelatedErrorsList() {
        List<GraphQLResponse.Error> unrelatedErrors = Arrays.asList(
            new GraphQLResponse.Error(
                "unrelated1",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap()
            ),
            new GraphQLResponse.Error(
                "unrelated2",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap()
            )
        );
        ConflictUnhandledError<BlogOwner> conflictUnhandledError =
            ConflictUnhandledError.findFirst(BlogOwner.class, unrelatedErrors);
        assertNull(conflictUnhandledError);
    }

    /**
     * When a conflict error *is* present in the list provided to {@link ConflictUnhandledError#findFirst(Class, List)},
     * then a model for it should be returned from the factory method. That model should contain representation
     * of the serer version of the model that is currently in conflict.
     */
    @Test
    public void conflictErrorExtractedIfPresent() {
        String responseJson = Resources.readAsString("conflict-unhandled-response.json");
        Type type = TypeMaker.getParameterizedType(GraphQLResponse.class, ModelWithMetadata.class, Note.class);
        GraphQLResponse<ModelWithMetadata<Note>> response = gson.fromJson(responseJson, type);
        ConflictUnhandledError<Note> conflictUnhandledError =
            ConflictUnhandledError.findFirst(Note.class, response.getErrors());
        assertNotNull(conflictUnhandledError);

        // TODO: The JSON document has '1601499066604' as the time. These differ by 26604,
        // also the TimeUnit appears to be wrong. Should be MILLISECONDS.
        Temporal.Timestamp lastChangedAt = new Temporal.Timestamp(1601499040000L, TimeUnit.SECONDS);
        assertEquals(
            new ModelWithMetadata<>(
                new Note("KoolId22", "Resurecting the dataz"),
                new ModelMetadata("KoolId22", true, 7, lastChangedAt)
            ),
            conflictUnhandledError.getServerVersion()
        );
    }

    /**
     * This is a model for the data that is contained in
     * src/test/resources/conflict-unhandled-response.json.
     */
    @SuppressWarnings("checkstyle:ParameterName")
    static final class Note implements Model {
        private final String id;
        private final String content;

        Note(String id, String content) {
            this.id = id;
            this.content = content;
        }

        @NonNull
        @Override
        public String getId() {
            return id;
        }

        @NonNull
        String getContent() {
            return content;
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Note note = (Note) thatObject;

            if (!getId().equals(note.getId())) {
                return false;
            }
            return getContent().equals(note.getContent());
        }

        @Override
        public int hashCode() {
            int result = getId().hashCode();
            result = 31 * result + getContent().hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Note{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                '}';
        }
    }
}
