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

package com.amplifyframework.core.model.annotations;

import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.ModelOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link AuthRule} is used define an authorization rule for who can access and operate against a
 * {@link com.amplifyframework.core.model.Model} or a {@link com.amplifyframework.core.model.ModelField}.
 *
 * The {@link RetentionPolicy#RUNTIME} annotation is added to retain {@link AuthRule} at runtime for the reflection
 * capabilities to work in order to check if this annotation is present within a {@link ModelConfig} annotation.
 *
 * {@link ElementType#ANNOTATION_TYPE} annotation is added to indicate {@link AuthRule} annotation can be used only on
 * other annotation types (i.e. {@link ModelConfig} or {@link ModelField}).
 *
 * @see <a href="https://docs.amplify.aws/cli/graphql-transformer/directives#auth">GraphQL Transformer @auth directive
 * documentation.</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface AuthRule {
    /**
     * Defines the strategy for this rule.  This is the only required field.
     *
     * @return AuthStrategy for this {@link AuthRule}
     */
    AuthStrategy allow();

    /**
     * Used for owner authorization.  Defaults to "owner" when using AuthStrategy.OWNER.
     *
     * @return name of a {@link ModelField} of type String which specifies the user which should have access
     */
    String ownerField() default "";

    /**
     * Used to specify a custom claim.  Defaults to "username" when using AuthStrategy.OWNER.
     *
     * @return identity claim
     */
    String identityClaim() default "";

    /**
     * Used to specify a custom claim.   Defaults to "cognito:groups" when using AuthStrategy.GROUPS.
     *
     * @return group claim
     */
    String groupClaim() default "";

    /**
     * Used for static group authorization.
     *
     * @return array of groups which should have access
     */
    String[] groups() default {};

    /**
     * Used for dynamic group authorization.  Defaults to "groups" when using AuthStrategy.GROUPS.
     *
     * @return name of a {@link ModelField} of type String or array of Strings which specifies a group or list of groups
     * which should have access.
     */
    String groupsField() default "";

    /**
     * Specifies which {@link ModelOperation}s are protected by this {@link AuthRule}.  Any operations not included in
     * the list are not protected by default.
     * @return list of {@link ModelOperation}s for which this {@link AuthRule} should apply.
     */
    ModelOperation[] operations() default {};
}
