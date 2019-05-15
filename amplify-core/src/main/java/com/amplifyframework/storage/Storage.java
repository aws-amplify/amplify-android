package com.amplifyframework.storage;

import android.support.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.provider.Category;

public class Storage {
    public static StorageOperation put(@NonNull String remotePath, @NonNull String localPath) throws StorageException {
        StorageProvider storageProvider = (StorageProvider) Amplify.getDefaultProviderOfCategory(Category.STORAGE);
        if (storageProvider != null) {
            return storageProvider.put(remotePath, localPath);
        } else {
            throw new StorageException("No valid StorageProvider found. " +
                    "You need to configure a StorageProvider by calling " +
                    "Amplify.configure() or Amplify.addProvider(StorageProvider).");
        }
    }

    public static StorageOperation get(@NonNull String remotePath, @NonNull String localPath) throws StorageException {
        StorageProvider storageProvider = (StorageProvider) Amplify.getDefaultProviderOfCategory(Category.STORAGE);
        if (storageProvider != null) {
            return storageProvider.get(remotePath, localPath);
        } else {
            throw new StorageException("No valid StorageProvider found. " +
                    "You need to configure a StorageProvider by calling " +
                    "Amplify.configure() or Amplify.addProvider(StorageProvider).");
        }
    }

    public static StorageOperation list(@NonNull String remotePath) throws Exception {
        StorageProvider storageProvider = (StorageProvider) Amplify.getDefaultProviderOfCategory(Category.STORAGE);
        if (storageProvider != null) {
            return storageProvider.list(remotePath);
        } else {
            throw new StorageException("No valid StorageProvider found. " +
                    "You need to configure a StorageProvider by calling " +
                    "Amplify.configure() or Amplify.addProvider(StorageProvider).");
        }
    }

    public static StorageOperation remove(String remotePath) throws StorageException {
        StorageProvider storageProvider = (StorageProvider) Amplify.getDefaultProviderOfCategory(Category.STORAGE);
        if (storageProvider != null) {
            return storageProvider.remove(remotePath);
        } else {
            throw new StorageException("No valid StorageProvider found. " +
                    "You need to configure a StorageProvider by calling " +
                    "Amplify.configure() or Amplify.addProvider(StorageProvider).");
        }
    }
}
