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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Empty;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    private final AuthorizationType authProvider;
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
        this.authProvider = AuthorizationType.from(authRule);
        this.ownerField = authRule.ownerField();
        this.identityClaim = authRule.identityClaim();
        this.groupClaim = authRule.groupClaim();
        this.groups = Arrays.asList(authRule.groups());
        this.groupsField = authRule.groupsField();
        this.operations = Arrays.asList(authRule.operations());
    }

    /**
     * Construct the AuthRule object from the builder.
     * For internal use only.
     */
    private AuthRule(@NonNull AuthRule.Builder builder) {
        this.authStrategy = builder.authStrategy;
        this.authProvider = builder.authProvider;
        this.ownerField = builder.ownerField;
        this.identityClaim = builder.identityClaim;
        this.groupClaim = builder.groupClaim;
        this.groups = builder.groups;
        this.groupsField = builder.groupsField;
        this.operations = builder.operations;
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    @NonNull
    public static AuthRule.Builder builder() {
        return new AuthRule.Builder();
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
     * Returns the auth provider for this {@link AuthRule}.
     * @return the auth provider for this {@link AuthRule}
     */
    public AuthorizationType getAuthProvider() {
        return AuthorizationType.DEFAULT.equals(authProvider) ?
                                                        authStrategy.getDefaultAuthProvider() :
                                                        authProvider;
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
        // Default to returning a list of every ModelOperation:
        // CREATE, READ, UPDATE, DELETE
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

        return authStrategy == authRule.authStrategy &&
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

    /**
     * Builder class for {@link AuthRule}.
     */
    public static final class Builder {
        private AuthStrategy authStrategy;
        private AuthorizationType authProvider;
        private String ownerField;
        private String identityClaim;
        private String groupClaim;
        private List<String> groups;
        private String groupsField;
        private List<ModelOperation> operations = new ArrayList<>();

        /**
         * Sets the auth strategy of this rule.
         * @param authStrategy AuthStrategy is the type of auth strategy to use.
         * @return Current builder instance.
         */
        @NonNull
        public AuthRule.Builder authStrategy(@NonNull AuthStrategy authStrategy) {
            this.authStrategy = Objects.requireNonNull(authStrategy);
            return this;
        }

        /**
         * Sets the auth provider of this rule.
         * @param authProvider The name of the auth provider.
         * @return Current builder instance.
         */
        @NonNull
        public AuthRule.Builder authProvider(@Nullable AuthorizationType authProvider) {
            this.authProvider = Objects.requireNonNull(authProvider);
            return this;
        }

        /**
         * Sets the owner field of this rule.
         * @param ownerField OwnerField is the owner authorization.
         * @return Current builder instance.
         */
        @NonNull
        public AuthRule.Builder ownerField(@NonNull String ownerField) {
            this.ownerField = Objects.requireNonNull(ownerField);
            return this;
        }

        /**
         * Sets the identity claim of this rule.
         * @param identityClaim IdentityClaim specifies a custom claim.
         * @return Current builder instance.
         */
        @NonNull
        public AuthRule.Builder identityClaim(@NonNull String identityClaim) {
            this.identityClaim = Objects.requireNonNull(identityClaim);
            return this;
        }

        /**
         * Sets the group claim of this rule.
         * @param groupClaim GroupClaim specified a custom claim.
         * @return Current builder instance.
         */
        @NonNull
        public AuthRule.Builder groupClaim(@NonNull String groupClaim) {
            this.groupClaim = Objects.requireNonNull(groupClaim);
            return this;
        }
        
        /**
         * Sets the groups this rule applies to.
         * @param groups Groups is static group authorization.
         * @return Current builder instance.
         */
        @NonNull
        public AuthRule.Builder groups(@NonNull List<String> groups) {
            this.groups = Objects.requireNonNull(groups);
            return this;
        }

        /**
         * Sets the groupsField of this rule.
         * @param groupsField GroupsField is for dynamic group authorization.
         * @return Current builder instance.
         */
        @NonNull
        public AuthRule.Builder groupsField(@NonNull String groupsField) {
            this.groupsField = Objects.requireNonNull(groupsField);
            return this;
        }

        /**
         * Sets the operations allowed for this rule.
         * @param operations Operations specifies which {@link ModelOperation}s are protected by this {@link AuthRule}.
         * @return Current builder instance.
         */
        @NonNull
        public AuthRule.Builder operations(@NonNull List<ModelOperation> operations) {
            this.operations = Objects.requireNonNull(operations);
            return this;
        }

        /**
         * Builds an immutable AuthRule instance using
         * builder object.
         * @return AuthRule instance
         */
        @NonNull
        public AuthRule build() {
            Objects.requireNonNull(this.authStrategy);
            return new AuthRule(this);
        }
    }
}
