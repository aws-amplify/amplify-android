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

package com.amplifyframework.storage.options;

import com.amplifyframework.core.async.Options;
import com.amplifyframework.storage.StorageAccessLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Options to specify attributes of put API invocation.
 */
public final class StorageUploadFileOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final String contentType;
    private final Map<String, String> metadata;

    StorageUploadFileOptions(Builder builder) {
        this.accessLevel = builder.getAccessLevel();
        this.targetIdentityId = builder.getTargetIdentityId();
        this.contentType = builder.getContentType();
        this.metadata = builder.getMetadata();
    }

    /**
     * Gets the storage access level.
     * @return Storage access level
     */
    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Target user to apply the action on.
     * @return Target user's identity id
     */
    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * The standard MIME type describing the format of the object to store.
     * @return Content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Metadata for the object to store.
     * @return metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Returns a new Builder instance that can be used to configure
     * and build a new immutable instance of StorageUploadFileOptions.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new default instance of the StorageUploadFileOptions.
     * @return default storage put options
     */
    public static StorageUploadFileOptions defaultInstance() {
        return builder().build();
    }

    /**
     * Use to configure and build immutable instances of the
     * StorageUploadFileOptions, using fluent of property configuration
     * methods.
     */
    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;
        private String contentType;
        private Map<String, String> metadata;

        Builder() {
            Builder.this.metadata = new HashMap<>();
        }

        /**
         * Configures the storage access level for the new
         * StorageUploadFileOptions instance.
         * @param accessLevel Storage access level
         * @return Current Builder instance for fluent chaining
         */
        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        /**
         * Configures the target identity id for a new StorageUploadFileOptions instance.
         * @param targetIdentityId Target user's identity id
         * @return Current Builder instance for fluent chaining
         */
        public Builder targetIdentityId(String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        /**
         * Configures the content type for a new StorageUploadFileOptions instance.
         * @param contentType Content type
         * @return Current Builder instance for fluent chaining
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Configures metadata for new StorageUploadFileOptions instance.
         * @param metadata Metadata for StorageUploadFileOptions
         * @return Current Builder instance for fluent method chaining
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata.clear();
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        /**
         * Builds a new immutable StorageUploadFileOptions instance,
         * based on the configuration options that have been previously
         * set on this Builder instance.
         * @return A new immutable StorageUploadFileOptions instance
         */
        public StorageUploadFileOptions build() {
            return new StorageUploadFileOptions(this);
        }

        StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        String getTargetIdentityId() {
            return targetIdentityId;
        }

        String getContentType() {
            return contentType;
        }

        Map<String, String> getMetadata() {
            return Collections.unmodifiableMap(metadata);
        }
    }
}

