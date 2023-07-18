/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.api.aws.auth.CognitoJWTParser;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolsAuthProvider;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.auth.result.AuthSignInResult;
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
import com.amplifyframework.logging.Logger;
import com.amplifyframework.testmodels.commentsblog.Author;
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

import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.amplifyframework.core.Resources.readJsonResourceFromId;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.networkStatusFailure;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Tests a set of possible combinations of models, auth modes and login status to
 * verify behavior when in multi-auth mode.
 */
public final class MultiAuthSyncEngineInstrumentationTest {
    private static final Logger LOG = Amplify.Logging.logger(
        CategoryType.DATASTORE,
        "MultiAuthSyncEngineInstrumentationTest"
    );
    private static final int TIMEOUT_SECONDS = 20;
    private static final String AUDIENCE = "integtest";
    private static final String GOOGLE_ISS_CLAIM = "https://accounts.google.com";
    private static SynchronousAuth auth;

    private SynchronousApi api;
    private SynchronousDataStore dataStore;
    private String cognitoUser;
    private String cognitoPassword;
    private final AtomicReference<String> token = new AtomicReference<>();
    private ServiceAccountCredentials googleServiceAccount;
    private HttpRequestInterceptor requestInterceptor;

    /**
     * Sets up fields which are required to be configured only once while running whole test suite.
     * This is suited for things which cannot be called twice during applications lifecycle such as
     * Amplify configuration.
     *
     * @throws AmplifyException     if setup fails
     * @throws InterruptedException if setup fails
     */
    @BeforeClass
    public static void setup() throws AmplifyException, InterruptedException {
        auth = SynchronousAuth.delegatingToCognito(getApplicationContext(),
                new AWSCognitoAuthPlugin());
    }

