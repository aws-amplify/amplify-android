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

import android.content.Context;
import android.util.Log;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.api.aws.ApiAuthProviders;
import com.amplifyframework.api.aws.AuthModeStrategyType;
import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolsAuthProvider;
import com.amplifyframework.auth.AuthCategory;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.testmodels.multiauth.GroupPrivatePublicUPIAMAPIPost;
import com.amplifyframework.testmodels.multiauth.GroupPrivateUPIAMPost;
import com.amplifyframework.testmodels.multiauth.GroupPublicUPAPIPost;
import com.amplifyframework.testmodels.multiauth.GroupPublicUPIAMPost;
import com.amplifyframework.testmodels.multiauth.GroupUPPost;
import com.amplifyframework.testmodels.multiauth.MultiAuthTestModelProvider;
import com.amplifyframework.testmodels.multiauth.OwnerOIDCPost;
import com.amplifyframework.testmodels.multiauth.OwnerPrivatePublicUPIAMAPIPost;
import com.amplifyframework.testmodels.multiauth.OwnerPrivateUPIAMPost;
import com.amplifyframework.testmodels.multiauth.OwnerPublicOIDAPIPost;
import com.amplifyframework.testmodels.multiauth.OwnerPublicUPAPIPost;
import com.amplifyframework.testmodels.multiauth.OwnerUPPost;
import com.amplifyframework.testmodels.multiauth.PrivatePrivatePublicUPIAMAPIPost;
import com.amplifyframework.testmodels.multiauth.PrivatePrivatePublicUPIAMIAMPost;
import com.amplifyframework.testmodels.multiauth.PrivatePrivateUPIAMPost;
import com.amplifyframework.testmodels.multiauth.PrivatePublicComboAPIPost;
import com.amplifyframework.testmodels.multiauth.PrivatePublicComboUPPost;
import com.amplifyframework.testmodels.multiauth.PrivatePublicPublicUPAPIIAMPost;
import com.amplifyframework.testmodels.multiauth.PrivatePublicUPAPIPost;
import com.amplifyframework.testmodels.multiauth.PrivatePublicUPIAMPost;
import com.amplifyframework.testmodels.multiauth.PrivateUPPost;
import com.amplifyframework.testmodels.multiauth.PublicAPIPost;
import com.amplifyframework.testmodels.multiauth.PublicIAMPost;
import com.amplifyframework.testmodels.multiauth.PublicPublicIAMAPIPost;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousAuth;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.core.Resources.readJsonResourceFromId;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static org.junit.Assert.fail;

/**
 * Tests a set of possible combinations of models, auth modes and login status to
 * verify behavior when in multi-auth mode.
 */
@RunWith(Parameterized.class)
public class MultiAuthInstrumentationTest {
    private static final int TIMEOUT_SECONDS = 10;

    private final Class<? extends Model> modelType;
    private final boolean requiresSignIn;
    private final AuthorizationType expectedAuthType;
    private final String tag;
    private final String modelId;
    private SynchronousApi api;
    private SynchronousDataStore dataStore;
    private SynchronousAuth auth;

    private String cognitoUser;
    private String cognitoPassword;

