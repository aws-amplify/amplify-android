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

package com.amplifyframework.datastore;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.sync.SynchronousDataStore;
import com.amplifyframework.util.GsonFactory;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;

/**
 * Tests support for Hybrid platforms (e.g., Flutter, React Native).
 */
public final class HybridOfflineInstrumentationTest {
    private static final int TIMEOUT_SECONDS = 5;

    private SynchronousHybridBehaviors hybridBehaviors;
    private SynchronousDataStore normalBehaviors;
    private ModelSchema blogOwnerSchema;
    private ModelSchema blogSchema;

    /**
     * Configures an AWSDataStorePlugin which only operates offline (not connected to any remote backend),
     * and is able to warehouse the commentsblog family of models.
     * @throws AmplifyException In a variety of scenarios where setup fails
     */
    @Before
    public void setupPlugin() throws AmplifyException {
        blogOwnerSchema = schemaFrom("schemas/commentsblog/blog-owner.json");
        blogSchema = schemaFrom("schemas/commentsblog/blog.json");
        SchemaProvider schemaProvider = SchemaProvider.of(blogOwnerSchema, blogSchema);

        getApplicationContext().deleteDatabase("AmplifyDatastore.db");
        StrictMode.enable();
        Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));
        HubAccumulator initializationObserver =
            HubAccumulator.create(HubChannel.DATASTORE, InitializationStatus.SUCCEEDED, 1)
                .start();
        AWSDataStorePlugin plugin = AWSDataStorePlugin.builder().modelProvider(schemaProvider).build();
        DataStoreCategory dataStoreCategory = new DataStoreCategory();
        dataStoreCategory.addPlugin(plugin);
        dataStoreCategory.configure(new DataStoreCategoryConfiguration(), getApplicationContext());
        dataStoreCategory.initialize(getApplicationContext());
        initializationObserver.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        hybridBehaviors = SynchronousHybridBehaviors.delegatingTo(plugin);
        normalBehaviors = SynchronousDataStore.delegatingTo(plugin);
    }

    /**
     * A {@link Blog} is a parent of a {@link BlogOwner}.
     *
     * Save BlogOwner as a {@link SerializedModel}, then save a {@link BlogOwner}
     * as a {@link SerializedModel}.
     *
     * Verify that it is possible to query both objects back, by their Java class type.
     *
     * @throws DataStoreException On failure interacting with DataStore
     */
    @Test
    public void saveAssociatedModelsInSerializedForm() throws DataStoreException {
        Map<String, Object> blogOwnerData = new HashMap<>();
        blogOwnerData.put("name", "A seasoned writer");
        blogOwnerData.put("id", "e50ffa8f-783b-4780-89b4-27043ffc35be");

        Map<String, Object> blogData = new HashMap<>();
        blogData.put("id", "39c3c0e6-8726-436e-8cdf-bff38e9a62da");
        blogData.put("name", "A cherished blog");
        blogData.put("owner", SerializedModel.builder()
            .serializedData(Collections.singletonMap("id", blogOwnerData.get("id")))
            .modelSchema(null)
            .build()
        );
        hybridBehaviors.save(SerializedModel.builder()
            .serializedData(blogOwnerData)
            .modelSchema(blogOwnerSchema)
            .build());

        BlogOwner blogOwner = BlogOwner.builder()
            .name((String) blogOwnerData.get("name"))
            .id((String) blogOwnerData.get("id"))
            .build();
        assertJavaValues(blogOwner);

        hybridBehaviors.save(SerializedModel.builder()
            .serializedData(blogData)
            .modelSchema(blogSchema)
            .build());
        assertJavaValues(Blog.builder()
            .name((String) blogData.get("name"))
            .owner(blogOwner)
            .id((String) blogData.get("id"))
            .build()
        );
    }

    /**
     * Save some ordinary Java language models, then try to query
     * them in serialized form.
     * @throws DataStoreException on failure interacting with DataStore
     */
    @Test
    public void queryModelsInSerializedForm() throws DataStoreException {
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Johnny Stewart, Blogger Extraordinaire")
            .id("b01ab515-dc82-4e65-99c9-a4ed8d799bed")
            .build();
        normalBehaviors.save(blogOwner);

        Blog blog = Blog.builder()
            .name("Johnny's Exquisite Blog")
            .owner(blogOwner)
            .build();
        normalBehaviors.save(blog);

        Map<String, Object> serializedBlogOwnerData = new HashMap<>();
        serializedBlogOwnerData.put("id", blogOwner.getId());
        serializedBlogOwnerData.put("name", blogOwner.getName());
        assertEquals(
            Collections.singletonList(SerializedModel.builder()
                .serializedData(serializedBlogOwnerData)
                .modelSchema(blogOwnerSchema)
                .build()),
            hybridBehaviors.list("BlogOwner")
        );

        Map<String, Object> serializedBlogData = new HashMap<>();
        serializedBlogData.put("id", blog.getId());
        serializedBlogData.put("name", blog.getName());
        serializedBlogData.put("owner", SerializedModel.builder()
            .serializedData(serializedBlogOwnerData)
            .modelSchema(blogOwnerSchema)
            .build()
        );
        assertEquals(
            Collections.singletonList(SerializedModel.builder()
                .serializedData(serializedBlogData)
                .modelSchema(blogSchema)
                .build()),
            hybridBehaviors.list("Blog")
        );
    }

    private void assertJavaValues(Model... models) throws DataStoreException {
        Set<Class<? extends Model>> classes = new HashSet<>();
        for (Model model : models) {
            classes.add(model.getClass());
        }
        Set<Model> foundModels = new HashSet<>();
        for (Class<? extends Model> clazz : classes) {
            foundModels.addAll(normalBehaviors.list(clazz));
        }
        Set<Model> expectedModels = new HashSet<>(Arrays.asList(models));
        assertEquals(expectedModels, foundModels);
    }

    private static ModelSchema schemaFrom(String resourcePath) {
        String serializedForm = Assets.readAsString(resourcePath);
        return GsonFactory.instance().fromJson(serializedForm, ModelSchema.class);
    }
}
