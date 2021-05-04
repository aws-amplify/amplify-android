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

package com.amplifyframework.api.aws.auth;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.ApiAuthProviders;
import com.amplifyframework.api.aws.ApiGraphQLRequestOptions;
import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.Operation;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.api.graphql.model.ModelSubscription;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.AuthorizationType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.ModelConfig;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests that auth rule decorator can correctly decorate a request given an auth rule.
 */
@RunWith(RobolectricTestRunner.class)
public final class AuthRuleRequestDecoratorTest {
    private AuthRuleRequestDecorator decorator;

    /**
     * Sets up the test.
     */
    @Before
    public void setup() {
        ApiAuthProviders authProviders = ApiAuthProviders.builder()
                .cognitoUserPoolsAuthProvider(new FakeCognitoAuthProvider())
                .oidcAuthProvider(new FakeOidcAuthProvider())
                .build();
        decorator = new AuthRuleRequestDecorator(authProviders);
    }

    /**
     * Test that auth rule request decorator returns the same request if there
     * is no auth rule associated with it.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void requestPassThroughForNoAuth() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.AMAZON_COGNITO_USER_POOLS;

        // NoAuth class does not have use @auth directive
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<NoAuth> originalRequest = createRequest(NoAuth.class, subscriptionType);
            GraphQLRequest<NoAuth> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertEquals(originalRequest, modifiedRequest);
        }
    }

    /**
     * Test that owner argument fails to be appended to subscription request if
     * the authorization mode is not OIDC compliant.
     * @throws ApiException if request contains unsupported auth rule
     */
    @Test(expected = ApiException.class)
    public void ownerArgumentNotAddedWithApiKey() throws ApiException {
        decorator.decorate(ModelSubscription.onCreate(Owner.class), AuthorizationType.API_KEY);
    }

    /**
     * Test that owner argument fails to be appended to subscription request if
     * the authorization mode is not OIDC compliant.
     * @throws ApiException if request contains unsupported auth rule
     */
    @Test(expected = ApiException.class)
    public void ownerArgumentNotAddedWithAwsIam() throws ApiException {
        decorator.decorate(ModelSubscription.onCreate(Owner.class), AuthorizationType.AWS_IAM);
    }

