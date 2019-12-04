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

package com.amplifyframework.datastore.network;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;

/**
 * System model to represent the versioning/sync metadata for an object.
 */
@ModelConfig
@SuppressWarnings({"MemberName", "ParameterName"})
public final class ModelMetadata implements Model {
    private final @ModelField(targetType = "ID", isRequired = true) String id;
    private final @ModelField(targetType = "Boolean", isRequired = true) Boolean _deleted;
    private final @ModelField(targetType = "Int", isRequired = true) Integer _version;
    private final @ModelField(targetType = "AWSTimestamp", isRequired = true) Long _lastChangedAt;

    /**
     * Constructor for this metadata model.
     * @param id The ID of the object this is holding the metadata for (also this object's own ID)
     * @param deleted Whether this object was deleted since the last sync time specified
     * @param version What version this object was last seen at
     * @param lastChangedAt When was this object last changed
     */
    public ModelMetadata(String id, Boolean deleted, Integer version, Long lastChangedAt) {
        this.id = id;
        this._deleted = deleted;
        this._version = version;
        this._lastChangedAt = lastChangedAt;
    }

    /**
     * Gets ID.
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the deleted status.
     * @return True if deleted, False otherwise
     */
    public Boolean isDeleted() {
        return _deleted;
    }

    /**
     * Gets the version.
     * @return version
     */
    public Integer getVersion() {
        return _version;
    }

    /**
     * Gets last changed at time.
     * @return last changed at time
     */
    public Long getLastChangedAt() {
        return _lastChangedAt;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ModelMetadata metadata = (ModelMetadata) thatObject;

        if (!ObjectsCompat.equals(id, metadata.id)) {
            return false;
        }
        if (!ObjectsCompat.equals(_deleted, metadata._deleted)) {
            return false;
        }
        if (!ObjectsCompat.equals(_version, metadata._version)) {
            return false;
        }
        return ObjectsCompat.equals(_lastChangedAt, metadata._lastChangedAt);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (_deleted != null ? _deleted.hashCode() : 0);
        result = 31 * result + (_version != null ? _version.hashCode() : 0);
        result = 31 * result + (_lastChangedAt != null ? _lastChangedAt.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelMetadata{" +
            "id='" + id + '\'' +
            ", _deleted=" + _deleted +
            ", _version=" + _version +
            ", _lastChangedAt=" + _lastChangedAt +
            '}';
    }
}
