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

package com.amplifyframework.testutils.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.testutils.Await;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.config.AWSConfiguration;

import java.util.Objects;

/**
 * A wrapper around the {@link AWSMobileClient} which turns its asynchronous methods
 * into synchronous ones.
 */
public final class SynchronousMobileClient {
    private final AWSMobileClient awsMobileClient;

    private SynchronousMobileClient(AWSMobileClient awsMobileClient) {
        this.awsMobileClient = awsMobileClient;
    }

    /**
     * Creates an instance of the {@link SynchronousMobileClient}, using the
     * default instance of the {@link AWSMobileClient} as provided by {@link AWSMobileClient#getInstance()}.
     * @return A {@link SynchronousMobileClient} using the default {@link AWSMobileClient}
     */
    @NonNull
    public static SynchronousMobileClient instance() {
        return new SynchronousMobileClient(AWSMobileClient.getInstance());
    }

    /**
     * Creates an instance of the {@link SynchronousMobileClient}, using a provided
     * {@link AWSMobileClient} as a delegate.
     * @param awsMobileClient Client to which requests are delegated
     * @return A {@link SynchronousMobileClient} which delegates to the provided AWSMobileClient
     */
    @NonNull
    public static SynchronousMobileClient instance(@NonNull AWSMobileClient awsMobileClient) {
        return new SynchronousMobileClient(Objects.requireNonNull(awsMobileClient));
    }

    /**
     * Initialize the client using {@link AWSMobileClient#initialize(Context, Callback)},
     * and using a context provided by test {@link ApplicationProvider#getApplicationContext()}.
     * @return User state details on success
     * @throws MobileClientException On failure to initialize
     */
    @NonNull
    public UserStateDetails initialize() throws MobileClientException {
        Context context = ApplicationProvider.getApplicationContext();
        return initialize(context);
    }

    /**
     * Initialize the client for use, in a synchronous way, by delegating to
     * {@link AWSMobileClient#initialize(Context, Callback)}. It constructs a default
     * instance of {@link AWSConfiguration} using the provided context to locate
     * awsconfiguration.json file.
     * @param context An Android Context
     * @return The result received by {@link AWSMobileClient#initialize(Context, Callback)}, if successful
     * @throws MobileClientException A wrapped form of the error received by the async callback.
     */
    @NonNull
    public UserStateDetails initialize(@NonNull Context context) throws MobileClientException {
        Objects.requireNonNull(context);
        AWSConfiguration configuration = new AWSConfiguration(context);
        return initialize(context, configuration);
    }

    /**
     * Initialize the client for use, in a synchronous way, by delegating to
     * {@link AWSMobileClient#initialize(Context, Callback)}.
     * @param context An Android Context
     * @param awsConfiguration custom AWS configuration to use for initializing Mobile Client
     * @return The result received by {@link AWSMobileClient#initialize(Context, Callback)}, if successful
     * @throws MobileClientException A wrapped form of the error received by the async callback.
     */
    @NonNull
    public UserStateDetails initialize(@NonNull Context context,
                                       @NonNull AWSConfiguration awsConfiguration) throws MobileClientException {
        Objects.requireNonNull(context);
        Objects.requireNonNull(awsConfiguration);
        final UserStateDetails userStateDetails;
        try {
            userStateDetails = Await.<UserStateDetails, Exception>result((onResult, onError) -> {
                Callback<UserStateDetails> callback = DelegatingCallback.with(onResult, onError);
                awsMobileClient.initialize(context, awsConfiguration, callback);
            });
        } catch (Exception initializationError) {
            throw new MobileClientException("Failed to initialize Mobile Client", initializationError);
        }
        return Objects.requireNonNull(userStateDetails);
    }

    /**
     * Sign in using a user name and password.
     * @param username User name
     * @param password Password for the given user
     * @return A sign in result
     * @throws MobileClientException If sign in fails
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SignInResult signIn(@NonNull String username, @NonNull String password) throws MobileClientException {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        try {
            // Don't trust the AWS Mobile Client. It's not clear if it can return null or not, so check.
            SignInResult result = awsMobileClient.signIn(username, password, null);
            return Objects.requireNonNull(result);
        } catch (Exception baseJavaException) {
            throw new MobileClientException("Failed to sign in as " + username, baseJavaException);
        }
    }

    /**
     * Sign out.
     * @throws MobileClientException On failure to sign out
     */
    public void signOut() throws MobileClientException {
        try {
            awsMobileClient.signOut();
        } catch (Exception baseJavaException) {
            throw new MobileClientException("Failed to sign out", baseJavaException);
        }
    }

    /**
     * Gets the Identity ID of currently signed in user.
     * @return AWS Mobile Client Identity ID
     * @throws MobileClientException On failure to get ID
     */
    @Nullable
    public String getIdentityId() throws MobileClientException {
        try {
            return awsMobileClient.getIdentityId();
        } catch (Exception baseJavaException) {
            throw new MobileClientException("Failed to obtain identity ID", baseJavaException);
        }
    }

    /**
     * A named error that can be thrown if the {@link AWSMobileClient} fails to perform an action.
     * Ordinarily, that would return a base Java {@link Exception}, which is pretty ugly.
     */
    public static final class MobileClientException extends Exception {
        private static final long serialVersionUID = 707443782664477404L;

        /**
         * Constructs a new MobileClientException, with an underlying cause.
         * @param message User-friendly message regarding this exception
         * @param cause The reason the Mobile Client failed
         */
        MobileClientException(@NonNull String message, @NonNull Throwable cause) {
            super(Objects.requireNonNull(message), Objects.requireNonNull(cause));
        }
    }

    /**
     * An {@link Callback} which works by delegating its result and error
     * values to one of two {@link Consumer}s, and taking no other action(s).
     * @param <T> Type of result in callback
     */
    private static final class DelegatingCallback<T> implements Callback<T> {
        private final Consumer<T> onResult;
        private final Consumer<Exception> onError;

        private DelegatingCallback(
                Consumer<T> onResult,
                Consumer<Exception> onError) {
            this.onResult = onResult;
            this.onError = onError;
        }

        /**
         * Creates a delegating callback that delegates callback values to the provided consumers.
         * @param onResult Result consumer
         * @param onError Error consumer
         * @param <T> Type of result
         * @return A delegating callback implementation
         */
        static <T> DelegatingCallback<T> with(@NonNull Consumer<T> onResult, @NonNull Consumer<Exception> onError) {
            Objects.requireNonNull(onResult);
            Objects.requireNonNull(onError);
            return new DelegatingCallback<>(onResult, onError);
        }

        @Override
        public void onResult(T result) {
            onResult.accept(result);
        }

        @Override
        public void onError(Exception error) {
            onError.accept(error);
        }
    }
}