    /**
     * Verify that owner argument is required for all subscriptions if ModelOperation.READ is specified
     * while using Cognito User Pools auth mode.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedForRestrictedReadWithUserPools() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.AMAZON_COGNITO_USER_POOLS;
        final String expectedOwner = FakeCognitoAuthProvider.USERNAME;

        // Owner class has restriction on every operation including READ
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<Owner> originalRequest = createRequest(Owner.class, subscriptionType);
            GraphQLRequest<Owner> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertEquals(expectedOwner, getOwnerField(modifiedRequest));
        }

        // OwnerRead class only has restriction on READ
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerRead> originalRequest = createRequest(OwnerRead.class, subscriptionType);
            GraphQLRequest<OwnerRead> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertEquals(expectedOwner, getOwnerField(modifiedRequest));
        }
    }

    /**
     * Verify that owner argument is required for all subscriptions if ModelOperation.READ is specified
     * while using OpenID Connect auth mode.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedForRestrictedReadWithOidc() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.OPENID_CONNECT;
        final String expectedOwner = FakeOidcAuthProvider.SUB;

        // OwnerOidc class has restriction on every operation including READ
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerOidc> originalRequest = createRequest(OwnerOidc.class, subscriptionType);
            GraphQLRequest<OwnerOidc> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertEquals(expectedOwner, getOwnerField(modifiedRequest));
        }
    }

    /**
     * Verify owner argument is NOT required if the subscription type is not one of the restricted operations.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedForNonRestrictedReadWithUserPools() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.AMAZON_COGNITO_USER_POOLS;

        // OwnerCreate class only has restriction on CREATE
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerCreate> originalRequest = createRequest(OwnerCreate.class, subscriptionType);
            GraphQLRequest<OwnerCreate> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertNull(getOwnerField(modifiedRequest));
        }

        // OwnerUpdate class only has restriction on UPDATE
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerUpdate> originalRequest = createRequest(OwnerUpdate.class, subscriptionType);
            GraphQLRequest<OwnerUpdate> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertNull(getOwnerField(modifiedRequest));
        }

        // OwnerDelete class only has restriction on DELETE
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerDelete> originalRequest = createRequest(OwnerDelete.class, subscriptionType);
            GraphQLRequest<OwnerDelete> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertNull(getOwnerField(modifiedRequest));
        }
    }

    /**
     * Verify owner argument is NOT added if authStrategy is not OWNER.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedForNonOwnerBasedAuth() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.AMAZON_COGNITO_USER_POOLS;

        // Public class opens up every operation to the public
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<Public> originalRequest = createRequest(Public.class, subscriptionType);
            GraphQLRequest<Public> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertNull(getOwnerField(modifiedRequest));
        }

        // Private class only allows the correct IAM user
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<Private> originalRequest = createRequest(Private.class, subscriptionType);
            GraphQLRequest<Private> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertNull(getOwnerField(modifiedRequest));
        }

        // Group class only has group-based auth enabled
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<Group> originalRequest = createRequest(Group.class, subscriptionType);
            GraphQLRequest<Group> modifiedRequest = decorator.decorate(originalRequest, mode);
            assertNull(getOwnerField(modifiedRequest));
        }
    }

    /**
     * Verify owner argument is added if model contains both owner-based and group-based
     * authorization and the user is not in any read-restricted group.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedIfOwnerIsNotInGroupWithUserPools() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.AMAZON_COGNITO_USER_POOLS;
        final String expectedOwner = FakeCognitoAuthProvider.USERNAME;

        // OwnerNotInGroup class uses combined owner and group-based auth,
        // but user is not in the read-restricted group.
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerNotInGroup> originalRequest =
                    createRequest(OwnerNotInGroup.class, subscriptionType);
            GraphQLRequest<OwnerNotInGroup> modifiedRequest =
                    decorator.decorate(originalRequest, mode);
            assertEquals(expectedOwner, getOwnerField(modifiedRequest));
        }
    }

    /**
     * Verify owner argument is NOT added if model contains both owner-based and group-based
     * authorization and the user is in any of the read-restricted groups.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedIfOwnerIsInGroupWithUserPools() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.AMAZON_COGNITO_USER_POOLS;

        // OwnerInGroup class uses combined owner and group-based auth,
        // and user is in the read-restricted group.
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerInGroup> originalRequest =
                    createRequest(OwnerInGroup.class, subscriptionType);
            GraphQLRequest<OwnerInGroup> modifiedRequest =
                    decorator.decorate(originalRequest, mode);
            assertNull(getOwnerField(modifiedRequest));
        }
    }

    /**
     * Verify owner argument is added if model contains both owner-based and group-based
     * authorization and the user is not in any read-restricted group.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedIfOwnerIsNotInCustomGroup() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.OPENID_CONNECT;
        final String expectedOwner = FakeOidcAuthProvider.SUB;

        // OwnerNotInCustomGroup class uses combined owner and group-based auth,
        // but user is not in the read-restricted custom group.
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerNotInCustomGroup> originalRequest =
                    createRequest(OwnerNotInCustomGroup.class, subscriptionType);
            GraphQLRequest<OwnerNotInCustomGroup> modifiedRequest =
                    decorator.decorate(originalRequest, mode);
            assertEquals(expectedOwner, getOwnerField(modifiedRequest));
        }
    }

    /**
     * Verify owner argument is NOT added if model contains both owner-based and group-based
     * authorization and the user is in any of the read-restricted groups.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedIfOwnerIsInCustomGroup() throws AmplifyException {
        final AuthorizationType mode = AuthorizationType.OPENID_CONNECT;

        // OwnerInCustomGroup class uses combined owner and group-based auth,
        // and user is in the read-restricted custom group.
        for (SubscriptionType subscriptionType : SubscriptionType.values()) {
            GraphQLRequest<OwnerInCustomGroup> originalRequest =
                    createRequest(OwnerInCustomGroup.class, subscriptionType);
            GraphQLRequest<OwnerInCustomGroup> modifiedRequest =
                    decorator.decorate(originalRequest, mode);
            assertNull(getOwnerField(modifiedRequest));
        }
    }

    private <M extends Model> String getOwnerField(GraphQLRequest<M> request) {
        if (request.getVariables().containsKey("owner")) {
            return (String) request.getVariables().get("owner");
        }
        return null;
    }

    // Simple subscription request with given model class and operation
    private <M extends Model> GraphQLRequest<M> createRequest(Class<M> clazz, Operation operation)
            throws AmplifyException {
        return AppSyncGraphQLRequest.builder()
                .modelClass(clazz)
                .operation(operation)
                .requestOptions(new ApiGraphQLRequestOptions())
                .responseType(clazz)
                .build();
    }

    private static final class FakeCognitoAuthProvider implements CognitoUserPoolsAuthProvider {
        private static final String USERNAME = "facebook-test-user";
        private static final List<String> GROUPS = Collections.singletonList("Admins");

        @Override
        public String getLatestAuthToken() {
            return FakeJWTToken.builder()
                    .putPayload("username", USERNAME)
                    .putPayload("cognito:groups", new JSONArray(GROUPS))
                    .build()
                    .asString();
        }

        @Override
        public String getUsername() {
            return USERNAME;
        }
    }

    private static final class FakeOidcAuthProvider implements OidcAuthProvider {
        private static final String SUB = "google-test-user";
        private static final String APP_1_GROUP_CLAIM = "http://app1.com/claims/groups";
        private static final String APP_2_GROUP_CLAIM = "http://app2.com/claims/groups";
        private static final List<String> APP_1_GROUPS = Collections.singletonList("Admins");
        private static final List<String> APP_2_GROUPS = Collections.singletonList("Editors");

        @Override
        public String getLatestAuthToken() {
            return FakeJWTToken.builder()
                    .putPayload("sub", SUB)
                    .putPayload(APP_1_GROUP_CLAIM, new JSONArray(APP_1_GROUPS))
                    .putPayload(APP_2_GROUP_CLAIM, new JSONArray(APP_2_GROUPS))
                    .build()
                    .asString();
        }
    }

    @ModelConfig
    private abstract static class NoAuth implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.PUBLIC) })
    private abstract static class Public implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.PRIVATE) })
    private abstract static class Private implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER) })
    private abstract static class Owner implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.CREATE)})
    private abstract static class OwnerCreate implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.READ)})
    private abstract static class OwnerRead implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.UPDATE)})
    private abstract static class OwnerUpdate implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.DELETE)})
    private abstract static class OwnerDelete implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, identityClaim = "sub") })
    private abstract static class OwnerOidc implements Model {}

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.GROUPS, groups = "Admins") })
    private abstract static class Group implements Model {}

    @ModelConfig(authRules = {
        @AuthRule(allow = AuthStrategy.OWNER),
        @AuthRule(allow = AuthStrategy.GROUPS, groups = "Admins")
    })
    private abstract static class OwnerInGroup implements Model {}

    @ModelConfig(authRules = {
        @AuthRule(allow = AuthStrategy.OWNER),
        @AuthRule(allow = AuthStrategy.GROUPS, groups = "Editors")
    })
    private abstract static class OwnerNotInGroup implements Model {}

    @ModelConfig(authRules = {
        @AuthRule(allow = AuthStrategy.OWNER, identityClaim = "sub"),
        @AuthRule(
            allow = AuthStrategy.GROUPS,
            groupClaim = FakeOidcAuthProvider.APP_1_GROUP_CLAIM,
            groups = "Admins"
        )
    })
    private abstract static class OwnerInCustomGroup implements Model {}

    @ModelConfig(authRules = {
        @AuthRule(allow = AuthStrategy.OWNER, identityClaim = "sub"),
        @AuthRule(
            allow = AuthStrategy.GROUPS,
            groupClaim = FakeOidcAuthProvider.APP_1_GROUP_CLAIM,
            groups = "Editors"
        )
    })
    private abstract static class OwnerNotInCustomGroup implements Model {}
}
