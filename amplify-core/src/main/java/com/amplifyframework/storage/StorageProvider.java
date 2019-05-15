package com.amplifyframework.storage;

import android.support.annotation.NonNull;

import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.core.provider.Provider;

/**
 * Provider API for Storage Category. Any Storage Provider
 * would implement this interface.
 */
public interface StorageProvider extends Provider {

    AuthProvider authProvider = null;

    StorageOperation put(@NonNull String remotePath, @NonNull String localPath);

    StorageOperation get(@NonNull String remotePath, @NonNull String localPath);

    StorageOperation list(@NonNull String remotePath);

    StorageOperation remove(@NonNull String remotePath);
}
