/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.amplifyframework.core.model.ModelProvider
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

/**
 * Finds the code-generated [ModelProvider] on the class path.
 *
 * Codegen emits the provider at a fixed fully-qualified name with a static `getInstance()`. None of
 * that is verifiable at compile time — the provider lives in the consuming application, not here — so
 * it is resolved reflectively and every way that can fail is reported as a configuration error the
 * caller can act on.
 */
internal object AppSyncModelProviderLocator {

    private const val DEFAULT_CLASS_NAME = "com.amplifyframework.datastore.generated.model.AmplifyModelProvider"
    private const val ACCESSOR_NAME = "getInstance"

    fun locate(className: String = DEFAULT_CLASS_NAME): ModelProvider {
        val providerClass = try {
            Class.forName(className)
        } catch (error: ClassNotFoundException) {
            throw AppSyncInvalidConfigException(
                message = "Could not find the code-generated model provider $className.",
                recoverySuggestion = "Verify that codegen has run and that $className is compiled into the app.",
                cause = error
            )
        }

        if (!ModelProvider::class.java.isAssignableFrom(providerClass)) {
            throw AppSyncInvalidConfigException(
                message = "Found $className, but it does not implement ${ModelProvider::class.java.name}.",
                recoverySuggestion = "Verify that $className has not been modified since it was generated."
            )
        }

        val accessor = try {
            providerClass.getDeclaredMethod(ACCESSOR_NAME)
        } catch (error: NoSuchMethodException) {
            throw AppSyncInvalidConfigException(
                message = "Found the model provider $className, but it has no $ACCESSOR_NAME() method.",
                recoverySuggestion = "Verify that $className has not been modified since it was generated.",
                cause = error
            )
        }

        // Invoked with a null receiver below, so a non-static accessor would fail as an unhelpful
        // NullPointerException rather than as the configuration problem it is.
        if (!Modifier.isStatic(accessor.modifiers)) {
            throw AppSyncInvalidConfigException(
                message = "The $ACCESSOR_NAME() method on $className is not static.",
                recoverySuggestion = "Verify that $className has not been modified since it was generated."
            )
        }

        val provider = try {
            accessor.invoke(null)
        } catch (error: IllegalAccessException) {
            throw AppSyncInvalidConfigException(
                message = "The $ACCESSOR_NAME() method on $className is not accessible.",
                recoverySuggestion = "Verify that $className has not been modified since it was generated.",
                cause = error
            )
        } catch (error: InvocationTargetException) {
            throw AppSyncUnknownException(
                message = "Calling $ACCESSOR_NAME() on $className threw ${error.targetException}.",
                cause = error.targetException ?: error
            )
        }

        return provider as? ModelProvider ?: throw AppSyncInvalidConfigException(
            message = "The $ACCESSOR_NAME() method on $className returned no model provider.",
            recoverySuggestion = "Verify that $className has not been modified since it was generated."
        )
    }
}
