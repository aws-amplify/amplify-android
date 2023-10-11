/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.core.model.ModelProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * This utility class will inspect the JVM's Class loader to find a class named
 * "com.amplifyframework.api.generated.model.AmplifyModelProvider". This is the package
 * and class name that the Amplify CLI Code Generator tool uses when creating an {@link ModelProvider}
 * instance to be consumed inside of the user's consuming application. This class, by contract,
 * has public-access static method named `getInstance()` which returns ModelProvider.
 *
 * There is no compile-time guarantee that this class actually exists, or that it has that method,
 * or that we can use that method. But in the "happy path," this simplifies the amount of complexity
 * needed to begin using the API category. This utility class is used from
 * {@link com.amplifyframework.api.aws.AWSApiPlugin#initialize}.
 */
final class ModelProviderLocator {
    private static final String DEFAULT_MODEL_PROVIDER_CLASS_NAME =
            "com.amplifyframework.datastore.generated.model.AmplifyModelProvider";
    private static final String GET_INSTANCE_ACCESSOR_METHOD_NAME = "getInstance";

    private ModelProviderLocator() {}

    /**
     * Locate the code-generated model provider.
     * @return The code-generated model provider, if found
     * @throws ApiException If unable to find the code-generated model provider
     */
    static ModelProvider locate() throws ApiException {
        return locate(DEFAULT_MODEL_PROVIDER_CLASS_NAME);
    }

    @SuppressWarnings({"SameParameterValue", "unchecked"})
    static ModelProvider locate(@NonNull String modelProviderClassName) throws ApiException {
        Objects.requireNonNull(modelProviderClassName);
        final Class<? extends ModelProvider> modelProviderClass;
        try {
            //noinspection unchecked It's very unlikely that someone cooked up a different type at this FQCN.
            modelProviderClass = (Class<? extends ModelProvider>) Class.forName(modelProviderClassName);
        } catch (ClassNotFoundException modelProviderClassNotFoundError) {
            throw new ApiException(
                    "Failed to find code-generated model provider.", modelProviderClassNotFoundError,
                    "Validate that " + modelProviderClassName + " is built into your project."
            );
        }
        if (!ModelProvider.class.isAssignableFrom(modelProviderClass)) {
            throw new ApiException(
                    "Located class as " + modelProviderClass.getName() + ", but it does not implement " +
                            ModelProvider.class.getName() + ".",
                    "Validate that " + modelProviderClass.getName() + " has not been modified since the time " +
                            "it was code-generated."
            );
        }
        final Method getInstanceMethod;
        try {
            getInstanceMethod = modelProviderClass.getDeclaredMethod(GET_INSTANCE_ACCESSOR_METHOD_NAME);
        } catch (NoSuchMethodException noGetInstanceMethodError) {
            throw new ApiException(
                    "Found a code-generated model provider = " + modelProviderClass.getName() + ", however " +
                            "it had no static method named getInstance()!",
                    noGetInstanceMethodError,
                    "Validate that " + modelProviderClass.getName() + " has not been modified since the time " +
                            "it was code-generated."
            );
        }
        final ModelProvider locatedModelProvider;
        try {
            locatedModelProvider = (ModelProvider) getInstanceMethod.invoke(null);
        } catch (IllegalAccessException getInstanceIsNotAccessibleError) {
            throw new ApiException(
                    "Tried to call " + modelProviderClass.getName() + GET_INSTANCE_ACCESSOR_METHOD_NAME + ", but " +
                            "this method did not have public access.", getInstanceIsNotAccessibleError,
                    "Validate that " + modelProviderClass.getName() + " has not been modified since the time " +
                            "it was code-generated."
            );
        } catch (InvocationTargetException wrappedExceptionFromGetInstance) {
            throw new ApiException(
                    "An exception was thrown from " + modelProviderClass.getName() + GET_INSTANCE_ACCESSOR_METHOD_NAME +
                            " while invoking via reflection.", wrappedExceptionFromGetInstance,
                    "This is not expected to occur. Contact AWS."
            );
        }

        return locatedModelProvider;
    }
}
