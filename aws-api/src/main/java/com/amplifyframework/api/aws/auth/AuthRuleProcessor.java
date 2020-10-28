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

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.ApiAuthProviders;
import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.ModelOperation;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.exceptions.CognitoParameterInvalidException;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * Processor that can decorate an AppSync-compliant GraphQL request with additional variables
 * that are required for owner-based or group-based authorization.
 */
public final class AuthRuleProcessor {
    private final ApiAuthProviders authProvider;

    /**
     * Constructs a new instance of GraphQL request's auth rule processor.
     * @param authProvider the auth providers to authorize requests
     */
    public AuthRuleProcessor(@NonNull ApiAuthProviders authProvider) {
        this.authProvider = Objects.requireNonNull(authProvider);
    }

    /**
     * Decorate given GraphQL request instance with additional variables for owner-based or
     * group-based authorization.
     *
     * This will only work if the request is compliant with the AppSync specifications.
     * @param request an instance of {@link GraphQLRequest}.
     * @param authType the mode of authorization being used to authorize the request
     * @param <R> The type of data contained in the GraphQLResponse expected from this request.
     * @return the input request with additional variables that specify model's owner and/or
     *          groups
     * @throws ApiException If an error is encountered while processing the auth rules associated
     *          with the request or if the authorization fails
     */
    public <R> GraphQLRequest<R> process(
            @NonNull GraphQLRequest<R> request,
            @NonNull AuthorizationType authType
    ) throws ApiException {
        if (!(request instanceof AppSyncGraphQLRequest)) {
            return request;
        }

        AppSyncGraphQLRequest<R> appSyncRequest = (AppSyncGraphQLRequest<R>) request;
        AuthRule ownerRuleWithReadRestriction = null;
        ArrayList<String> readAuthorizedGroups = new ArrayList<>();

        // Note that we are intentionally supporting only one owner rule with a READ operation at this time.
        // If there is more than one, the operation will fail because AppSync generates a parameter for each
        // one. The question then is which one do we pass. JavaScript currently doesn't support this use case
        // and it's not clear what a good solution would be until AppSync supports real time filters.
        for (AuthRule authRule : appSyncRequest.getModelSchema().getAuthRules()) {
            if (isReadRestrictingOwner(authRule)) {
                if (ownerRuleWithReadRestriction == null) {
                    ownerRuleWithReadRestriction = authRule;
                } else {
                    throw new ApiException(
                            "Detected multiple owner type auth rules with a READ operation",
                            "We currently do not support this use case. Please limit your type to just one " +
                                    "owner auth rule with a READ operation restriction."
                    );
                }
            } else if (isReadRestrictingStaticGroup(authRule)) {
                readAuthorizedGroups.addAll(authRule.getGroups());
            }
        }

        // We only add the owner parameter to the subscription if there is an owner rule with a READ restriction
        // and either there are no group auth rules with read access or there are but the user isn't in any of
        // them.
        if (ownerRuleWithReadRestriction != null
                && (readAuthorizedGroups.isEmpty()
                || Collections.disjoint(readAuthorizedGroups, getUserGroups(authType)))
        ) {
            String idClaim = ownerRuleWithReadRestriction.getIdentityClaimOrDefault();
            String key = ownerRuleWithReadRestriction.getOwnerFieldOrDefault();
            String value = getIdentityValue(idClaim, authType);

            try {
                return appSyncRequest.newBuilder()
                        .variable(key, "String!", value)
                        .build();
            } catch (AmplifyException exception) {
                throw new ApiException(
                        "Failed to set owner field on AppSyncGraphQLRequest", exception,
                        "See attached exception for details.");
            }
        }

        return request;
    }

    private boolean isReadRestrictingOwner(AuthRule authRule) {
        return AuthStrategy.OWNER.equals(authRule.getAuthStrategy())
                && authRule.getOperationsOrDefault().contains(ModelOperation.READ);
    }

    private boolean isReadRestrictingStaticGroup(AuthRule authRule) {
        return AuthStrategy.GROUPS.equals(authRule.getAuthStrategy())
                && authRule.getGroups() != null && !authRule.getGroups().isEmpty()
                && authRule.getOperationsOrDefault().contains(ModelOperation.READ);
    }

    private String getIdentityValue(String identityClaim, AuthorizationType authType) throws ApiException {
        String identityValue = null;

        try {
            identityValue = CognitoJWTParser
                    .getPayload(getAuthToken(authType))
                    .getString(identityClaim);
        } catch (JSONException | CognitoParameterInvalidException error) {
            // Could not read identity value from the token...
            // Exception will be thrown so do nothing for now
        }

        if (identityValue == null || identityValue.isEmpty()) {
            throw new ApiException(
                    "Attempted to subscribe to a model with owner based authorization without " + identityClaim + " " +
                            "which was specified (or defaulted to) as the identity claim.",
                    "If you did not specify a custom identityClaim in your schema, make sure you are logged in. If " +
                            "you did, check that the value you specified in your schema is present in the access key."
            );
        }

        return identityValue;
    }

    private ArrayList<String> getUserGroups(AuthorizationType authType) throws ApiException {
        // Custom groups claim isn't supported yet.
        if (!AuthorizationType.AMAZON_COGNITO_USER_POOLS.equals(authType)) {
            throw new ApiException("Custom groups claim is not supported yet.",
                    "Please use Amazon Cognito User Pools to authorize your API.");
        }

        ArrayList<String> groups = new ArrayList<>();
        final String GROUPS_KEY = "cognito:groups";

        try {
            JSONObject accessToken = CognitoJWTParser.getPayload(getAuthToken(authType));

            if (accessToken.has(GROUPS_KEY)) {
                JSONArray jsonGroups = accessToken.getJSONArray(GROUPS_KEY);

                for (int i = 0; i < jsonGroups.length(); i++) {
                    groups.add(jsonGroups.getString(i));
                }
            }
        } catch (JSONException | CognitoParameterInvalidException error) {
            throw new ApiException(
                    "Failed to parse groups from auth rule.",
                    error,
                    "This should never happen - see attached exception for more details and report to us on GitHub."
            );
        }

        return groups;
    }

    private String getAuthToken(AuthorizationType authType) throws ApiException {
        switch (authType) {
            case AMAZON_COGNITO_USER_POOLS:
                CognitoUserPoolsAuthProvider cognitoProvider = authProvider.getCognitoUserPoolsAuthProvider();
                if (cognitoProvider == null) {
                    cognitoProvider = new DefaultCognitoUserPoolsAuthProvider();
                }
                return cognitoProvider.getLatestAuthToken();
            case OPENID_CONNECT:
                OidcAuthProvider oidcProvider = authProvider.getOidcAuthProvider();
                if (oidcProvider == null) {
                    throw new ApiException(
                            "OidcAuthProvider interface is not implemented.",
                            "Configure AWSApiPlugin with ApiAuthProviders containing an implementation of " +
                                    "OidcAuthProvider interface that can vend a valid JWT token."
                    );
                }
                return oidcProvider.getLatestAuthToken();
            case API_KEY:
            case AWS_IAM:
            case NONE:
            default:
                throw new ApiException(
                        "Failed to obtain access token from the configured auth provider.",
                        "Verify that the API is configured with either Cognito User Pools or OpenID Connect."
                );
        }
    }
}
