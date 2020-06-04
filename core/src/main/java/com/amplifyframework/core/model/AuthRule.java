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

import java.util.Arrays;
import java.util.Objects;

/**
 * {@link AuthRule} is used define an authorization rule for who can access and operate against a
 * {@link Model} or a {@link ModelField}.
 *
 * @see <a href="https://docs.amplify.aws/cli/graphql-transformer/directives#auth")>GraphQL Transformer @auth directive
 * documentation.</a>
 */
public final class AuthRule {
    private final AuthStrategy allow;
    private final String ownerField;
    private final String identityClaim;
    private final String groupClaim;
    private final String[] groups;
    private final String groupsField;
    private final ModelOperation[] operations;

    /**
     * Constructor to create an {@link AuthRule} from an {@link com.amplifyframework.core.model.annotations.AuthRule}
     * annotation.
     * @param authRule an {@link com.amplifyframework.core.model.annotations.AuthRule} annotation.
     */
    public AuthRule(com.amplifyframework.core.model.annotations.AuthRule authRule) {
        this.allow = authRule.allow();
        this.ownerField = authRule.ownerField();
        this.identityClaim = authRule.identityClaim();
        this.groupClaim = authRule.groupClaim();
        this.groups = authRule.groups();
        this.groupsField = authRule.groupsField();
        this.operations = authRule.operations();
    }

    /**
     * Returns the type of strategy for this {@link AuthRule}.
     * @return the type of strategy for this {@link AuthRule}
     */
    public AuthStrategy allow() {
        return this.allow;
    }

    /**
     * Used for owner authorization.  Defaults to "owner" when using AuthStrategy.OWNER.
     *
     * @return name of a {@link ModelField} of type String which specifies the user which should have access
     */
    public String ownerField() {
        return this.ownerField;
    }

    /**
     * Used to specify a custom claim.  Defaults to "username" when using AuthStrategy.OWNER.
     *
     * @return identity claim
     */
    public String identityClaim() {
        return this.identityClaim;
    }

    /**
     * Used to specify a custom claim.   Defaults to "cognito:groups" when using AuthStrategy.GROUPS.
     *
     * @return group claim
     */
    public String groupClaim() {
        return this.groupClaim;
    }

    /**
     * Used for static group authorization.
     *
     * @return array of groups which should have access
     */
    public String[] groups() {
        return this.groups;
    }

    /**
     * Used for dynamic group authorization.  Defaults to "groups" when using AuthStrategy.GROUPS.
     *
     * @return name of a {@link ModelField} of type String or array of Strings which specifies a group or list of groups
     * which should have access.
     */
    public String groupsField() {
        return this.groupsField;
    }

    /**
     * Specifies which {@link ModelOperation}s are protected by this {@link AuthRule}.  Any operations not included in
     * the list are not protected by default.
     * @return list of {@link ModelOperation}s for which this {@link AuthRule} should apply.
     */
    public ModelOperation[] operations() {
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

        return allow == authRule.allow &&
                Objects.equals(ownerField, authRule.ownerField) &&
                Objects.equals(identityClaim, authRule.identityClaim) &&
                Objects.equals(groupClaim, authRule.groupClaim) &&
                Arrays.equals(groups, authRule.groups) &&
                Objects.equals(groupsField, authRule.groupsField) &&
                Arrays.equals(operations, authRule.operations);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(allow, ownerField, identityClaim, groupClaim, groupsField);
        result = 31 * result + Arrays.hashCode(groups);
        result = 31 * result + Arrays.hashCode(operations);
        return result;
    }

    @Override
    public String toString() {
        return "AuthRuleImpl{" +
                "allow=" + allow +
                ", ownerField='" + ownerField + '\'' +
                ", identityClaim='" + identityClaim + '\'' +
                ", groupClaim='" + groupClaim + '\'' +
                ", groups=" + Arrays.toString(groups) +
                ", groupsField='" + groupsField + '\'' +
                ", operations=" + Arrays.toString(operations) +
                '}';
    }
}
