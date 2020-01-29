package com.amplifyframework.storage;

import android.content.Context;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
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
    public void downloadExistingFile() throws StorageException {
        // TODO: what is a local path on Android? What's the protocol:// ?
        // assets:// or file:// ?
        String fromRemoteKey = RandomString.string();
        String toLocalPath = RandomString.string();

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
            // return what?
        }).when(transferObserver.setTransferListener(any(TransferListener.class)));

        StorageDownloadFileResult result =
            Await.<StorageDownloadFileResult, StorageException>result((onResult, onError) -> {
                storage.downloadFile(
                    fromRemoteKey,
                    toLocalPath,
                    onResult,
                    onError
                );
            });

        assertEquals("expected file name", result.getFile());
    }
}
