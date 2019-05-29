package com.amplifyframework.storage;

import android.support.annotation.NonNull;

import com.amplifyframework.auth.AuthPlugin;
import com.amplifyframework.core.plugin.Plugin;

/**
 * Plugin API for Storage Category. Any Storage Plugin
 * would implement this interface.
 */
public interface StoragePlugin extends Plugin {

    AuthPlugin authProvider = null;

    StorageOperation put(@NonNull String remotePath, @NonNull String localPath);

    StorageOperation get(@NonNull String remotePath, @NonNull String localPath);

    StorageOperation list(@NonNull String remotePath);

    StorageOperation remove(@NonNull String remotePath);
}
