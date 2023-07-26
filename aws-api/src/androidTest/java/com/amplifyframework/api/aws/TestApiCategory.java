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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolsAuthProvider;
import com.amplifyframework.auth.CognitoCredentialsProvider;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

/**
 * A factory to construct instances of {@link ApiCategory}, suitable for test.
 * A user would ordinarily interact with the process-singleton {@link Amplify} facade.
 * But from integration tests, it is easier to get fresh instances of {@link ApiCategory}
 * and to interact with those, each potentially with a different configuration.
 */
final class TestApiCategory {
    private TestApiCategory() {}

    /**
     * Creates an instance of {@link ApiCategory}, using the provided configuration
     * file, referred to by its android resource ID.
     * @return A configured and initialized ApiCategory instance
     */
    @NonNull
    static ApiCategory fromConfiguration(@RawRes int resourceId) throws AmplifyException {
        CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider =
            new DefaultCognitoUserPoolsAuthProvider();
        ApiAuthProviders providers = ApiAuthProviders.builder()
            .awsCredentialsProvider(new CognitoCredentialsProvider())
            .cognitoUserPoolsAuthProvider(cognitoUserPoolsAuthProvider)
            .build();
        AWSApiPlugin plugin = AWSApiPlugin.builder()
            .apiAuthProviders(providers)
            .build();
        ApiCategory apiCategory = new ApiCategory();
        apiCategory.addPlugin(plugin);

        CategoryConfiguration apiConfiguration =
            AmplifyConfiguration.fromConfigFile(getApplicationContext(), resourceId)
                .forCategoryType(CategoryType.API);
        apiCategory.configure(apiConfiguration, getApplicationContext());
        // apiCategory.initialize(...); Doesn't currently contain any logic, so, skip it.
        return apiCategory;
    }
}
