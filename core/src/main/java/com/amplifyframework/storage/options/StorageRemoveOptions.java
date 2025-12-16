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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.Consumer;

/**
 * Options to specify attributes of remove API invocation.
 */
public class StorageRemoveOptions extends StorageOptions {
    
    private final boolean recursive;
    private final int batchSize;
    private final BatchStrategy batchStrategy;
    private final long delayBetweenBatchesMs;
    private final int maxConcurrency;
    private final ErrorHandling errorHandling;
    private final Consumer<ProgressInfo> onProgress;

    /**
     * Strategy for processing batches during folder deletion.
     */
    public enum BatchStrategy {
        SEQUENTIAL,
        PARALLEL
    }

    /**
     * Error handling strategy for batch operations.
     */
    public enum ErrorHandling {
        FAIL_EARLY,
        CONTINUE
    }

    /**
     * Progress information for folder deletion operations.
     */
    public static class ProgressInfo {
        private final int totalFiles;
        private final int deletedCount;
        private final int failedCount;
        private final int currentBatch;
        private final int totalBatches;

        public ProgressInfo(int totalFiles, int deletedCount, int failedCount, int currentBatch, int totalBatches) {
            this.totalFiles = totalFiles;
            this.deletedCount = deletedCount;
            this.failedCount = failedCount;
            this.currentBatch = currentBatch;
            this.totalBatches = totalBatches;
        }

        public int getTotalFiles() { return totalFiles; }
        public int getDeletedCount() { return deletedCount; }
        public int getFailedCount() { return failedCount; }
        public int getCurrentBatch() { return currentBatch; }
        public int getTotalBatches() { return totalBatches; }
    }

