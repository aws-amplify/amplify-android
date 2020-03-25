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
 * Holds the key phrase detection results for the
 * predictions category. Target text itself will be
 * the attribute of this class.
 *
 * Key phrase is a portion of text that is deemed to
 * hold a significant value in determining the context
 * for text analysis.
 */
public final class KeyPhrase extends TextAttribute<String> {
    /**
     * Attribute type for {@link KeyPhrase}.
     */
    public static final String ATTRIBUTE_TYPE = KeyPhrase.class.getSimpleName();

    private KeyPhrase(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getType() {
        return ATTRIBUTE_TYPE;
    }

    /**
     * Gets a builder to construct a key phrase attribute.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link KeyPhrase}.
     */
    public static final class Builder extends TextAttribute.Builder<Builder, KeyPhrase, String> {
        @Override
        @NonNull
        public KeyPhrase build() {
            return new KeyPhrase(this);
        }
    }
}
