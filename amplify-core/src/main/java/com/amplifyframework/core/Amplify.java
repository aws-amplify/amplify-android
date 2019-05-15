/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amplifyframework.core.exception.AmplifyAlreadyConfiguredException;
import com.amplifyframework.core.exception.NoSuchProviderException;
import com.amplifyframework.core.provider.Category;
import com.amplifyframework.core.provider.Provider;

import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;

public class Amplify {

    /**
     * Map of {Category, {providerClass, providerObject}}.
     *
     * {
     *     "AUTH" => {
     *         "AmazonCognitoAuthProvider.class" => "AmazonCognitoAuthProvider@object"
     *     },
     *     "STORAGE" => {
     *         "AmazonS3StorageProvider.class" => "AmazonS3StorageProvider@object"
     *     },
     *     "ANALYTICS" => {
     *         "AmazonPinpointAnalyticsProvider.class" => "AmazonPinpointAnalyticsProvider@object",
     *         "AmazonKinesisAnalyticsProvider.class" => "AmazonKinesisAnalyticsProvider@object"
     *     },
     *     "API" => {
     *         "AWSRESTAPIGatewayProvider.class" => "AWSRESTAPIGatewayProvider@object"
     *     }
     * }
     */
    private static HashMap<Category, HashMap<Class<? extends Provider>, Provider>> providers =
            new HashMap<Category, HashMap<Class<? extends Provider>, Provider>>();

    private static final Object LOCK = new Object();

    public static void configure() throws AmplifyAlreadyConfiguredException, NoSuchProviderException {
        synchronized (LOCK) {
        }
    }

    public static void configure(@NonNull String environment) throws AmplifyAlreadyConfiguredException, NoSuchProviderException {
        synchronized (LOCK) {
        }
    }

    public static void configure(@NonNull JSONObject jsonObject) throws AmplifyAlreadyConfiguredException, NoSuchProviderException {
        synchronized (LOCK) {
        }
    }

    public static void configure(@NonNull JSONObject jsonObject, @NonNull String environment) throws AmplifyAlreadyConfiguredException, NoSuchProviderException {
        synchronized (LOCK) {
        }
    }

    public static <P extends Provider> void add(P provider) {
        synchronized (LOCK) {
            HashMap<Class<? extends Provider>, Provider> providersOfCategory = providers.get(provider.getCategory());
            if (providersOfCategory == null) {
                providersOfCategory = new HashMap<Class<? extends Provider>, Provider>();
            }
            providersOfCategory.put(provider.getClass(), provider);
        }
    }

    public static <P extends Provider> void remove(P provider) {
        synchronized (LOCK) {
            providers.get(provider.getCategory()).remove(provider.getClass());
        }
    }

    public static void reset() {
        synchronized (LOCK) {

        }
    }

    public static <P extends Provider> Provider getProvider(Class<P> providerClass) {
        synchronized (LOCK) {
            for (final HashMap<Class<? extends Provider>, Provider> providersOfCategory: providers.values()) {
                if (providersOfCategory.get(providerClass) != null) {
                    return providersOfCategory.get(providerClass);
                }
            }
            return null;
        }
    }

    public static HashMap<Category, HashMap<Class<? extends Provider>, Provider>> getProviders() {
        synchronized (LOCK) {
            return providers;
        }
    }

    public static Collection<Provider> getProvidersForCategory(Category category) {
        synchronized (LOCK) {
            return providers.get(category).values();
        }
    }

    public static Provider getDefaultProviderOfCategory(Category category) {
        synchronized (LOCK) {
            final HashMap<Class<? extends Provider>, Provider> providersOfCategory = providers.get(category);
            for (Provider provider: providersOfCategory.values()) {
                if (provider.isDefault()) {
                    return provider;
                }
            }
            return null;
        }
    }
}
