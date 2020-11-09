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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.logging.Logger;

/**
 * Interface to enable custom error handling when encountered with an error from the
 * GraphQL server. Will be invoked while publishing a mutation for any GraphQL error
 * that is not of ConflictUnhandled type, which will be handled by {@link DataStoreConflictHandler}.
 */
public interface DataStoreErrorHandler extends Consumer<DataStoreError<? extends Model>> {
    /**
     * Gets a new instance of the {@link LoggingErrorHandler}.
     * @return A new {@link LoggingErrorHandler}
     */
    @NonNull
    static DataStoreErrorHandler defaultHandler() {
        return new LoggingErrorHandler();
    }

    /**
     * Default instance for handling error response from the GraphQL server. Error message
     * is logged, but no further action is taken.
     */
    final class LoggingErrorHandler implements DataStoreErrorHandler {
        private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

        private LoggingErrorHandler() {}

        @Override
        public void accept(@NonNull DataStoreError<? extends Model> error) {
            LOG.error("Error encountered in the DataStore: " + error.getError().getMessage());
        }
    }
}
