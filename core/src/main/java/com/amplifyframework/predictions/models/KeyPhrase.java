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
 * Holds the key phrase detection results for the predictions category.
 * Target text itself will be the feature of this class.
 *
 * A key phrase is a string containing a noun phrase that describes a
 * particular thing. It generally consists of a noun and the modifiers
 * that distinguish it.
 *
 * For example, "a beautiful day" is a key phrase that includes an
 * article ("a"), an adjective ("beautiful"), and a noun ("day").
 */
public final class KeyPhrase extends TextFeature<String> {

    private KeyPhrase(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getTypeAlias() {
        return FeatureType.KEY_PHRASE.getAlias();
    }

    /**
     * Gets a builder to construct a key phrase feature.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link KeyPhrase}.
     */
    public static final class Builder extends TextFeature.Builder<Builder, KeyPhrase, String> {
        @Override
        @NonNull
        public KeyPhrase build() {
            return new KeyPhrase(this);
        }
    }
}
