/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.api.aws.test.R
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.includes
import com.amplifyframework.datastore.generated.model.HasManyChild
import com.amplifyframework.datastore.generated.model.HasOneChild
import com.amplifyframework.datastore.generated.model.Parent
import com.amplifyframework.datastore.generated.model.ParentPath
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class GraphQLLazyDeleteInstrumentationTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUp() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val config = AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfigurationlazy)
            Amplify.addPlugin(AWSApiPlugin())
            Amplify.configure(config, context)
        }
    }

    @Test
    fun delete_with_no_includes() = runTest {
        // GIVEN
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        Amplify.API.mutate(ModelMutation.create(hasOneChild))

        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        Amplify.API.mutate(ModelMutation.create(parent)).data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // WHEN
        val request = ModelMutation.delete(parent)
        val updatedParent = Amplify.API.mutate(request).data

        // THEN
        assertEquals(parent.id, updatedParent.id)
        assertEquals(hasOneChild.id, updatedParent.parentChildId)
        (updatedParent.child as? LazyModelReference)?.fetchModel()?.let {
            assertEquals(hasOneChild.id, it.id)
            assertEquals(hasOneChild.content, it.content)
        } ?: fail("Response child was null or not a LazyModelReference")
        (updatedParent.children as? LazyModelList)?.fetchPage()?.let {
            assertEquals(1, it.items.size)
        } ?: fail("Response child was null or not a LazyModelList")

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
    }

    @Test
    fun delete_with_includes() = runTest {
        // GIVEN
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        Amplify.API.mutate(ModelMutation.create(hasOneChild))

        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        Amplify.API.mutate(ModelMutation.create(parent)).data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // WHEN
        val request = ModelMutation.delete<Parent, ParentPath>(parent) {
            includes(it.child, it.children)
        }
        val updatedParent = Amplify.API.mutate(request).data

        // THEN
        assertEquals(parent.id, updatedParent.id)
        assertEquals(hasOneChild.id, updatedParent.parentChildId)
        (updatedParent.child as? LoadedModelReference)?.value?.let {
            assertEquals(hasOneChild.id, it.id)
            assertEquals(hasOneChild.content, it.content)
        } ?: fail("Response child was null or not a LoadedModelReference")
        (updatedParent.children as? LoadedModelList)?.let {
            assertEquals(1, it.items.size)
        } ?: fail("Response child was null or not a LoadedModelList")

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
    }
}
