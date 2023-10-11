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
import com.amplifyframework.datastore.generated.model.HasManyChildPath
import com.amplifyframework.datastore.generated.model.HasOneChild
import com.amplifyframework.datastore.generated.model.Parent
import com.amplifyframework.datastore.generated.model.ParentPath
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class GraphQLLazyUpdateInstrumentationTest {

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
    fun update_with_no_includes() = runTest {
        // GIVEN
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        Amplify.API.mutate(ModelMutation.create(hasOneChild))

        val hasOneChild2 = HasOneChild.builder().content("Child2").build()
        Amplify.API.mutate(ModelMutation.create(hasOneChild2))

        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        val parentResponse = Amplify.API.mutate(ModelMutation.create(parent)).data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // WHEN
        val newParent = parentResponse.copyOfBuilder().parentChildId(hasOneChild2.id).build()
        val request = ModelMutation.update(newParent)
        val updatedParent = Amplify.API.mutate(request).data

        // THEN
        assertEquals(parent.id, updatedParent.id)
        assertEquals(hasOneChild2.id, updatedParent.parentChildId)
        (updatedParent.child as? LazyModelReference)?.fetchModel()?.let {
            assertEquals(hasOneChild2.id, it.id)
            assertEquals(hasOneChild2.content, it.content)
        } ?: fail("Response child was null or not a LazyModelReference")
        (updatedParent.children as? LazyModelList)?.fetchPage()?.let {
            assertEquals(1, it.items.size)
        } ?: fail("Response child was null or not a LazyModelList")

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(hasOneChild2))
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @Test
    fun update_with_includes() = runTest {
        // GIVEN
        val hasOneChild = HasOneChild.builder().content("Child1").build()
        Amplify.API.mutate(ModelMutation.create(hasOneChild))

        val hasOneChild2 = HasOneChild.builder().content("Child2").build()
        Amplify.API.mutate(ModelMutation.create(hasOneChild2))

        val parent = Parent.builder().parentChildId(hasOneChild.id).build()
        val parentResponse = Amplify.API.mutate(ModelMutation.create(parent)).data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // WHEN
        val newParent = parentResponse.copyOfBuilder().parentChildId(hasOneChild2.id).build()
        val request = ModelMutation.update<Parent, ParentPath>(newParent) {
            includes(it.child, it.children)
        }
        val updatedParent = Amplify.API.mutate(request).data

        // THEN
        assertEquals(parent.id, updatedParent.id)
        assertEquals(hasOneChild2.id, updatedParent.parentChildId)
        (updatedParent.child as? LoadedModelReference)?.value?.let {
            assertEquals(hasOneChild2.id, it.id)
            assertEquals(hasOneChild2.content, it.content)
        } ?: fail("Response child was null or not a LoadedModelReference")
        (updatedParent.children as? LoadedModelList)?.let {
            assertEquals(1, it.items.size)
        } ?: fail("Response child was null or not a LoadedModelList")

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasOneChild))
        Amplify.API.mutate(ModelMutation.delete(hasOneChild2))
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @Test
    fun update_without_includes_does_not_remove_relationship() = runTest {
        // GIVEN
        val parent = Parent.builder().build()
        Amplify.API.mutate(ModelMutation.create(parent)).data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // WHEN
        val hasManyChildToUpdate = hasManyChild.copyOfBuilder().content("Child2-Updated").build()
        val request = ModelMutation.update(hasManyChildToUpdate)
        val updatedHasManyChild = Amplify.API.mutate(request).data

        // THEN
        assertEquals(hasManyChild.id, updatedHasManyChild.id)
        assertEquals("Child2-Updated", updatedHasManyChild.content)
        (updatedHasManyChild.parent as? LazyModelReference)?.fetchModel()?.let {
            assertEquals(parent.id, it.id)
        } ?: fail("Response child was null or not a LazyModelReference")

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @Test
    fun update_with_includes_does_not_remove_relationship() = runTest {
        // GIVEN
        val parent = Parent.builder().build()
        Amplify.API.mutate(ModelMutation.create(parent)).data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // WHEN
        val hasManyChildToUpdate = hasManyChild.copyOfBuilder().content("Child2-Updated").build()
        val request = ModelMutation.update<HasManyChild, HasManyChildPath>(hasManyChildToUpdate) {
            includes(it.parent)
        }
        val updatedHasManyChild = Amplify.API.mutate(request).data

        // THEN
        assertEquals(hasManyChild.id, updatedHasManyChild.id)
        assertEquals("Child2-Updated", updatedHasManyChild.content)
        (updatedHasManyChild.parent as? LoadedModelReference)?.value?.let {
            assertEquals(parent.id, it.id)
        } ?: fail("Response child was null or not a LoadedModelReference")

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @Test
    fun update_without_includes_explicit_remove_relationship() = runTest {
        // GIVEN
        val parent = Parent.builder().build()
        Amplify.API.mutate(ModelMutation.create(parent)).data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // WHEN
        val hasManyChildToUpdate = hasManyChild.copyOfBuilder().parent(null).content("Child2-Updated").build()
        val request = ModelMutation.update(hasManyChildToUpdate)
        val updatedHasManyChild = Amplify.API.mutate(request).data

        // THEN
        assertEquals(hasManyChild.id, updatedHasManyChild.id)
        assertEquals("Child2-Updated", updatedHasManyChild.content)
        assertNull((updatedHasManyChild.parent as LoadedModelReference).value)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }

    @Test
    fun update_with_includes_explicit_remove_relationship() = runTest {
        // GIVEN
        val parent = Parent.builder().build()
        Amplify.API.mutate(ModelMutation.create(parent)).data

        val hasManyChild = HasManyChild.builder().content("Child2").parent(parent).build()
        Amplify.API.mutate(ModelMutation.create(hasManyChild))

        // WHEN
        val hasManyChildToUpdate = hasManyChild.copyOfBuilder().parent(null).content("Child2-Updated").build()
        val request = ModelMutation.update<HasManyChild, HasManyChildPath>(hasManyChildToUpdate) {
            includes(it.parent)
        }
        val updatedHasManyChild = Amplify.API.mutate(request).data

        // THEN
        assertEquals(hasManyChild.id, updatedHasManyChild.id)
        assertEquals("Child2-Updated", updatedHasManyChild.content)
        assertNull((updatedHasManyChild.parent as LoadedModelReference).value)

        // CLEANUP
        Amplify.API.mutate(ModelMutation.delete(hasManyChild))
        Amplify.API.mutate(ModelMutation.delete(parent))
    }
}
