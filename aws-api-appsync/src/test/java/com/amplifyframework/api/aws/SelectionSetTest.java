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

package com.amplifyframework.api.aws;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.ownerauth.OwnerAuth;
import com.amplifyframework.testmodels.ownerauth.OwnerAuthExplicit;
import com.amplifyframework.testmodels.parenting.Parent;
import com.amplifyframework.testutils.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class SelectionSetTest {
    /**
     * Test that selection set serialization works as expected.
     * @throws AmplifyException if a ModelSchema can't be derived from Post.class
     */
    @Test
    public void selectionSetSerializesToExpectedValue() throws AmplifyException {
        SelectionSet selectionSet = SelectionSet.builder()
                .modelClass(Post.class)
                .operation(QueryType.GET)
                .requestOptions(new DefaultGraphQLRequestOptions())
                .build();
        assertEquals(Resources.readAsString("selection-set-post.txt"), selectionSet.toString() + "\n");
    }

    /**
     * Test that custom type selection set serialization works as expected.
     * @throws AmplifyException if a ModelSchema can't be derived from Post.class
     */
    @Test
    public void nestedCustomTypeSelectionSetSerializesToExpectedValue() throws AmplifyException {
        SelectionSet selectionSet = SelectionSet.builder()
                .modelClass(Parent.class)
                .operation(QueryType.GET)
                .requestOptions(new DefaultGraphQLRequestOptions())
                .build();
        assertEquals(Resources.readAsString("selection-set-parent.txt"), selectionSet.toString() + "\n");
    }

    /**
     * Test that owner field is added to selection set when a model has an @{link AuthStrategy.OWNER} auth strategy.
     * @throws AmplifyException if a ModelSchema can't be derived from OwnerAuth.class
     */
    @Test
    public void ownerFieldAddedForImplicitOwnerAuth() throws AmplifyException {
        SelectionSet selectionSet = SelectionSet.builder()
                .modelClass(OwnerAuth.class)
                .operation(QueryType.GET)
                .requestOptions(new DefaultGraphQLRequestOptions())
                .build();
        assertEquals(Resources.readAsString("selection-set-ownerauth.txt"), selectionSet.toString() + "\n");
    }

    /**
     * As in {@link #ownerFieldAddedForImplicitOwnerAuth()}, tests that owner field is added to
     * selection set when a model has an {@link AuthStrategy#OWNER} auth strategy, and the owner is
     * implicit. The difference in this test is that the {@link SelectionSet} is built directly
     * from a {@link ModelSchema} instead of an Java model class.
     * @throws AmplifyException on failure to build selection set (not expected)
     */
    @Test
    public void ownerFieldAddedForImplicitOwnerAuthWhenUsingSchema() throws AmplifyException {
        ModelField modelId = ModelField.builder()
            .isRequired(true)
            .targetType("ID")
            .javaClassForValue(String.class)
            .build();
        ModelField title = ModelField.builder()
            .isRequired(true)
            .targetType("String")
            .javaClassForValue(String.class)
            .build();
        Map<String, ModelField> fields = new HashMap<>();
        fields.put("id", modelId);
        fields.put("title", title);

        ModelSchema schema = ModelSchema.builder()
            .name("OwnerAuth")
            .pluralName("OwnerAuths")
            .modelClass(SerializedModel.class)
            .fields(fields)
            .authRules(Collections.singletonList(AuthRule.builder()
                .authStrategy(AuthStrategy.OWNER)
                .identityClaim("cognito:username")
                .ownerField("owner")
                .operations(Arrays.asList(
                    ModelOperation.CREATE,
                    ModelOperation.UPDATE,
                    ModelOperation.DELETE,
                    ModelOperation.READ
                ))
                .build()
            ))
            .build();

        SelectionSet selectionSet = SelectionSet.builder()
            .modelClass(SerializedModel.class) // Note: this is different from the above test.
            .modelSchema(schema) // Note: this test passes an explicit schema, instead of relying on modelClass().
            .operation(QueryType.GET)
            .requestOptions(new DefaultGraphQLRequestOptions())
            .build();
        assertEquals(Resources.readAsString("selection-set-ownerauth.txt"), selectionSet.toString() + "\n");
    }

    /**
     * Test that if owner field is explicitly defined on the model, the selection set is built without any errors.
     * @throws AmplifyException if a ModelSchema can't be derived from OwnerAuth.class
     */
    @Test
    public void ownerFieldNotAddedForExplicitOwnerAuth() throws AmplifyException {
        SelectionSet selectionSet = SelectionSet.builder()
                .modelClass(OwnerAuthExplicit.class)
                .operation(QueryType.GET)
                .requestOptions(new DefaultGraphQLRequestOptions())
                .build();
        assertEquals(Resources.readAsString("selection-set-ownerauth.txt"), selectionSet.toString() + "\n");
    }
}
