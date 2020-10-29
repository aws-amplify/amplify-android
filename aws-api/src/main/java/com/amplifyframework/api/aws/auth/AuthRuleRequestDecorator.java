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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Processor that can decorate an AppSync-compliant GraphQL request with additional variables
 * that are required for owner-based or group-based authorization.
 */
public final class AuthRuleRequestDecorator {
    private final ApiAuthProviders authProvider;

    /**
     * Constructs a new instance of GraphQL request's auth rule processor.
     * @param authProvider the auth providers to authorize requests
     */
    public AuthRuleRequestDecorator(@NonNull ApiAuthProviders authProvider) {
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
    public <R> GraphQLRequest<R> decorate(
            @NonNull GraphQLRequest<R> request,
            @NonNull AuthorizationType authType
    ) throws ApiException {
        if (!(request instanceof AppSyncGraphQLRequest)) {
            return request;
        }

        AppSyncGraphQLRequest<R> appSyncRequest = (AppSyncGraphQLRequest<R>) request;
        AuthRule ownerRuleWithReadRestriction = null;
        Map<String, Set<String>> readAuthorizedGroupsMap = new HashMap<>();

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
                        "We currently do not support this use case. Please limit your type to just one owner " +
                            "auth rule with a READ operation restriction.");
                }
            } else if (isReadRestrictingStaticGroup(authRule)) {
                // Group read-restricting groups by the claim name
                String groupClaim = authRule.getGroupClaimOrDefault();
                List<String> groups = authRule.getGroups();
                Set<String> readAuthorizedGroups = readAuthorizedGroupsMap.get(groupClaim);
                if (readAuthorizedGroups != null) {
                    readAuthorizedGroups.addAll(groups);
                } else {
                    readAuthorizedGroupsMap.put(groupClaim, new HashSet<>(groups));
                }
            }
        }

        // We only add the owner parameter to the subscription if there is an owner rule with a READ restriction
        // and either there are no group auth rules with read access or there are but the user isn't in any of
        // them.
        if (ownerRuleWithReadRestriction != null
                && userNotInReadRestrictingGroups(readAuthorizedGroupsMap, authType)) {
            String idClaim = ownerRuleWithReadRestriction.getIdentityClaimOrDefault();
            String key = ownerRuleWithReadRestriction.getOwnerFieldOrDefault();
            String value = getIdentityValue(idClaim, authType);

            try {
                return appSyncRequest.newBuilder()
                    .variable(key, "String!", value)
                    .build();
            } catch (AmplifyException error) {
                // This should not happen normally
                throw new ApiException(
                    "Failed to set owner field on AppSyncGraphQLRequest.", error,
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION);
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
                && !authRule.getGroups().isEmpty()
                && authRule.getOperationsOrDefault().contains(ModelOperation.READ);
    }

    private String getIdentityValue(String identityClaim, AuthorizationType authType) throws ApiException {
        try {
            return CognitoJWTParser
                    .getPayload(getAuthToken(authType))
                    .getString(identityClaim);
        } catch (JSONException error) {
            throw new ApiException(
                "Attempted to subscribe to a model with owner-based authorization without " + identityClaim + " " +
                    "which was specified (or defaulted to) as the identity claim.",
                "If you did not specify a custom identityClaim in your schema, make sure you are logged in. If " +
                    "you did, check that the value you specified in your schema is present in the access key."
            );
        } catch (CognitoParameterInvalidException error) {
            throw new ApiException(
                "Failed to parse the ID token for identity claim: " + error.getMessage(),
                "Please verify the validity of token vended by the registered auth provider."
            );
        }
    }

    private ArrayList<String> getUserGroups(String groupClaim, AuthorizationType authType) throws ApiException {
        ArrayList<String> groups = new ArrayList<>();
        try {
            JSONObject accessToken = CognitoJWTParser
                    .getPayload(getAuthToken(authType));
            if (accessToken.has(groupClaim)) {
                JSONArray jsonGroups = accessToken.getJSONArray(groupClaim);
                for (int index = 0; index < jsonGroups.length(); index++) {
                    groups.add(jsonGroups.getString(index));
                }
            }
        } catch (JSONException error) {
            // This should not happen normally
            throw new ApiException(
                "Failed obtain group claim from the parsed JWT token.", error,
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        } catch (CognitoParameterInvalidException error) {
            throw new ApiException(
                "Failed to parse the ID token for group claim: " + error.getMessage(),
                "Please verify the validity of token vended by the registered auth provider."
            );
        }

        return groups;
    }

    private boolean userNotInReadRestrictingGroups(
            Map<String, Set<String>> readAuthorizedGroupsMap,
            AuthorizationType authType
    ) throws ApiException {
        // Iterate through map of "group claim" -> "read-restricting groups from that claim". e.g.
        // {
        //   "https://myapp.com/claims/groups" -> {Admins}
        //   "https://differentapp.com/claims/groups" -> {Moderators, Editors}
        // }
        for (Map.Entry<String, Set<String>> entry : readAuthorizedGroupsMap.entrySet()) {
            String groupClaim = entry.getKey();
            // Get a list of groups that user belongs in for a given claim.
            // e.g. [Admins, User]
            List<String> userGroups = getUserGroups(groupClaim, authType);
            Set<String> readAuthorizedGroups = entry.getValue();
            // If user belongs in any group for a given group claim, return false.
            if (!Collections.disjoint(userGroups, readAuthorizedGroups)) {
                return false;
            }
        }
        return true;
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
                    "Tried to use owner/group-based authorization on an API that is not configured " +
                        "with either Cognito User Pools or OpenID Connect.",
                    "Verify that the API is configured with either Cognito User Pools or OpenID Connect. @auth " +
                        "with owner/group-based authorization is not supported for other modes."
                );
        }
    }
}
