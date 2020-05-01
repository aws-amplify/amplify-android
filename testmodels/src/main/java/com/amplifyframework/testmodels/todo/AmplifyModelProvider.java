package com.amplifyframework.testmodels.todo;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.util.Immutable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains the set of model classes that implement {@link Model}
 * interface.
 */

public final class AmplifyModelProvider implements ModelProvider {
    private static final String AMPLIFY_MODEL_VERSION = "0777ec8dbcac6dc2cb4bf9f80f0e4ab7";
    private static AmplifyModelProvider amplifyGeneratedModelInstance;

    private AmplifyModelProvider() {

    }

    public static AmplifyModelProvider getInstance() {
        if (amplifyGeneratedModelInstance == null) {
            amplifyGeneratedModelInstance = new AmplifyModelProvider();
        }
        return amplifyGeneratedModelInstance;
    }

    /**
     * Get a set of the model classes.
     *
     * @return a set of the model classes.
     */
    @Override
    public Set<Class<? extends Model>> models() {
        final Set<Class<? extends Model>> modifiableSet = new HashSet<>(
                Arrays.<Class<? extends Model>>asList(Todo.class)
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
