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

package com.amplifyframework.predictions.models;

import androidx.annotation.NonNull;

/**
 * Syntax holds the information regarding what
 * part of speech is represented by the target
 * portion of the text.
 */
public final class Syntax extends TextFeature<SpeechType> {

    private Syntax(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getTypeAlias() {
        return FeatureType.SYNTAX.getAlias();
    }

    /**
     * Gets a builder to construct a speech type feature.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Syntax}.
     */
    public static final class Builder extends TextFeature.Builder<Builder, Syntax, SpeechType> {
        @Override
        @NonNull
        public Syntax build() {
            return new Syntax(this);
        }
    }
}
