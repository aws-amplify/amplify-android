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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

/**
 * Just a ~type-alias for a consumer of DataStoreException.
 */
public interface DataStoreErrorHandler extends Consumer<DataStoreErrorHandler.SyncError<? extends Model>> {
    /**
     * Gets a new instance of the {@link DefaultDataStoreErrorHandler}.
     * @return A new {@link DefaultDataStoreErrorHandler}
     */
    @NonNull
    static DataStoreErrorHandler defaultHandler() {
        return new DefaultDataStoreErrorHandler();
    }

    final class DefaultDataStoreErrorHandler implements DataStoreErrorHandler {
        private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

        private DefaultDataStoreErrorHandler() {}

        @Override
        public void accept(@NonNull SyncError<? extends Model> error) {
            LOG.error("Error encountered in the DataStore:" + error.getError().getMessage());
        }
    }

    final class SyncError<M extends Model> {
        private final GraphQLResponse.Error error;
        private final M local;
        private final M remote;

        public SyncError(
                @NonNull GraphQLResponse.Error error,
                @Nullable M local,
                @Nullable M remote
        ) {
            this.error = Objects.requireNonNull(error);
            this.local = local;
            this.remote = remote;
        }

        @NonNull
        public GraphQLResponse.Error getError() {
            return error;
        }

        @Nullable
        public M getLocal() {
            return local;
        }

        @Nullable
        public M getRemote() {
            return remote;
        }
    }
}