    /**
     * Constructs a StorageRemoveOptions instance with the
     * attributes from builder instance.
     * @param builder the builder with configured attributes
     */
    @SuppressWarnings("deprecation")
    protected StorageRemoveOptions(final Builder<?> builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId(), builder.getBucket());
        this.recursive = builder.recursive;
        this.batchSize = builder.batchSize;
        this.batchStrategy = builder.batchStrategy;
        this.delayBetweenBatchesMs = builder.delayBetweenBatchesMs;
        this.maxConcurrency = builder.maxConcurrency;
        this.errorHandling = builder.errorHandling;
        this.onProgress = builder.onProgress;
    }

    /**
     * Factory method to create a new instance of the
     * {@link StorageRemoveOptions.Builder}.  The builder can be
     * used to configure properties and then construct a new immutable
     * instance of the StorageRemoveOptions.
     * @return An instance of the {@link StorageRemoveOptions.Builder}
     */
    @NonNull
    public static Builder<?> builder() {
        return new Builder<>();
    }

    /**
     * Factory method to create builder which is configured to prepare
     * object instances with the same field values as the provided
     * options. This can be used as a starting ground to create a
     * new clone of the provided options, which shares some common
     * configuration.
     * @param options Options to populate into a new builder configuration
     * @return A Builder instance that has been configured using the
     *         values in the provided options
     */
    @NonNull
    @SuppressWarnings("deprecation")
    public static Builder<?> from(@NonNull final StorageRemoveOptions options) {
        return builder()
                .accessLevel(options.getAccessLevel())
                .targetIdentityId(options.getTargetIdentityId())
                .bucket(options.getBucket())
                .recursive(options.isRecursive())
                .batchSize(options.getBatchSize())
                .batchStrategy(options.getBatchStrategy())
                .delayBetweenBatchesMs(options.getDelayBetweenBatchesMs())
                .maxConcurrency(options.getMaxConcurrency())
                .errorHandling(options.getErrorHandling())
                .onProgress(options.getOnProgress());
    }

    /**
     * Gets whether recursive folder deletion is enabled.
     * @return true if recursive deletion is enabled, false otherwise
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Gets the batch size for folder deletion operations.
     * @return the batch size (default: 1000)
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Gets the batch processing strategy.
     * @return the batch strategy (SEQUENTIAL or PARALLEL)
     */
    @NonNull
    public BatchStrategy getBatchStrategy() {
        return batchStrategy;
    }

    /**
     * Gets the delay between batch operations in milliseconds.
     * @return the delay in milliseconds
     */
    public long getDelayBetweenBatchesMs() {
        return delayBetweenBatchesMs;
    }

    /**
     * Gets the maximum number of concurrent batch operations.
     * @return the maximum concurrency
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    /**
     * Gets the error handling strategy.
     * @return the error handling strategy
     */
    @NonNull
    public ErrorHandling getErrorHandling() {
        return errorHandling;
    }

    /**
     * Gets the progress callback.
     * @return the progress callback, or null if not set
     */
    @Nullable
    public Consumer<ProgressInfo> getOnProgress() {
        return onProgress;
    }

    /**
     * Constructs a default instance of the {@link StorageRemoveOptions}.
     * @return default instance of StorageRemoveOptions
     */
    @NonNull
    public static StorageRemoveOptions defaultInstance() {
        return builder().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof StorageRemoveOptions)) {
            return false;
        } else {
            StorageRemoveOptions that = (StorageRemoveOptions) obj;
            return ObjectsCompat.equals(getAccessLevel(), that.getAccessLevel()) &&
                    ObjectsCompat.equals(getTargetIdentityId(), that.getTargetIdentityId()) &&
                    ObjectsCompat.equals(getBucket(), that.getBucket()) &&
                    recursive == that.recursive &&
                    batchSize == that.batchSize &&
                    ObjectsCompat.equals(batchStrategy, that.batchStrategy) &&
                    delayBetweenBatchesMs == that.delayBetweenBatchesMs &&
                    maxConcurrency == that.maxConcurrency &&
                    ObjectsCompat.equals(errorHandling, that.errorHandling);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public int hashCode() {
        return ObjectsCompat.hash(
                getAccessLevel(),
                getTargetIdentityId(),
                getBucket(),
                recursive,
                batchSize,
                batchStrategy,
                delayBetweenBatchesMs,
                maxConcurrency,
                errorHandling
        );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public String toString() {
        return "StorageRemoveOptions {" +
                "accessLevel=" + getAccessLevel() +
                ", targetIdentityId=" + getTargetIdentityId() +
                ", bucket=" + getBucket() +
                ", recursive=" + recursive +
                ", batchSize=" + batchSize +
                ", batchStrategy=" + batchStrategy +
                ", delayBetweenBatchesMs=" + delayBetweenBatchesMs +
                ", maxConcurrency=" + maxConcurrency +
                ", errorHandling=" + errorHandling +
                '}';
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link StorageRemoveOptions}, by chaining
     * fluent configuration method calls.
     * @param <B> the type of builder to chain with
     */
    public static class Builder<B extends Builder<B>> extends StorageOptions.Builder<B, StorageRemoveOptions> {
        
        private boolean recursive = false;
        private int batchSize = 1000;
        private BatchStrategy batchStrategy = BatchStrategy.SEQUENTIAL;
        private long delayBetweenBatchesMs = 0;
        private int maxConcurrency = 3;
        private ErrorHandling errorHandling = ErrorHandling.FAIL_EARLY;
        private Consumer<ProgressInfo> onProgress = null;

        /**
         * Configures recursive folder deletion.
         * @param recursive true to enable recursive deletion, false otherwise
         * @return the builder instance for method chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public B recursive(boolean recursive) {
            this.recursive = recursive;
            return (B) this;
        }

        /**
         * Configures the batch size for folder deletion operations.
         * @param batchSize number of files to process per batch (max 1000)
         * @return the builder instance for method chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public B batchSize(int batchSize) {
            this.batchSize = Math.min(batchSize, 1000);
            return (B) this;
        }

        /**
         * Configures the batch processing strategy.
         * @param batchStrategy the strategy (SEQUENTIAL or PARALLEL)
         * @return the builder instance for method chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public B batchStrategy(@NonNull BatchStrategy batchStrategy) {
            this.batchStrategy = batchStrategy;
            return (B) this;
        }

        /**
         * Configures the delay between batch operations.
         * @param delayBetweenBatchesMs delay in milliseconds
         * @return the builder instance for method chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public B delayBetweenBatchesMs(long delayBetweenBatchesMs) {
            this.delayBetweenBatchesMs = delayBetweenBatchesMs;
            return (B) this;
        }

        /**
         * Configures the maximum number of concurrent batch operations.
         * @param maxConcurrency maximum concurrency level
         * @return the builder instance for method chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public B maxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return (B) this;
        }

        /**
         * Configures the error handling strategy.
         * @param errorHandling the error handling strategy
         * @return the builder instance for method chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public B errorHandling(@NonNull ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
            return (B) this;
        }

        /**
         * Configures the progress callback.
         * @param onProgress callback for progress updates
         * @return the builder instance for method chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public B onProgress(@Nullable Consumer<ProgressInfo> onProgress) {
            this.onProgress = onProgress;
            return (B) this;
        }

        /**
         * Returns an instance of StorageRemoveOptions with the parameters
         * specified by this builder.
         * @return a configured instance of StorageRemoveOptions
         */
        @SuppressLint("SyntheticAccessor")
        @Override
        @NonNull
        public StorageRemoveOptions build() {
            return new StorageRemoveOptions(this);
        }
    }
}