    /**
     * Constructor for the parameterized test.
     * @param clazz The model type.
     * @param requiresSignIn Does the test scenario require the user to be logged in.
     * @param expectedAuthType The auth type that should succeed for the test.
     * @throws AmplifyException No expected.
     */
    public MultiAuthInstrumentationTest(Class<? extends Model> clazz,
                                        boolean requiresSignIn,
                                        AuthorizationType expectedAuthType) throws AmplifyException {
        Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));
        this.tag = clazz.getSimpleName();
        this.modelType = clazz;
        this.requiresSignIn = requiresSignIn;
        this.expectedAuthType = expectedAuthType;
        this.modelId = UUID.randomUUID().toString();
        logTestInfo("Configuring");

        MultiAuthTestModelProvider modelProvider =
            MultiAuthTestModelProvider.getInstance(Collections.singletonList(modelType));
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();

        modelSchemaRegistry.register(modelType.getSimpleName(), ModelSchema.fromModelClass(modelType));

        StrictMode.enable();
        Context context = getApplicationContext();
        //TODO: CHANGE THIS TO AMPLIFYCONFIGURATION
        @RawRes int configResourceId = Resources.getRawResourceId(context, "amplifyconfiguration_dsmydevaccount");
        AmplifyConfiguration amplifyConfiguration = AmplifyConfiguration.fromConfigFile(context, configResourceId);

        readCredsFromConfig(context);

        // Setup an auth plugin
        CategoryConfiguration authCategoryConfiguration = amplifyConfiguration.forCategoryType(CategoryType.AUTH);
        AuthCategory authCategory = new AuthCategory();
        AWSCognitoAuthPlugin authPlugin = new AWSCognitoAuthPlugin();
        authCategory.addPlugin(authPlugin);
        authCategory.configure(authCategoryConfiguration, context);
        auth = SynchronousAuth.delegatingTo(authCategory);
        if (this.requiresSignIn) {
            Log.v(tag, "Test requires signIn.");
            AuthSignInResult authSignInResult = auth.signIn(cognitoUser, cognitoPassword);
            if (!authSignInResult.isSignInComplete()) {
                fail("Unable to complete initial sign-in");
            }
        }

        // Setup an API
        DefaultCognitoUserPoolsAuthProvider cognitoProvider =
            new DefaultCognitoUserPoolsAuthProvider(authPlugin.getEscapeHatch());
        CategoryConfiguration apiCategoryConfiguration = amplifyConfiguration.forCategoryType(CategoryType.API);
        ApiAuthProviders apiAuthProviders = ApiAuthProviders.builder()
                                                            .cognitoUserPoolsAuthProvider(cognitoProvider)
                                                            .awsCredentialsProvider(authPlugin.getEscapeHatch())
                                                            .build();
        ApiCategory apiCategory = new ApiCategory();
        apiCategory.addPlugin(AWSApiPlugin.builder()
                                          .configureClient("DataStoreIntegTestsApi", okHttpClientBuilder ->
                                              okHttpClientBuilder.addInterceptor(new HttpRequestInterceptor())
                                          )
                                          .apiAuthProviders(apiAuthProviders)
                                          .build());
        apiCategory.configure(apiCategoryConfiguration, context);
        api = SynchronousApi.delegatingTo(apiCategory);

        DataStoreConfiguration dsConfig = DataStoreConfiguration.builder()
                                                                .errorHandler(exception -> {
                                                                    Log.e(tag,
                                                                          "DataStore error handler received an error.",
                                                                          exception);
                                                                })
                                                                .build();
        DataStoreCategory dataStoreCategory = DataStoreCategoryConfigurator.begin()
                                                                           .api(apiCategory)
                                                                           .clearDatabase(true)
                                                                           .context(context)
                                                                           .dataStoreConfiguration(dsConfig)
                                                                           .modelProvider(modelProvider)
                                                                           .modelSchemaRegistry(modelSchemaRegistry)
                                                                           .resourceId(configResourceId)
                                                                           .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                                                           .authModeStrategy(
                                                                               AuthModeStrategyType.MULTIAUTH
                                                                           )
                                                                           .finish();
        dataStore = SynchronousDataStore.delegatingTo(dataStoreCategory);
    }

    /**
     * Builds an array of test scenarios which will be passed to the
     * constructor of this class.
     * @return An array of test scenarios representing the following: modelType, requiresLogin, expected auth type.
     */
    @Parameterized.Parameters(name = "model:{0} requiresSignIn: {1} expectedAuthType: {2}")
    public static Iterable<Object[]> data() {
        //Parameters: model class, isSignedIn, expected successful auth type
        return Arrays.asList(new Object[][]{
            // Single rule cases.
            {OwnerUPPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {OwnerOIDCPost.class, true, AuthorizationType.OPENID_CONNECT},
            {GroupUPPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {PrivateUPPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {PublicIAMPost.class, false, AuthorizationType.AWS_IAM},
            {PublicIAMPost.class, false, AuthorizationType.AWS_IAM},
            {PublicAPIPost.class, false, AuthorizationType.API_KEY},

            /* Test cases of models with 2 rules */
            // Owner + private
            {OwnerPrivateUPIAMPost.class, false, AuthorizationType.API_KEY},
            {OwnerPrivateUPIAMPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},

            // Owner + public
            {OwnerPublicUPAPIPost.class, false, AuthorizationType.API_KEY},
            {OwnerPublicUPAPIPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {OwnerPublicOIDAPIPost.class, true, AuthorizationType.OPENID_CONNECT},

            // Group + private
            {GroupPrivateUPIAMPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {GroupPrivateUPIAMPost.class, false, AuthorizationType.AWS_IAM},

            // Group + public
            {GroupPublicUPAPIPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {GroupPublicUPAPIPost.class, false, AuthorizationType.API_KEY},
            {GroupPublicUPIAMPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {GroupPublicUPIAMPost.class, false, AuthorizationType.AWS_IAM},

            // Private + Private
            {PrivatePrivateUPIAMPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {PrivatePrivateUPIAMPost.class, false, AuthorizationType.AMAZON_COGNITO_USER_POOLS},

            // Private + Public
            {PrivatePublicUPAPIPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {PrivatePublicUPAPIPost.class, false, AuthorizationType.API_KEY},
            {PrivatePublicUPIAMPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {PrivatePublicUPIAMPost.class, false, AuthorizationType.AWS_IAM},
            {PrivatePublicUPAPIPost.class, true, AuthorizationType.AWS_IAM},
            {PrivatePublicUPAPIPost.class, false, AuthorizationType.API_KEY},

            // Public + Public
            {PublicPublicIAMAPIPost.class, false, AuthorizationType.API_KEY},

            /* Test cases of models with 3 or more rules */
            {OwnerPrivatePublicUPIAMAPIPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {OwnerPrivatePublicUPIAMAPIPost.class, false, AuthorizationType.AWS_IAM},

            {GroupPrivatePublicUPIAMAPIPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {GroupPrivatePublicUPIAMAPIPost.class, false, AuthorizationType.AWS_IAM},

            {PrivatePrivatePublicUPIAMIAMPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {PrivatePrivatePublicUPIAMIAMPost.class, false, AuthorizationType.AWS_IAM},

            {PrivatePrivatePublicUPIAMAPIPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {PrivatePrivatePublicUPIAMAPIPost.class, false, AuthorizationType.API_KEY},

            {PrivatePublicPublicUPAPIIAMPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS},
            {PrivatePrivatePublicUPIAMAPIPost.class, false, AuthorizationType.AWS_IAM},

            {PrivatePublicComboAPIPost.class, false, AuthorizationType.API_KEY},
            {PrivatePublicComboUPPost.class, true, AuthorizationType.AMAZON_COGNITO_USER_POOLS}
        });
    }

    /**
     * Test tear-down activities.
     */
    @After
    public void tearDown() {
        try {
            dataStore.stop();
        } catch (DataStoreException | RuntimeException exception) {
            Log.e(tag, "Failed to stop DataStore.", exception);
        }
        Log.i(tag, "Deleting database");
        getApplicationContext().deleteDatabase("AmplifyDatastore.db");
        Log.i(tag, "Teardown completed.");
    }

    /**
     * Runs the test for the parameters set in the constructor.
     * @throws AmplifyException Not expected.
     */
    @Test
    public void verifyScenario() throws AmplifyException {
        logTestInfo("Starting");
        Model record1 = createRecord();
        HubAccumulator publishedMutationsAccumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(modelType.getSimpleName(), record1.getId()), 1)
                          .start();
        dataStore.save(record1);
        publishedMutationsAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Create a instance of the model using the private constructor via reflection.
     * @return A instance of the model being tested.
     */
    private Model createRecord() {
        try {
            Constructor<?> constructor = modelType.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return (Model) constructor.newInstance(modelId,
                                                   "Dummy " + modelType.getSimpleName() + " " + RandomString.string());
        } catch (IllegalAccessException |
            InstantiationException |
            InvocationTargetException exception) {
            Log.e(tag, "Unable to create an instance of model " + modelType.getSimpleName(), exception);
            throw new RuntimeException("Unable to create an instance of model " + modelType.getSimpleName(), exception);
        }

    }

    /**
     * Reads credential information from a file. In CI, this file will be pulled from an S3 bucket.
     * The resource file being used is in the .gitignore file to prevent accidental commit.
     * @param context The application context.
     */
    private void readCredsFromConfig(Context context) {
        //TODO: use secrets manager instead.
        @RawRes int cognitoCredsResourceId = Resources.getRawResourceId(context, "credentials");
        try {
            JSONObject credentialsJson = readJsonResourceFromId(context, cognitoCredsResourceId);
            cognitoUser = credentialsJson.getJSONObject("datastore")
                                         .getJSONObject("userPool")
                                         .getString("user");

            cognitoPassword = credentialsJson.getJSONObject("datastore")
                                             .getJSONObject("userPool")
                                             .getString("password");
        } catch (com.amplifyframework.core.Resources.ResourceLoadingException | JSONException exception) {
            Log.e(tag, "Failed to read cognito credentials");
            throw new RuntimeException("Failed to read cognito credentials");
        }
    }

    private void logTestInfo(String stage) {
        String message = "Model type: " +
            modelType.getSimpleName() +
            " requiresSignIn: " +
            this.requiresSignIn +
            " expectedAuthType: " +
            this.expectedAuthType.name() +
            " stage: " +
            stage;
        Log.i(tag, message);
    }

    private static final class HttpRequestInterceptor implements Interceptor {
        private final Map<Request, Response> okHttpRequests;

        HttpRequestInterceptor() {
            this.okHttpRequests = new HashMap<>();
        }

        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
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
            okHttpRequests.put(copyOfRequest, copyOfResponse);
            ResponseBody responseBody = copyOfResponse.newBuilder().build().body();
            String responseBodyString = responseBody != null ? responseBody.string() : "";

            return originalResponse.newBuilder()
                                   .body(ResponseBody.create(responseBodyString, originalResponse.body().contentType()))
                                   .build();
        }
    }
}
