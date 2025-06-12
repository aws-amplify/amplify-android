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

package com.amplifyframework.auth.cognito.options;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.AuthFactorType;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.util.Immutable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cognito extension of sign in options to add the platform specific fields.
 */
public final class AWSCognitoAuthSignInOptions extends AuthSignInOptions {
    private final Map<String, String> metadata;
    private final AuthFlowType authFlowType;
    private final AuthFactorType preferredFirstFactor;
    private final WeakReference<Activity> callingActivity;

    /**
     * Advanced options for signing in.
     *
     * @param metadata Additional custom attributes to be sent to the service such as information about the client
     * @param authFlowType AuthFlowType to be used by signIn API
     * @param preferredFirstFactor The preferred authentication factor to use, if available.
     *                             This is only used if authFlowType is USER_AUTH.
     * @param callingActivity The Activity reference to use when showing the PassKey UI.
     *                        This is only used if authFlowType is USER_AUTH and WebAuthn is
     *                        used to sign in.
     */
    protected AWSCognitoAuthSignInOptions(
            @NonNull Map<String, String> metadata,
            AuthFlowType authFlowType,
            AuthFactorType preferredFirstFactor,
            WeakReference<Activity> callingActivity
    ) {
        this.metadata = metadata;
        this.authFlowType = authFlowType;
        this.preferredFirstFactor = preferredFirstFactor;
        this.callingActivity = callingActivity;
    }

    /**
     * Get custom attributes to be sent to the service such as information about the client.
     *
     * @return custom attributes to be sent to the service such as information about the client
     */
    @NonNull
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Get authFlowType to be sent to the service.
     *
     * @return authFlowType to be sent to the signIn api
     */
    @Nullable
    public AuthFlowType getAuthFlowType() {
        return authFlowType;
    }

    /**
     * Get the preferred {@link AuthFactorType} to use when signing in with USER_AUTH. If that
     * AuthFactorType is available for the user signing in then it will be used to authenticate
     * the user, otherwise another factor may be used or the user may be prompted to select a
     * factor.
     * @return The preferred {@link AuthFactorType} to use, if available.
     */
    @Nullable
    public AuthFactorType getPreferredFirstFactor() {
        return preferredFirstFactor;
    }

    /**
     * Get the Activity reference to use when showing the PassKey UI. This is only used if
     * authFlowType is USER_AUTH and WebAuthn is used to sign in.
     * @return The Activity reference
     */
    @NonNull
    public WeakReference<Activity> getCallingActivity() {
        return callingActivity;
    }

    /**
     * Get a builder object.
     *
     * @return a builder object.
     */
    @NonNull
    public static CognitoBuilder builder() {
        return new CognitoBuilder();
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(getMetadata(), getAuthFlowType(), getPreferredFirstFactor(), getCallingActivity());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AWSCognitoAuthSignInOptions authSignInOptions = (AWSCognitoAuthSignInOptions) obj;
            return ObjectsCompat.equals(getMetadata(), authSignInOptions.getMetadata()) &&
                    ObjectsCompat.equals(getAuthFlowType(), authSignInOptions.getAuthFlowType()) &&
                    ObjectsCompat.equals(getPreferredFirstFactor(),
                            authSignInOptions.getPreferredFirstFactor()) &&
                    ObjectsCompat.equals(getCallingActivity(), authSignInOptions.getCallingActivity());
        }
    }

    @Override
    public String toString() {
        return "AWSCognitoAuthSignInOptions{" +
                "metadata=" + getMetadata() +
                ", authFlowType=" + getAuthFlowType() +
                ", preferredFirstFactor=" + getPreferredFirstFactor() +
                ", callingActivity=" + getCallingActivity() +
                '}';
    }

    /**
     * The builder for this class.
     */
    public static final class CognitoBuilder extends Builder<CognitoBuilder> {
        private final Map<String, String> metadata;
        private AuthFlowType authFlowType;
        private AuthFactorType preferredFirstFactor;
        private WeakReference<Activity> callingActivity = new WeakReference<>(null);

        /**
         * Constructor for the builder.
         */
        public CognitoBuilder() {
            super();
            this.metadata = new HashMap<>();
        }

        /**
         * Returns the type of builder this is to support proper flow with it being an extended class.
         *
         * @return the type of builder this is to support proper flow with it being an extended class.
         */
        @Override
        public CognitoBuilder getThis() {
            return this;
        }

        /**
         * Set the metadata field for the object being built.
         *
         * @param metadata Custom user metadata to be sent with the sign in request.
         * @return The builder object to continue building.
         */
        @NonNull
        public CognitoBuilder metadata(@NonNull Map<String, String> metadata) {
            Objects.requireNonNull(metadata);
            this.metadata.clear();
            this.metadata.putAll(metadata);
            return getThis();
        }

        /**
         * Set the authFlowType for the object being built.
         *
         * @param authFlowType authFlowType to be sent to sign in request.
         * @return The builder object to continue building.
         */
        @NonNull
        public CognitoBuilder authFlowType(@NonNull AuthFlowType authFlowType) {
            this.authFlowType = authFlowType;
            return getThis();
        }

        /**
         * Set the preferred {@link AuthFactorType} to use when signing in with USER_AUTH. If that
         * AuthFactorType is available for the user signing in then it will be used to authenticate
         * the user. If this option is not set or is not available for the user then another factor
         * may be used or the user may be prompted to select a factor.
         * @param factorType The preferred factor.
         * @return The builder object to continue building.
         */
        @NonNull
        public CognitoBuilder preferredFirstFactor(@Nullable AuthFactorType factorType) {
            this.preferredFirstFactor = factorType;
            return getThis();
        }

        /**
         * Set the Activity reference to use when showing the PassKey UI. This is only used if
         * authFlowType is USER_AUTH and WebAuthn is used to sign in. This option should always be
         * set if your app may be expecting to use WebAuthn, as not setting this option will lead
         * to a sub-optimal user experience when authenticating via WebAuthn.
         * @param callingActivity The Activity instance. This is stored in a WeakReference so that
         *                        it will not be leaked.
         * @return The builder object to continue building.
         */
        @NonNull
        public CognitoBuilder callingActivity(@NonNull Activity callingActivity) {
            this.callingActivity = new WeakReference<>(callingActivity);
            return getThis();
        }

        /**
         * Construct and return the object with the values set in the builder.
         *
         * @return a new instance of AWSCognitoAuthSignInOptions with the values specified in the builder.
         */
        @NonNull
        public AWSCognitoAuthSignInOptions build() {
            return new AWSCognitoAuthSignInOptions(
                    Immutable.of(metadata),
                    authFlowType,
                    preferredFirstFactor,
                    callingActivity
            );
        }
    }
}
