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

package com.amplifyframework.datastore.model;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.testmodels.commentsblog.Author;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.reactivex.rxjava3.core.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link CompoundModelProvider}.
 */
public final class CompoundModelProviderTest {
    /**
     * It should be possible to create multiple instance of {@link CompoundModelProvider}
     * from different component providers that have the same UUID. The compound versions
     * should be repeatably the same.
     */
    @Test
    public void compoundVersionIsStable() {
        // Arrange three model providers.
        // The first has one UUID
        // The seconds and third share a UUID.
        String uniqueVersion = UUID.randomUUID().toString();
        ModelProvider first = SimpleModelProvider.instance(uniqueVersion, Blog.class);

        String repeatedVersion = UUID.randomUUID().toString();
        ModelProvider second = SimpleModelProvider.instance(repeatedVersion, BlogOwner.class);
        ModelProvider third = SimpleModelProvider.instance(repeatedVersion, BlogOwner.class);

        // Act: create a couple of compounds, one of (A + B), and another of (A + C).
        CompoundModelProvider firstPlusSecond = CompoundModelProvider.of(first, second);
        CompoundModelProvider firstPlusThird = CompoundModelProvider.of(first, third);

        // Assert: both instances report the same version for the compound,
        // on the basis that the ipnut versions were all the same.
        assertEquals(firstPlusSecond.version(), firstPlusThird.version());
    }

    /**
     * If a component provider A provides models 1, 2,
     * and another provider B provides models 2, 3,
     * The compound shall provide 1,2,3.
     * @throws AmplifyException when converting modelClass to modelSchema
     */
    @Test
    public void compoundProvidesAllComponentModels() throws AmplifyException {
        SimpleModelProvider oneTwoProvider = SimpleModelProvider.withRandomVersion(Blog.class, BlogOwner.class);
        SimpleModelProvider twoThreeProvider = SimpleModelProvider.withRandomVersion(BlogOwner.class, Author.class);
        CompoundModelProvider compound = CompoundModelProvider.of(oneTwoProvider, twoThreeProvider);

        assertEquals(
                Observable.fromArray(
                        ModelSchema.fromModelClass(Author.class),
                        ModelSchema.fromModelClass(BlogOwner.class),
                        ModelSchema.fromModelClass(Blog.class))
                        .toList()
                        .blockingGet(),
                new ArrayList<>(compound.modelSchemas().values())
        );

    }

    /**
     * Check {@link CompoundModelProvider#equals(Object)} and {@link CompoundModelProvider#hashCode()} etc.,
     * for two instances of {@link CompoundModelProvider} that contain the same logical contents.
     */
    @Test
    public void differentInstancesOfSameContentAreEquals() {
        SimpleModelProvider componentProvider = SimpleModelProvider.withRandomVersion(BlogOwner.class);
        CompoundModelProvider first = CompoundModelProvider.of(componentProvider);
        CompoundModelProvider second = CompoundModelProvider.of(componentProvider);

        // The two providers had the same component, so they should have the same versions, too.
        assertEquals(first.version(), second.version());
        // The two providers are considered equals(), on the basis that they have the same version.
        assertEquals(first, second);

        // hashCode() works; adding different instances with same content into a set
        // will result in only one copy.
        Set<CompoundModelProvider> provider = new HashSet<>();
        provider.add(first);
        provider.add(second);
        assertEquals(1, provider.size());
    }

    /**
     * It's alright to have an {@link CompoundModelProvider} that provides no models.
     */
    @Test
    public void possibleToHaveAnEmptyProvider() {
        CompoundModelProvider compoundModelProvider = CompoundModelProvider.of();
        assertTrue(compoundModelProvider.models().isEmpty());
        String emptyStringUuid = UUID.nameUUIDFromBytes("".getBytes()).toString();
        assertEquals(emptyStringUuid, compoundModelProvider.version());
    }

    /**
     * A compound provider of A, B, should NOT be the same as a compound provider of B, A.
     * This is because the provided models have a dependency ordering that is important.
     * For user models, this is the topological ordering of the models. And beyond that,
     * system models must be present before user models. These are business details that
     * are outside the humble scope of a compound model provider's implementation, except
     * for that this component must maintain order, to maintain the external ordering rules.
     */
    @Test
    public void parameterOrderIsSignificant() {
        // Arrange two providers that have inputs provided out-of-order
        SimpleModelProvider one = SimpleModelProvider.withRandomVersion(Blog.class);
        SimpleModelProvider two = SimpleModelProvider.withRandomVersion(BlogOwner.class);
        CompoundModelProvider oneAndTwo = CompoundModelProvider.of(one, two);
        CompoundModelProvider twoAndOne = CompoundModelProvider.of(two, one);

        // They should be equivalent.
        assertNotEquals(oneAndTwo, twoAndOne);

        // And hashCode() should produce a distinct result for each.
        Set<CompoundModelProvider> providers = new HashSet<>();
        providers.add(oneAndTwo);
        providers.add(twoAndOne);
        assertEquals(2, providers.size());
    }
}
