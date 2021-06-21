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

import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;
import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
public class MultiAuthSyncEngineInstrumentationTest {
    private static final Logger LOG = Amplify.Logging.forNamespace("MultiAuthSyncEngineInstrumentationTest");
    private static final int TIMEOUT_SECONDS = 20;
    private static final String AUDIENCE = "integtest";
    private static final String GOOGLE_ISS_CLAIM = "https://accounts.google.com";

    private Class<? extends Model> modelType;
    private boolean requiresCognitoSign;
    private boolean requiresOidcSignIn;
    private AuthorizationType expectedAuthType;
    private String tag;
    private String modelId;
    private SynchronousApi api;
    private SynchronousDataStore dataStore;
    private SynchronousAuth auth;
    private String cognitoUser;
    private String cognitoPassword;
    private final AtomicReference<String> token = new AtomicReference<>();
    private ServiceAccountCredentials googleServiceAccount;
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
    public void testAuthorAnonymous() throws IOException, AmplifyException {
        setupScenario(Author.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(OwnerUPPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(OwnerOIDCPost.class,
                      false,
                      true,
                      AuthorizationType.OPENID_CONNECT
        );
        verifyScenario();
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
        setupScenario(GroupUPPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(PrivateUPPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(PublicIAMPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
        verifyScenario();
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
        setupScenario(PublicAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(OwnerPrivateUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(OwnerPublicUPAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(OwnerPublicUPAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(OwnerPublicOIDAPIPost.class,
                      false,
                      true,
                      AuthorizationType.OPENID_CONNECT
        );
        verifyScenario();
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
        setupScenario(GroupPrivateUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
    public void testGroupPrivateUPIAMPostAnonymous() throws IOException, AmplifyException {
        setupScenario(GroupPrivateUPIAMPost.class,
                      false,
                      false,
                      null
        );
        verifyScenario();
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
        setupScenario(GroupPublicUPAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(GroupPublicUPAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(GroupPublicUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(GroupPublicUPIAMPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
        verifyScenario();
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
        setupScenario(PrivatePrivateUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
    public void testPrivatePrivateUPIAMPostAnonymous() throws IOException, AmplifyException {
        setupScenario(PrivatePrivateUPIAMPost.class,
                      false,
                      false,
                      null
        );
        verifyScenario();
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
        setupScenario(PrivatePublicUPAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(PrivatePublicUPAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(PrivatePublicUPIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(PrivatePublicUPIAMPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
        verifyScenario();
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
        setupScenario(PublicPublicIAMAPIPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
        verifyScenario();
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
        setupScenario(OwnerPrivatePublicUPIAMAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(OwnerPrivatePublicUPIAMAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(GroupPrivatePublicUPIAMAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(GroupPrivatePublicUPIAMAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(PrivatePrivatePublicUPIAMIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(PrivatePrivatePublicUPIAMIAMPost.class,
                      false,
                      false,
                      AuthorizationType.AWS_IAM
        );
        verifyScenario();
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
        setupScenario(PrivatePrivatePublicUPIAMAPIPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(PrivatePrivatePublicUPIAMAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(PrivatePublicPublicUPAPIIAMPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
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
        setupScenario(PrivatePublicComboAPIPost.class,
                      false,
                      false,
                      AuthorizationType.API_KEY
        );
        verifyScenario();
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
        setupScenario(PrivatePublicComboUPPost.class,
                      true,
                      false,
                      AuthorizationType.AMAZON_COGNITO_USER_POOLS
        );
        verifyScenario();
    }

    /**
     * Constructor for the parameterized test.
     * @param clazz The model type.
     * @param signInToCognito Does the test scenario require the user to be logged in with user pools.
     * @param signInWithOidc Does the test scenario require the user to be logged in with an OIDC provider.
     * @param expectedAuthType The auth type that should succeed for the test.
     * @throws AmplifyException No expected.
     * @throws IOException Not expected.
     */
    private void setupScenario(Class<? extends Model> clazz,
                                                  boolean signInToCognito,
                                                  boolean signInWithOidc,
                                                  AuthorizationType expectedAuthType)
        throws AmplifyException, IOException {
        Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));
        this.tag = clazz.getSimpleName();
        this.modelType = clazz;
        this.requiresCognitoSign = signInToCognito;
        this.requiresOidcSignIn = signInWithOidc;
        this.expectedAuthType = expectedAuthType;
        this.modelId = UUID.randomUUID().toString();
        logTestInfo("Configuring");

        MultiAuthTestModelProvider modelProvider =
            MultiAuthTestModelProvider.getInstance(Collections.singletonList(modelType));
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();

        modelSchemaRegistry.register(modelType.getSimpleName(), ModelSchema.fromModelClass(modelType));

        StrictMode.enable();
        Context context = getApplicationContext();
        @RawRes int configResourceId = Resources.getRawResourceId(context, "amplifyconfiguration");
        AmplifyConfiguration amplifyConfiguration = AmplifyConfiguration.fromConfigFile(context, configResourceId);

        readCredsFromConfig(context);

        // Setup an auth plugin
        CategoryConfiguration authCategoryConfiguration = amplifyConfiguration.forCategoryType(CategoryType.AUTH);
        AuthCategory authCategory = new AuthCategory();
        AWSCognitoAuthPlugin authPlugin = new AWSCognitoAuthPlugin();
        authCategory.addPlugin(authPlugin);
        authCategory.configure(authCategoryConfiguration, context);
        auth = SynchronousAuth.delegatingTo(authCategory);
        if (this.requiresCognitoSign) {
            Log.v(tag, "Test requires signIn.");
            AuthSignInResult authSignInResult = auth.signIn(cognitoUser, cognitoPassword);
            if (!authSignInResult.isSignInComplete()) {
                fail("Unable to complete initial sign-in");
            }
        }

        if (this.requiresOidcSignIn) {
            oidcLogin();
            if (token.get() == null) {
                fail("Unable to autenticate with OIDC provider");
            }
        }

        // Setup an API
        DefaultCognitoUserPoolsAuthProvider cognitoProvider =
            new DefaultCognitoUserPoolsAuthProvider(authPlugin.getEscapeHatch());
        CategoryConfiguration apiCategoryConfiguration = amplifyConfiguration.forCategoryType(CategoryType.API);
        ApiAuthProviders apiAuthProviders = ApiAuthProviders.builder()
                                                            .cognitoUserPoolsAuthProvider(cognitoProvider)
                                                            .awsCredentialsProvider(authPlugin.getEscapeHatch())
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
                                                                .errorHandler(exception -> {
                                                                    Log.e(tag,
                                                                          "DataStore error handler received an error.",
                                                                          exception);
                                                                })
                                                                .build();
        CategoryConfiguration dataStoreCategoryConfiguration =
            AmplifyConfiguration.fromConfigFile(context, configResourceId)
                                .forCategoryType(CategoryType.DATASTORE);

        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                                                                  .modelProvider(modelProvider)
                                                                  .apiCategory(apiCategory)
                                                                  .authModeStrategy(AuthModeStrategyType.MULTIAUTH)
                                                                  .modelSchemaRegistry(modelSchemaRegistry)
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
    @After
    public void tearDown() {
        Log.i(tag, "Deleting database");
        getApplicationContext().deleteDatabase("AmplifyDatastore.db");
        Log.i(tag, "Teardown completed.");
    }

    /**
     * Runs the test for the parameters set in the constructor.
     * @throws AmplifyException Not expected.
     */
    private void verifyScenario() throws AmplifyException {
        logTestInfo("Starting");
        Model testRecord = createRecord();
        HubAccumulator expectedEventAccumulator = null;
        if (expectedAuthType != null) {
            expectedEventAccumulator =
                HubAccumulator
                    .create(HubChannel.DATASTORE, publicationOf(modelType.getSimpleName(), testRecord.getId()), 1)
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
    }

    /**
     * Create a instance of the model using the private constructor via reflection.
     * @return A instance of the model being tested.
     */
    private Model createRecord() {
        try {
            Constructor<?> constructor = modelType.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            String recordDetail = "IntegTest-" + modelType.getSimpleName() + " " + RandomString.string();
            // Most constructor for the multiauth test models have 2 parameters.
            if (constructor.getParameterCount() == 2) {
                return (Model) constructor.newInstance(modelId, recordDetail);
            }
            // For the one exception, we just pass a null for the extra parameter.
            return (Model) constructor.newInstance(modelId, recordDetail, null);
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
            Log.e(tag, "Failed to read cognito credentials");
            throw new RuntimeException("Failed to read cognito credentials", exception);
        }
    }

    private void logTestInfo(String stage) {
        String message = "Model type: " +
            modelType.getSimpleName() +
            " requiresSignIn: " +
            this.requiresCognitoSign +
            " expectedAuthType: " +
            (this.expectedAuthType == null ? "FAILURE" : this.expectedAuthType.name()) +
            " stage: " +
            stage;
        Log.i(tag, message);
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
        String iss = CognitoJWTParser.getClaim(authHeaderValue, "iss");
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
                    if (expectedAuthType.equals(requestAuthType)) {
                        return true;
                    }
                } else {
                    // Request was successful. Make sure it's the expected auth type.
                    if (!expectedAuthType.equals(requestAuthType)) {
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
