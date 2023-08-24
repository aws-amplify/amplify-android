/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.content.Context;
import android.util.Log;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.api.aws.AuthModeStrategyType;
import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.auth.cognito.helpers.JWTParser;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;
import com.amplifyframework.datastore.storage.sqlite.TestStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testmodels.commentsblog.Author;
import com.amplifyframework.testmodels.multiauth.MultiAuthTestModelProvider;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Tests a set of possible combinations of models, auth modes and login status to
 * verify behavior when in multi-auth mode.
 */
public final class MultiAuthSyncEngineNoAuthInstrumentationTest {
    private static final Logger LOG = Amplify.Logging.logger(
        CategoryType.DATASTORE,
        "MultiAuthSyncEngineInstrumentationTest"
    );
    private static final int TIMEOUT_SECONDS = 20;
    private static final String GOOGLE_ISS_CLAIM = "https://accounts.google.com";

    private SynchronousDataStore dataStore;
    private HttpRequestInterceptor requestInterceptor;

    /**
     * Class name: Author.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    @Ignore("fix in dev-preview")
    public void testAuthorAnonymous() throws IOException, AmplifyException {
        verifyScenario();
    }

    /**
     * Method used to configure each scenario.
     * @throws AmplifyException No expected.
     */
    private void configure()
        throws AmplifyException {
        Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));
        String tag = Author.class.getSimpleName();

        MultiAuthTestModelProvider modelProvider =
            MultiAuthTestModelProvider.getInstance(Collections.singletonList(Author.class));
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();

        ModelSchema modelSchema = ModelSchema.fromModelClass(Author.class);
        schemaRegistry.register(Author.class.getSimpleName(), modelSchema);

        StrictMode.enable();
        Context context = getApplicationContext();
        @RawRes int configResourceId = Resources.getRawResourceId(context, "amplifyconfigurationupdated");
        AmplifyConfiguration amplifyConfiguration = AmplifyConfiguration.fromConfigFile(context, configResourceId);

        CategoryConfiguration apiCategoryConfiguration = amplifyConfiguration.forCategoryType(CategoryType.API);

        ApiCategory apiCategory = new ApiCategory();
        requestInterceptor = new HttpRequestInterceptor(AuthorizationType.API_KEY);
        apiCategory.addPlugin(AWSApiPlugin.builder()
                                          .configureClient("DataStoreIntegTestsApi", okHttpClientBuilder ->
                                              okHttpClientBuilder.addInterceptor(requestInterceptor)
                                          )
                                          .build());
        apiCategory.configure(apiCategoryConfiguration, context);

        // Setup DataStore
        DataStoreConfiguration dsConfig = DataStoreConfiguration.builder()
                                                .errorHandler(exception -> Log.e(tag,
                                                    "DataStore error handler received an error.",
                                                    exception))
                                                .syncExpression(modelSchema.getName(),
                                                    () -> {
                                                        try {
                                                            return Where.identifier(modelSchema.getModelClass(),
                                                                    "FAKE_ID").getQueryPredicate();
                                                        } catch (AmplifyException exception) {
                                                            fail();
                                                        }
                                                        return null;
                                                    })
                                                .build();
        CategoryConfiguration dataStoreCategoryConfiguration =
            AmplifyConfiguration.fromConfigFile(context, configResourceId)
                                .forCategoryType(CategoryType.DATASTORE);

        String databaseName = "IntegTest" + Author.class.getSimpleName() + ".db";
        SQLiteStorageAdapter sqLiteStorageAdapter = TestStorageAdapter
            .create(schemaRegistry, modelProvider, databaseName);
        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                                                                  .storageAdapter(sqLiteStorageAdapter)
                                                                  .modelProvider(modelProvider)
                                                                  .apiCategory(apiCategory)
                                                                  .authModeStrategy(AuthModeStrategyType.MULTIAUTH)
                                                                  .schemaRegistry(schemaRegistry)
                                                                  .dataStoreConfiguration(dsConfig)
                                                                  .build();
        DataStoreCategory dataStoreCategory = new DataStoreCategory();
        dataStoreCategory.addPlugin(awsDataStorePlugin);
        dataStoreCategory.configure(dataStoreCategoryConfiguration, context);
        dataStoreCategory.initialize(context);
        dataStore = SynchronousDataStore.delegatingTo(dataStoreCategory);
    }

    /**
     * Test tear-down activities.
     */
    @AfterClass
    public static void cleanupAfterAllTests() {
        Log.i("TearDown", "Cleaning up databases");
        for (String db : getApplicationContext().databaseList()) {
            Log.i("TearDown", "Removing " + db);
            getApplicationContext().deleteDatabase(db);
            Log.i("TearDown", db + " removed");
        }
        Log.i("TearDown", "Cleanup completed.");
    }

    private void verifyScenario() throws AmplifyException {
        configure();
        String modelId = UUID.randomUUID().toString();
        Model testRecord = createRecord(modelId);
        HubAccumulator expectedEventAccumulator = HubAccumulator
                    .create(HubChannel.DATASTORE, publicationOf(Author.class.getSimpleName(),
                            testRecord.getPrimaryKeyString()), 1)
                    .start();
        dataStore.start();
        dataStore.save(testRecord);
        expectedEventAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(requestInterceptor.hasUnexpectedRequests());
    }

    /**
     * Create a instance of the model using the private constructor via reflection.
     * @return A instance of the model being tested.
     */
    private Model createRecord(String modelId) {
        try {
            String recordDetail = "IntegTest-" + Author.class.getSimpleName() + " " + RandomString.string();
            Map<String, Object> modelMap = new HashMap<>();
            modelMap.put("id", modelId);
            modelMap.put("name", recordDetail);
            return SerializedModel.builder()
                    .modelSchema(ModelSchema.fromModelClass(Author.class))
                    .serializedData(modelMap)
                    .build();
        } catch (AmplifyException exception) {
            Log.e(Author.class.getSimpleName(), "Unable to create an instance of model " +
                            Author.class.getSimpleName(),
                  exception);
            throw new RuntimeException("Unable to create an instance of model " +
                    Author.class.getSimpleName(), exception);
        }
    }

    private static AuthorizationType getRequestAuthType(Headers headers) {
        String authHeaderValue = headers.get("Authorization");
        String apiKeyHeaderValue = headers.get("x-api-key");
        if (authHeaderValue != null && apiKeyHeaderValue != null) {
            throw new IllegalStateException("Request contains Authorization and API key headers.");
        }
        if (apiKeyHeaderValue != null) {
            return AuthorizationType.API_KEY;
        }
        if (authHeaderValue == null) {
            return AuthorizationType.NONE;
        }
        if (authHeaderValue.startsWith("AWS4-HMAC-SHA256")) {
            return AuthorizationType.AWS_IAM;
        }
        String iss = JWTParser.INSTANCE.getClaim(authHeaderValue, "iss");
        if (iss == null) {
            throw new IllegalStateException("Could not find any valid auth headers");
        }
        if (GOOGLE_ISS_CLAIM.equals(iss)) {
            return AuthorizationType.OPENID_CONNECT;
        }
        if (iss.contains("cognito")) {
            return AuthorizationType.AMAZON_COGNITO_USER_POOLS;
        }
        throw new IllegalStateException("Unable to determine the authorization type of the request.");
    }

    private static final class HttpRequestInterceptor implements Interceptor {
        private final Map<Request, Response> unexpectedRequests;
        private final AuthorizationType expectedAuthType;

        HttpRequestInterceptor(AuthorizationType expectedAuthType) {
            this.unexpectedRequests = new HashMap<>();
            this.expectedAuthType = expectedAuthType;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            Request httpRequest = originalResponse.request().newBuilder().build();
            final Buffer buffer = new Buffer();
            RequestBody requestBody = httpRequest.body();
            if (requestBody != null) {
                requestBody.writeTo(buffer);
            } else {
                buffer.write("".getBytes());
            }

            Request copyOfRequest = httpRequest.newBuilder().build();
            Response copyOfResponse = originalResponse.newBuilder().build();
            ResponseBody responseBody = copyOfResponse.newBuilder().build().body();
            String responseBodyString = responseBody != null ? responseBody.string() : "";
            AuthorizationType requestAuthType = getRequestAuthType(copyOfRequest.headers());
            if (isUnexpectedRequest(responseBodyString, copyOfResponse.code(), requestAuthType)) {
                unexpectedRequests.put(copyOfRequest, copyOfResponse);
            }

            return originalResponse.newBuilder()
                                   .body(ResponseBody.create(responseBodyString, originalResponse.body().contentType()))
                                   .build();
        }

        public boolean hasUnexpectedRequests() {
            return unexpectedRequests.size() > 0;
        }

        private boolean isUnexpectedRequest(String responseBodyString,
                                            int responseCode,
                                            AuthorizationType requestAuthType) {
            try {
                JSONObject responseJson = new JSONObject(responseBodyString);
                JSONArray errors = responseJson.has("errors") ? responseJson.getJSONArray("errors") : new JSONArray();
                if (responseCode > 399 || errors.length() > 0) {
                    // Request failed. Make sure it's not for the expected auth type.
                    if (expectedAuthType != null && expectedAuthType.equals(requestAuthType)) {
                        return true;
                    }
                } else {
                    // Request was successful. Make sure it's the expected auth type.
                    if (expectedAuthType != null && !expectedAuthType.equals(requestAuthType)) {
                        return true;
                    }
                }
            } catch (JSONException | IllegalStateException exception) {
                LOG.warn("Unable to validate authorization type for request.", exception);
                return true;
            }
            return false;
        }
    }
}