    /**
     * Class name: Author.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    @Ignore("fix on dev-preview")
    public void testAuthorAnonymous() throws IOException, AmplifyException {
        verifyScenario(Author.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: OwnerUPPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testOwnerUPPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(OwnerUPPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: OwnerOIDCPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: true.
     * Expected result: AuthorizationType.OPENID_CONNECT.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testOwnerOIDCPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(OwnerOIDCPost.class,
                      false,
                      true,
                      AuthorizationType.OPENID_CONNECT
        );
    }

    /**
     * Class name: GroupUPPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testGroupUPPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(GroupUPPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: PrivateUPPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivateUPPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(PrivateUPPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: PublicIAMPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AWS_IAM.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPublicIAMPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PublicIAMPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
    }

    /**
     * Class name: PublicAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPublicAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PublicAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: OwnerPrivateUPIAMPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testOwnerPrivateUPIAMPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(OwnerPrivateUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: OwnerPublicUPAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testOwnerPublicUPAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(OwnerPublicUPAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: OwnerPublicUPAPIPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testOwnerPublicUPAPIPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(OwnerPublicUPAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: OwnerPublicOIDAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: true.
     * Expected result: AuthorizationType.OPENID_CONNECT.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testOwnerPublicOIDAPIPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(OwnerPublicOIDAPIPost.class,
                      false,
                      true,
                      AuthorizationType.OPENID_CONNECT
        );
    }

    /**
     * Class name: GroupPrivateUPIAMPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testGroupPrivateUPIAMPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(GroupPrivateUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: GroupPrivateUPIAMPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: No matching auth types.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    @Ignore("Test is inconsistent, needs further investigation")
    public void testGroupPrivateUPIAMPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(GroupPrivateUPIAMPost.class,
                      false,
                      false,
                      null
        );
    }

    /**
     * Class name: GroupPublicUPAPIPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testGroupPublicUPAPIPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(GroupPublicUPAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: GroupPublicUPAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testGroupPublicUPAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(GroupPublicUPAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: GroupPublicUPIAMPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testGroupPublicUPIAMPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(GroupPublicUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: GroupPublicUPIAMPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AWS_IAM.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testGroupPublicUPIAMPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(GroupPublicUPIAMPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
    }

    /**
     * Class name: PrivatePrivateUPIAMPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePrivateUPIAMPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(PrivatePrivateUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: PrivatePrivateUPIAMPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: No matching auth types.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    @Ignore("Test is inconsistent, needs further investigation")
    public void testPrivatePrivateUPIAMPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PrivatePrivateUPIAMPost.class,
                      false,
                      false,
                      null
        );
    }

    /**
     * Class name: PrivatePublicUPAPIPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePublicUPAPIPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(PrivatePublicUPAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: PrivatePublicUPAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePublicUPAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PrivatePublicUPAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: PrivatePublicUPIAMPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePublicUPIAMPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(PrivatePublicUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: PrivatePublicUPIAMPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AWS_IAM.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePublicUPIAMPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PrivatePublicUPIAMPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
    }

    /**
     * Class name: PublicPublicIAMAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AWS_IAM.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPublicPublicIAMAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PublicPublicIAMAPIPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
    }

    /**
     * Class name: OwnerPrivatePublicUPIAMAPIPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testOwnerPrivatePublicUPIAMAPIPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(OwnerPrivatePublicUPIAMAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: OwnerPrivatePublicUPIAMAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testOwnerPrivatePublicUPIAMAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(OwnerPrivatePublicUPIAMAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: GroupPrivatePublicUPIAMAPIPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testGroupPrivatePublicUPIAMAPIPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(GroupPrivatePublicUPIAMAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: GroupPrivatePublicUPIAMAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testGroupPrivatePublicUPIAMAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(GroupPrivatePublicUPIAMAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: PrivatePrivatePublicUPIAMIAMPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePrivatePublicUPIAMIAMPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(PrivatePrivatePublicUPIAMIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: PrivatePrivatePublicUPIAMIAMPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AWS_IAM.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePrivatePublicUPIAMIAMPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PrivatePrivatePublicUPIAMIAMPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
    }

    /**
     * Class name: PrivatePrivatePublicUPIAMAPIPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePrivatePublicUPIAMAPIPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(PrivatePrivatePublicUPIAMAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: PrivatePrivatePublicUPIAMAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePrivatePublicUPIAMAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PrivatePrivatePublicUPIAMAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: PrivatePublicPublicUPAPIIAMPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePublicPublicUPAPIIAMPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(PrivatePublicPublicUPAPIIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Class name: PrivatePublicComboAPIPost.
     * Signed in with user pools: false.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.API_KEY.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePublicComboAPIPostAnonymous() throws IOException, AmplifyException {
        verifyScenario(PrivatePublicComboAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
    }

    /**
     * Class name: PrivatePublicComboUPPost.
     * Signed in with user pools: true.
     * Signed in with OIDC: false.
     * Expected result: AuthorizationType.AMAZON_COGNITO_USER_POOLS.
     * @throws AmplifyException Not expected.
     * @throws IOException Not expected.
     */
    @Test
    public void testPrivatePublicComboUPPostAuthenticated() throws IOException, AmplifyException {
        verifyScenario(PrivatePublicComboUPPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
    }

    /**
     * Method used to configure each scenario.
     * @param modelType The model type.
     * @param signInToCognito Does the test scenario require the user to be logged in with user pools.
     * @param signInWithOidc Does the test scenario require the user to be logged in with an OIDC provider.
     * @param expectedAuthType The auth type that should succeed for the test.
     * @throws AmplifyException No expected.
     * @throws IOException Not expected.
     */
    private void configure(Class<? extends Model> modelType,
                             boolean signInToCognito,
                             boolean signInWithOidc,
                             AuthorizationType expectedAuthType)
        throws AmplifyException, IOException {
        String tag = modelType.getSimpleName();

        MultiAuthTestModelProvider modelProvider =
            MultiAuthTestModelProvider.getInstance(Collections.singletonList(modelType));
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();

        ModelSchema modelSchema = ModelSchema.fromModelClass(modelType);
        schemaRegistry.register(modelType.getSimpleName(), modelSchema);

        StrictMode.enable();
        Context context = getApplicationContext();
        @RawRes int configResourceId = Resources.getRawResourceId(context, "amplifyconfiguration");
        AmplifyConfiguration amplifyConfiguration = AmplifyConfiguration.fromConfigFile(context, configResourceId);

        readCredsFromConfig(context);

        auth.signOut(AuthSignOutOptions.builder().build());
        if (signInToCognito) {
            Log.v(tag, "Test requires signIn.");
            AuthSignInResult authSignInResult = auth.signIn(cognitoUser, cognitoPassword);
            if (!authSignInResult.isSignedIn()) {
                fail("Unable to complete initial sign-in");
            }
        }

        if (signInWithOidc) {
            oidcLogin();
            if (token.get() == null) {
                fail("Unable to autenticate with OIDC provider");
            }
        }

        // Setup an API
        DefaultCognitoUserPoolsAuthProvider cognitoProvider =
            new DefaultCognitoUserPoolsAuthProvider();
        CategoryConfiguration apiCategoryConfiguration = amplifyConfiguration.forCategoryType(CategoryType.API);
        ApiAuthProviders apiAuthProviders = ApiAuthProviders.builder()
            .cognitoUserPoolsAuthProvider(cognitoProvider)
            .oidcAuthProvider(token::get)
            .build();

        ApiCategory apiCategory = new ApiCategory();
        requestInterceptor = new HttpRequestInterceptor(expectedAuthType);
        apiCategory.addPlugin(AWSApiPlugin.builder()
                                          .configureClient("DataStoreIntegTestsApi", okHttpClientBuilder ->
                                              okHttpClientBuilder.addInterceptor(requestInterceptor)
                                          )
                                          .apiAuthProviders(apiAuthProviders)
                                          .build());
        apiCategory.configure(apiCategoryConfiguration, context);
        api = SynchronousApi.delegatingTo(apiCategory);

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

        String databaseName = "IntegTest" + modelType.getSimpleName() + ".db";
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

    // The following link was helpful in finding the right setup
    // https://github.com/googleapis/google-auth-library-java#google-auth-library-oauth2-http
    private void oidcLogin() {
        try {
            IdToken idToken = googleServiceAccount.idTokenWithAudience(AUDIENCE, Collections.emptyList());
            token.set(idToken.getTokenValue());
        } catch (IOException exception) {
            LOG.warn("An error occurred while trying to authenticate against OIDC provider", exception);
        }
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
        auth = null;
        Log.i("TearDown", "Cleanup completed.");
    }

    private void verifyScenario(Class<? extends Model> modelType,
                                  boolean signInToCognito,
                                  boolean signInWithOidc,
                                  AuthorizationType expectedAuthType) throws AmplifyException, IOException {
        configure(modelType, signInToCognito, signInWithOidc, expectedAuthType);
        String modelId = UUID.randomUUID().toString();
        Model testRecord = createRecord(modelType, modelId);
        HubAccumulator expectedEventAccumulator = null;
        if (expectedAuthType != null) {
            expectedEventAccumulator =
                HubAccumulator
                    .create(HubChannel.DATASTORE, publicationOf(modelType.getSimpleName(),
                            testRecord.getPrimaryKeyString()), 1)
                    .start();
        } else {
            expectedEventAccumulator = HubAccumulator
                    .create(HubChannel.DATASTORE, networkStatusFailure(), 1)
                    .start();
        }
        dataStore.start();
        dataStore.save(testRecord);
        expectedEventAccumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertFalse(requestInterceptor.hasUnexpectedRequests());
        // Sign the user out if sign-in was required.
        if (signInToCognito) {
            auth.signOut(AuthSignOutOptions.builder().build());
        }
    }

    /**
     * Create a instance of the model using the private constructor via reflection.
     * @return A instance of the model being tested.
     */
    private Model createRecord(Class<? extends Model> modelType, String modelId) {
        try {
            String recordDetail = "IntegTest-" + modelType.getSimpleName() + " " + RandomString.string();
            Map<String, Object> modelMap = new HashMap<>();
            modelMap.put("id", modelId);
            modelMap.put("name", recordDetail);
            return SerializedModel.builder()
                    .modelSchema(ModelSchema.fromModelClass(modelType))
                    .serializedData(modelMap)
                    .build();
        } catch (AmplifyException exception) {
            Log.e(modelType.getSimpleName(), "Unable to create an instance of model " + modelType.getSimpleName(),
                  exception);
            throw new RuntimeException("Unable to create an instance of model " + modelType.getSimpleName(), exception);
        }

    }

    /**
     * Reads credential information from a file. In CI, this file will be pulled from an S3 bucket.
     * The resource file being used is in the .gitignore file to prevent accidental commit.
     * @param context The application context.
     */
    private void readCredsFromConfig(Context context) throws IOException {
        //TODO: use secrets manager instead.
        @RawRes int cognitoCredsResourceId = Resources.getRawResourceId(context, "credentials");
        @RawRes int googleCredsResourceId = Resources.getRawResourceId(context, "google_client_creds");
        try {
            JSONObject credentialsJson = readJsonResourceFromId(context, cognitoCredsResourceId);
            cognitoUser = credentialsJson.getJSONObject("datastore")
                                         .getJSONObject("userPool")
                                         .getString("user");

            cognitoPassword = credentialsJson.getJSONObject("datastore")
                                             .getJSONObject("userPool")
                                             .getString("password");

            googleServiceAccount = ServiceAccountCredentials.fromStream(context.getResources()
                                                                               .openRawResource(googleCredsResourceId));

        } catch (com.amplifyframework.core.Resources.ResourceLoadingException | JSONException exception) {
            Log.e("ReadingCredentials", "Failed to read cognito credentials");
            throw new RuntimeException("Failed to read cognito credentials", exception);
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
        String iss = CognitoJWTParser.Companion.getClaim(authHeaderValue, "iss");
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
