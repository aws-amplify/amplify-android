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

package com.amplifyframework.auth.cognito.options;

import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class APIOptionsContractTest {

    @SuppressWarnings("serial")
    HashMap<String, String> metadata = new HashMap<String, String>() {
        {
            put("testKey", "testValue");
        }
    };

    @Test
    public void testCognitoOptions() {
        AWSCognitoAuthResendUserAttributeConfirmationCodeOptions
                resendUserAttributeConfirmationCodeOptions =
                AWSCognitoAuthResendUserAttributeConfirmationCodeOptions.builder()
                        .metadata(metadata).build();
        Assert.assertEquals(resendUserAttributeConfirmationCodeOptions.getMetadata(), metadata);

        AWSCognitoAuthConfirmResetPasswordOptions confirmResetPasswordOptions =
                AWSCognitoAuthConfirmResetPasswordOptions.builder().metadata(metadata).build();
        Assert.assertEquals(confirmResetPasswordOptions.getMetadata(), metadata);

        AWSCognitoAuthConfirmSignInOptions confirmSignInOptions =
                AWSCognitoAuthConfirmSignInOptions.builder().metadata(metadata).build();
        Assert.assertEquals(confirmSignInOptions.getMetadata(), metadata);

        AWSCognitoAuthConfirmSignUpOptions confirmSignUpOptions =
                AWSCognitoAuthConfirmSignUpOptions.builder().clientMetadata(metadata).build();
        Assert.assertEquals(confirmSignUpOptions.getClientMetadata(), metadata);

        AWSCognitoAuthResendSignUpCodeOptions resendSignUpCodeOptions =
                AWSCognitoAuthResendSignUpCodeOptions.builder().metadata(metadata).build();
        Assert.assertEquals(resendSignUpCodeOptions.getMetadata(), metadata);

        AWSCognitoAuthResetPasswordOptions resetPasswordOptions =
                AWSCognitoAuthResetPasswordOptions.builder().metadata(metadata).build();
        Assert.assertEquals(resetPasswordOptions.getMetadata(), metadata);

        AWSCognitoAuthSignInOptions signInOptions =
                AWSCognitoAuthSignInOptions.builder().metadata(metadata).build();
        Assert.assertEquals(signInOptions.getMetadata(), metadata);

        AWSCognitoAuthSignOutOptions signOutOptions =
                AWSCognitoAuthSignOutOptions.builder().browserPackage("chrome").build();
        Assert.assertEquals(signOutOptions.getBrowserPackage(), "chrome");

        ArrayList<AuthUserAttribute> attributes = new ArrayList<>();
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.email(), "my@email.com"));
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "+15551234567"));
        AWSCognitoAuthSignUpOptions signUpOptions =
                AWSCognitoAuthSignUpOptions.builder().clientMetadata(metadata)
                        .userAttributes(attributes).build();
        Assert.assertEquals(signUpOptions.getClientMetadata(), metadata);
        Assert.assertEquals(signUpOptions.getUserAttributes(), attributes);

        AWSCognitoAuthUpdateUserAttributeOptions updateUserAttributeOptions =
                AWSCognitoAuthUpdateUserAttributeOptions.builder().metadata(metadata).build();
        Assert.assertEquals(updateUserAttributeOptions.getMetadata(), metadata);

        AWSCognitoAuthUpdateUserAttributesOptions updateUserAttributesOptions =
                AWSCognitoAuthUpdateUserAttributesOptions.builder().metadata(metadata).build();
        Assert.assertEquals(updateUserAttributesOptions.getMetadata(), metadata);

        List<String> scopes = Arrays.asList("name");
        AWSCognitoAuthWebUISignInOptions webUISignInOptions =
                AWSCognitoAuthWebUISignInOptions.builder().browserPackage("chrome")
                        .scopes(scopes).build();
        Assert.assertEquals(webUISignInOptions.getBrowserPackage(), "chrome");
        Assert.assertEquals(webUISignInOptions.getScopes(), scopes);

        FederateToIdentityPoolOptions federateToIdentityPoolOptions =
                FederateToIdentityPoolOptions.builder().developerProvidedIdentityId("test-idp")
                        .build();
        Assert.assertEquals(federateToIdentityPoolOptions
                .getDeveloperProvidedIdentityId(), "test-idp");
    }
}
