package com.amplifyframework.storage;

import android.content.Context;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.amplifyframework.storage.s3.IdentityIdProvider;
import com.amplifyframework.storage.s3.service.StorageService;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StorageComponentTest {

    private StorageCategory storage;
    private StorageService storageService;

    @Before
    public void setup() throws AmplifyException {
        this.storage = new StorageCategory();
        this.storageService = mock(StorageService.class);
        StorageService.Factory storageServiceFactory = (context, region, bucket) -> storageService;
        IdentityIdProvider identityIdProvider = RandomString::string;
        this.storage.addPlugin(new AWSS3StoragePlugin(storageServiceFactory, identityIdProvider));
        this.storage.configure(buildConfiguration(), mock(Context.class));
    }

    private static StorageCategoryConfiguration buildConfiguration() {
        StorageCategoryConfiguration configuration = new StorageCategoryConfiguration();
        JSONObject storageJson;
        try {
            storageJson = new JSONObject()
                .put("plugins", new JSONObject()
                    .put("awsS3StoragePlugin", new JSONObject()
                        .put("region", "us-east-1")
                        .put("bucket", "hamburger-bucket")));
            configuration.populateFromJSON(storageJson);
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
        return configuration;
    }

    @Test
    public void testDownloadToFileGetsFile() throws StorageException {
        final String fromRemoteKey = RandomString.string();
        final String toLocalPath = RandomString.string();

        // Since we use a mock StorageService, it will return a null
        // result by default. We need a non-null transfer observer.
        // One option is to mock that, too.
        TransferObserver transferObserver = mock(TransferObserver.class);
        when(storageService.downloadToFile(anyString(), any(File.class)))
                .thenReturn(transferObserver);

        // Since we use a mock TransferObserver, it has no internal logic
        // to know to call back the listener! So, we simulate the success
        // callback, as part of our "happy path" test.
        doAnswer(invocation -> {
            TransferListener listener = invocation.getArgument(0);
            listener.onStateChanged(0, TransferState.COMPLETED);
            return null;
        })
                .when(transferObserver)
                .setTransferListener(any(TransferListener.class));

        StorageDownloadFileResult result =
                Await.<StorageDownloadFileResult, StorageException>result((onResult, onError) -> {
                    storage.downloadFile(
                            fromRemoteKey,
                            toLocalPath,
                            onResult,
                            onError
                    );
                });

        assertEquals(toLocalPath, result.getFile().toString());
    }

    @Test
    public void testUploadFileGetsKey() throws StorageException {
        final String toRemoteKey = RandomString.string();
        final String fromLocalPath = RandomString.string();

        // Since we use a mock StorageService, it will return a null
        // result by default. We need a non-null transfer observer.
        // One option is to mock that, too.
        TransferObserver transferObserver = mock(TransferObserver.class);
        when(storageService.uploadFile(anyString(), any(File.class)))
                .thenReturn(transferObserver);

        // Since we use a mock TransferObserver, it has no internal logic
        // to know to call back the listener! So, we simulate the success
        // callback, as part of our "happy path" test.
        doAnswer(invocation -> {
            TransferListener listener = invocation.getArgument(0);
            listener.onStateChanged(0, TransferState.COMPLETED);
            return null;
        })
                .when(transferObserver)
                .setTransferListener(any(TransferListener.class));

        StorageUploadFileResult result =
                Await.<StorageUploadFileResult, StorageException>result((onResult, onError) -> {
                    storage.uploadFile(
                            toRemoteKey,
                            fromLocalPath,
                            onResult,
                            onError
                    );
                });

        assertEquals(toRemoteKey, result.getKey());
    }

    @Test
    public void testListObject() throws StorageException {
        final String path = RandomString.string();
        final StorageListResult.Item item = new StorageListResult.Item(
                RandomString.string(),
                0L,
                new Date(),
                RandomString.string(),
                null
        );

        when(storageService.listFiles(anyString()))
                .thenReturn(StorageListResult.fromItems(Collections.singletonList(item)));

        StorageListResult result =
                Await.<StorageListResult, StorageException>result((onResult, onError) -> {
                    storage.list(
                            path,
                            onResult,
                            onError
                    );
                });

        assertEquals(item, result.getItems().get(0));
    }

    @Test
    public void testRemoveObjectGetsKey() throws StorageException {
        final String remoteKey = RandomString.string();

        StorageRemoveResult result =
                Await.<StorageRemoveResult, StorageException>result((onResult, onError) -> {
                    storage.remove(
                            remoteKey,
                            onResult,
                            onError
                    );
                });

        assertEquals(remoteKey, result.getKey());
    }
}
