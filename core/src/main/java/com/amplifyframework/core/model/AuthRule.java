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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Empty;
import com.amplifyframework.util.Immutable;

import java.util.Arrays;
import java.util.List;

/**
 * {@link AuthRule} is used define an authorization rule for who can access and operate against a
 * {@link Model} or a {@link ModelField}.
 *
 * @see <a href="https://docs.amplify.aws/cli/graphql-transformer/directives#auth">GraphQL Transformer @auth directive
 * documentation.</a>
 */
public final class AuthRule {
    private static final String DEFAULT_OWNER_FIELD = "owner";
    private static final String DEFAULT_IDENTITY_CLAIM = "username";
    private static final String DEFAULT_GROUPS_FIELD = "groups";
    private static final String DEFAULT_GROUP_CLAIM = "cognito:groups";

    private final AuthStrategy authStrategy;
    private final String ownerField;
    private final String identityClaim;
    private final String groupsField;
    private final String groupClaim;
    private final List<String> groups;
    private final List<ModelOperation> operations;

    /**
     * Constructor to create an {@link AuthRule} from an {@link com.amplifyframework.core.model.annotations.AuthRule}
     * annotation.
     * @param authRule an {@link com.amplifyframework.core.model.annotations.AuthRule} annotation.
     */
    public AuthRule(com.amplifyframework.core.model.annotations.AuthRule authRule) {
        this.authStrategy = authRule.allow();
        this.ownerField = authRule.ownerField();
        this.identityClaim = authRule.identityClaim();
        this.groupClaim = authRule.groupClaim();
        this.groups = Arrays.asList(authRule.groups());
        this.groupsField = authRule.groupsField();
        this.operations = Arrays.asList(authRule.operations());
    }

    /**
     * Returns the type of strategy for this {@link AuthRule}.
     * @return the type of strategy for this {@link AuthRule}
     */
    @NonNull
    public AuthStrategy getAuthStrategy() {
        return authStrategy;
    }

    /**
     * Used for owner authorization.
     * Defaults to "owner" when using {@link AuthStrategy#OWNER}.
     *
     * @return name of a {@link ModelField} of type String which specifies the user which should have access
     */
    @NonNull
    public String getOwnerFieldOrDefault() {
        return Empty.check(ownerField)
            ? DEFAULT_OWNER_FIELD
            : ownerField;
    }

    /**
     * Used to specify a custom claim.
     * Defaults to "username" when using AuthStrategy.OWNER.
     *
     * Note: An older version of the CLI incorrectly generated a value of "cognito:username"
     * so we also check for this value and convert it to the proper default of "username" for 
     * backwards compatibility.
     *
     * @return identity claim
     */
    @NonNull
    public String getIdentityClaimOrDefault() {
        final String cliGeneratedIdentityClaim = "cognito:username";
        return Empty.check(identityClaim) || cliGeneratedIdentityClaim.equals(identityClaim)
            ? DEFAULT_IDENTITY_CLAIM
            : identityClaim;
    }

    /**
     * Used for dynamic group authorization.
     * Defaults to "groups" when using AuthStrategy.GROUPS.
     *
     * @return name of a {@link ModelField} of type String or array of Strings which specifies a group or list of groups
     * which should have access.
     */
    @NonNull
    public String getGroupsFieldOrDefault() {
        return Empty.check(groupsField)
            ? DEFAULT_GROUPS_FIELD
            : groupsField;
    }

    /**
     * Used to specify a custom claim.
     * Defaults to "cognito:groups" when using AuthStrategy.GROUPS.
     *
     * @return group claim
     */
    @NonNull
    public String getGroupClaimOrDefault() {
        return Empty.check(groupClaim)
            ? DEFAULT_GROUP_CLAIM
            : groupClaim;
    }

    /**
     * Used for static group authorization.
     *
     * @return array of groups which should have access
     */
    @NonNull
    public List<String> getGroups() {
        return Immutable.of(groups);
    }

    /**
     * Specifies which {@link ModelOperation}s are protected by this {@link AuthRule}.  Any operations not included in
     * the list are not protected by default.
     * @return list of {@link ModelOperation}s for which this {@link AuthRule} should apply.
     */
    @NonNull
    public List<ModelOperation> getOperationsOrDefault() {
        return Immutable.of(Empty.check(operations)
            ? Arrays.asList(ModelOperation.values())
            : operations);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        AuthRule authRule = (AuthRule) object;

        return ObjectsCompat.equals(authStrategy, authRule.authStrategy) &&
                ObjectsCompat.equals(ownerField, authRule.ownerField) &&
                ObjectsCompat.equals(identityClaim, authRule.identityClaim) &&
                ObjectsCompat.equals(groupsField, authRule.groupsField) &&
                ObjectsCompat.equals(groupClaim, authRule.groupClaim) &&
                ObjectsCompat.equals(groups, authRule.groups) &&
                ObjectsCompat.equals(operations, authRule.operations);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                authStrategy,
                ownerField,
                identityClaim,
                groupsField,
                groupClaim,
                groups,
                operations
        );
    }

    @Override
    public String toString() {
        return "AuthRule{" +
                "authStrategy=" + authStrategy +
                ", ownerField='" + ownerField + '\'' +
                ", identityClaim='" + identityClaim + '\'' +
                ", groupsField='" + groupsField + '\'' +
                ", groupClaim='" + groupClaim + '\'' +
                ", groups=" + groups + '\'' +
                ", operations=" + operations + '\'' +
                '}';
    }
}
