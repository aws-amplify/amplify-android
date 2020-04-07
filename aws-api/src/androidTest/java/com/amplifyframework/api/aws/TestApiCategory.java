/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
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
        Context context = getApplicationContext();
        ApiCategory apiCategory = new ApiCategory();
        apiCategory.addPlugin(new AWSApiPlugin());
        CategoryConfiguration apiConfiguration =
            AmplifyConfiguration.fromConfigFile(context, resourceId)
                .forCategoryType(CategoryType.API);
        apiCategory.configure(apiConfiguration, context);
        // apiCategory.initialize(context); Doesn't currently contain any logic, so, skip it.
        return apiCategory;
    }
}
