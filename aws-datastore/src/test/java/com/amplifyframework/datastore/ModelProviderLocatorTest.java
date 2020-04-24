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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Test;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

/**
 * Tests the {@link ModelProviderLocator}.
 */
public final class ModelProviderLocatorTest {
    /**
     * Validate that the ModelProviderLocator can find a ModelProvider by class name,
     * and can make product use of it to load models for the system.
     * @throws DataStoreException Not expect in this test. Possible from {@link ModelProviderLocator#locate(String)}.
     */
    @Test
    public void locateValidModelProvider() throws DataStoreException {
        String className = Objects.requireNonNull(ValidModelProvider.class.getName());
        ModelProvider provider = ModelProviderLocator.locate(className);
        assertNotNull(provider);
        assertEquals(ValidModelProvider.getInstance(), provider);
        assertEquals(Collections.singleton(BoilerPlateModel.class), provider.models());
    }

    /**
     * When the ModelProviderLocator attempts to locate a ModelProvider whose getInstance() method
     * is not accessible, the locator should raise an exception.
     */
    @Test
    public void locateProviderWithoutAccess() {
        String className = Objects.requireNonNull(BadAccessModelProvider.class.getName());
        DataStoreException actualException =
            assertThrows(DataStoreException.class, () -> ModelProviderLocator.locate(className));
        assertEquals(
            "Tried to call " + BadAccessModelProvider.class.getName() + "getInstance" +
            ", but this method did not have public access.",
            actualException.getMessage()
        );
        assertEquals(
            "Validate that " + BadAccessModelProvider.class.getName() +
            " has not been modified since the time it was code-generated.",
            actualException.getRecoverySuggestion()
        );
    }

    /**
     * When the ModelProviderLocator attempts to locate a ModelProvider whose getInstance() method
     * throws an exception, the locator should bubble up that exception as DataStoreException.
     */
    @Test
    public void locateThrowingModelProvider() {
        String className = Objects.requireNonNull(ThrowingModelProvider.class.getName());
        DataStoreException actualException =
            assertThrows(DataStoreException.class, () -> ModelProviderLocator.locate(className));
        assertEquals(
            "An exception was thrown from " + ThrowingModelProvider.class.getName() + "getInstance" +
            " while invoking via reflection.",
            actualException.getMessage()
        );
        assertEquals(
            "This is not expected to occur. Contact AWS.",
            actualException.getRecoverySuggestion()
        );
    }

    /**
     * When the ModelProviderLocator attempts to call locate a class that *is not* an
     * {@link ModelProvider}, the locator should bubble up that exception as DataStoreException.
     */
    @Test
    public void locateNotAModelProvider() {
        String className = Objects.requireNonNull(NotAModelProvider.class.getName());
        DataStoreException actualException =
            assertThrows(DataStoreException.class, () -> ModelProviderLocator.locate(className));
        assertEquals(
            "Located class as " + NotAModelProvider.class.getName() +
            ", but it does not implement com.amplifyframework.core.model.ModelProvider.",
            actualException.getMessage()
        );
        assertEquals(
            "Validate that " + NotAModelProvider.class.getName() +
            " has not been modified since the time it was code-generated.",
            actualException.getRecoverySuggestion()
        );
    }

    /**
     * A {@link ModelProvider} that is designed to be locatable by the {@link ModelProvider}.
     */
    static final class ValidModelProvider extends BoilerPlateModelProvider {
        // This is part of the required contract, even though it isn't in ModelProvider.
        static ValidModelProvider getInstance() {
            return new ValidModelProvider();
        }
    }

    /**
     * This {@link ModelProvider} is built in such a way that {@link ModelProviderLocator}
     * won't have access to its (private) {@link BadAccessModelProvider#getInstance()} factory
     * method.
     */
    private static final class BadAccessModelProvider extends BoilerPlateModelProvider {
        @SuppressWarnings("unused") // It it is used, via reflection, at runtime.
        private static /* note that it said private */ BadAccessModelProvider getInstance() {
            return new BadAccessModelProvider();
        }
    }

    /**
     * This {@link ModelProvider} is built with the intention of throwing an {@link RuntimeException}
     * from inside of {@link ThrowingModelProvider#getInstance()}.
     */
    static final class ThrowingModelProvider extends BoilerPlateModelProvider {
        @SuppressWarnings("unused") // It it is used, via reflection, at runtime.
        static ThrowingModelProvider getInstance() {
            throw new RuntimeException("Something went wrong, bud.");
        }
    }

    /**
     * This is not an {@link ModelProvider} at all. It's just some POJO, that happens to
     * have a method {@link NotAModelProvider#getInstance()}, that looks suspiciously like
     * the one found in the contract of the code-gen'd model providers.
     */
    static final class NotAModelProvider {
        // For sake of argument, let's say it _does_ have the right getInstance().
        // This doesn't matter, since we shouldn't try even get far enough to invoke it.
        @SuppressWarnings("unused") // It it is used, via reflection, at runtime.
        static BoilerPlateModelProvider getInstance() {
            return new BoilerPlateModelProvider();
        }
    }

    /**
     * This is base class to centralize implementations of {@link Object#equals(Object)},
     * {@link Object#hashCode()}, {@link Object#toString()},
     * {@link ModelProvider#models()}, {@link ModelProvider#version()}, etc.
     */
    private static /* extensible */ class BoilerPlateModelProvider implements ModelProvider {
        private static final String STABLE_VERSION = RandomString.string();
        private static final Set<Class<? extends Model>> MODELS = Collections.singleton(BoilerPlateModel.class);

        @Override
        public Set<Class<? extends Model>> models() {
            return MODELS;
        }

        @Override
        public String version() {
            return STABLE_VERSION;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(MODELS, STABLE_VERSION);
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (!(thatObject instanceof ModelProvider)) {
                return false;
            }
            ModelProvider modelProvider = (ModelProvider) thatObject;

            return ObjectsCompat.equals(MODELS, modelProvider.models()) &&
                ObjectsCompat.equals(STABLE_VERSION, modelProvider.version());
        }

        @NonNull
        @Override
        public String toString() {
            return "BoilerPlateModelProvider{" +
                "STABLE_VERSION=" + STABLE_VERSION + ", " +
                "MODELS=" + MODELS +
                "}";
        }
    }

    /**
     * This is provided by the {@link BoilerPlateModelProvider}, and all of its subclasses.
     * The purpose of this {@link BoilerPlateModel} is to have some unique model class that
     * we can validate when we got make assertions on the various ModelProviders we try to
     * discover.
     */
    private static final class BoilerPlateModel implements Model {
        private final String modelId;

        private BoilerPlateModel() {
            this.modelId = RandomString.string();
        }

        @NonNull
        @Override
        public String getId() {
            return modelId;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            BoilerPlateModel that = (BoilerPlateModel) thatObject;

            return ObjectsCompat.equals(modelId, that.modelId);
        }

        @Override
        public int hashCode() {
            return modelId.hashCode();
        }

        @Override
        public String toString() {
            return "BoilerPlateModel{" +
                "modelId='" + modelId + '\'' +
                '}';
        }
    }
}
