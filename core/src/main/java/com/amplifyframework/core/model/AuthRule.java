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

import android.text.TextUtils;
import androidx.core.util.ObjectsCompat;

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
    private final AuthStrategy authStrategy;
    private final String ownerField;
    private final String identityClaim;
    private final String groupClaim;
    private final List<String> groups;
    private final String groupsField;
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
    public AuthStrategy getAuthStrategy() {
        return this.authStrategy;
    }

    /**
     * Used for owner authorization.  Defaults to "owner" when using AuthStrategy.OWNER.
     *
     * @return name of a {@link ModelField} of type String which specifies the user which should have access
     */
    public String getOwnerFieldOrDefault() {
        return TextUtils.isEmpty(this.ownerField) ? "owner" : this.ownerField;
    }

    /**
     * Used to specify a custom claim.  Defaults to "username" when using AuthStrategy.OWNER.
     *
     * @return identity claim
     */
    public String getIdentityClaim() {
        return this.identityClaim;
    }

    /**
     * Used to specify a custom claim.   Defaults to "cognito:groups" when using AuthStrategy.GROUPS.
     *
     * @return group claim
     */
    public String getGroupClaim() {
        return this.groupClaim;
    }

    /**
     * Used for static group authorization.
     *
     * @return array of groups which should have access
     */
    public List<String> getGroups() {
        return this.groups;
    }

    /**
     * Used for dynamic group authorization.  Defaults to "groups" when using AuthStrategy.GROUPS.
     *
     * @return name of a {@link ModelField} of type String or array of Strings which specifies a group or list of groups
     * which should have access.
     */
    public String getGroupsFieldOrDefault() {
        return TextUtils.isEmpty(this.groupsField) ? "groups" : this.groupsField;
    }

    /**
     * Specifies which {@link ModelOperation}s are protected by this {@link AuthRule}.  Any operations not included in
     * the list are not protected by default.
     * @return list of {@link ModelOperation}s for which this {@link AuthRule} should apply.
     */
    public List<ModelOperation> getOperationsOrDefault() {
        if (this.operations == null || this.operations.isEmpty()) {
            return Arrays.asList(
                    ModelOperation.CREATE,
                    ModelOperation.UPDATE,
                    ModelOperation.DELETE,
                    ModelOperation.READ);
        }
        return this.operations;
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
                ObjectsCompat.equals(groupClaim, authRule.groupClaim) &&
                ObjectsCompat.equals(groups, authRule.groups) &&
                ObjectsCompat.equals(groupsField, authRule.groupsField) &&
                ObjectsCompat.equals(operations, authRule.operations);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(authStrategy, ownerField, identityClaim, groupClaim, groups, groupsField, operations);
    }

    @Override
    public String toString() {
        return "AuthRule{" +
                "authStrategy=" + authStrategy +
                ", ownerField='" + ownerField + '\'' +
                ", identityClaim='" + identityClaim + '\'' +
                ", groupClaim='" + groupClaim + '\'' +
                ", groups=" + groups + '\'' +
                ", groupsField='" + groupsField + '\'' +
                ", operations=" + operations + '\'' +
                '}';
    }
}
