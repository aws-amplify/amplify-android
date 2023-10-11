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

package com.amplifyframework.api.aws;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.CustomTypeField;
import com.amplifyframework.core.model.CustomTypeSchema;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.lazy.PostPath;
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

import static com.amplifyframework.core.model.ModelPropertyPathKt.includes;
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

    /**
     * Test generating SelectionSet for ModelSchema that nests CustomTypeSchema.
     * @throws AmplifyException if a ModelSchema can't be derived from OwnerAuth.class
     */
    @Test
    public void nestedSerializedModelAndSerializedCustomType() throws AmplifyException {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();

        CustomTypeField phoneCountryField = CustomTypeField.builder()
                .targetType("String")
                .isRequired(true)
                .build();
        CustomTypeField phoneAreaField = CustomTypeField.builder()
                .targetType("String")
                .isRequired(true)
                .build();
        CustomTypeField phoneNumber = CustomTypeField.builder()
                .targetType("String")
                .isRequired(true)
                .build();

        Map<String, CustomTypeField> phoneFields = new HashMap<>();
        phoneFields.put("country", phoneCountryField);
        phoneFields.put("area", phoneAreaField);
        phoneFields.put("number", phoneNumber);

        CustomTypeSchema phoneSchema = CustomTypeSchema.builder()
                .fields(phoneFields)
                .name("Phone")
                .pluralName("Phones")
                .build();

        CustomTypeField addressCityField = CustomTypeField.builder()
                .targetType("String")
                .isRequired(true)
                .build();
        CustomTypeField addressPhoneNumberField = CustomTypeField.builder()
                .targetType("Phone")
                .isCustomType(true)
                .build();
        CustomTypeField addressLine1Field = CustomTypeField.builder()
                .targetType("String")
                .isRequired(true)
                .build();
        CustomTypeField addressLine2Field = CustomTypeField.builder()
                .targetType("String")
                .build();
        CustomTypeField addressStateField = CustomTypeField.builder()
                .targetType("String")
                .isRequired(true)
                .build();
        CustomTypeField addressPostalCodeField = CustomTypeField.builder()
                .targetType("String")
                .isRequired(true)
                .build();
        Map<String, CustomTypeField> addressFields = new HashMap<>();
        addressFields.put("city", addressCityField);
        addressFields.put("phoneNumber", addressPhoneNumberField);
        addressFields.put("line1", addressLine1Field);
        addressFields.put("line2", addressLine2Field);
        addressFields.put("state", addressStateField);
        addressFields.put("postalCode", addressPostalCodeField);
        CustomTypeSchema addressSchema = CustomTypeSchema.builder()
                .fields(addressFields)
                .name("Address")
                .pluralName("Addresses")
                .build();

        ModelField personAddressField = ModelField.builder()
                .name("address")
                .isCustomType(true)
                .targetType("Address")
                .isRequired(true)
                .build();
        ModelField personNameField = ModelField.builder()
                .name("name")
                .targetType("String")
                .isRequired(true)
                .build();
        ModelField personPhonesField = ModelField.builder()
                .name("phoneNumbers")
                .targetType("Phone")
                .isCustomType(true)
                .isArray(true)
                .build();

        Map<String, ModelField> personFields = new HashMap<>();
        personFields.put("address", personAddressField);
        personFields.put("name", personNameField);
        personFields.put("phones", personPhonesField);

        ModelSchema personSchema = ModelSchema.builder()
                .fields(personFields)
                .name("Person")
                .pluralName("People")
                .modelClass(SerializedModel.class)
                .build();

        // Register custom type schema for usage in SelectionSet
        schemaRegistry.register("Address", addressSchema);
        schemaRegistry.register("Phone", phoneSchema);

        // Register model schema for usage in SelectionSet
        schemaRegistry.register("Person", personSchema);

        SelectionSet selectionSet = SelectionSet.builder()
                .modelClass(SerializedModel.class)
                .modelSchema(personSchema)
                .operation(QueryType.GET)
                .requestOptions(new DefaultGraphQLRequestOptions())
                .build();
        String result = selectionSet.toString();
        assertEquals(Resources.readAsString("selection-set-nested-serialized-model-serialized-custom-type.txt"),
                result + "\n");
    }

    /**
     * Test generating SelectionSet for nested ModelSchema using SerializedModel.
     * @throws AmplifyException if a ModelSchema can't be derived from postSchema
     */
    @Test
    public void nestedSerializedModel() throws AmplifyException {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        ModelField blogModelId = ModelField.builder()
                .isRequired(true)
                .targetType("ID")
                .build();
        ModelField blogName = ModelField.builder()
                .isRequired(true)
                .targetType("String")
                .build();
        Map<String, ModelField> blogFields = new HashMap<>();
        blogFields.put("id", blogModelId);
        blogFields.put("name", blogName);

        ModelSchema blogSchema = ModelSchema.builder()
                .name("Blog")
                .pluralName("Blogs")
                .modelClass(SerializedModel.class)
                .fields(blogFields)
                .build();

        ModelField postModelId = ModelField.builder()
                .isRequired(true)
                .targetType("ID")
                .build();
        ModelField postTitle = ModelField.builder()
                .isRequired(true)
                .targetType("String")
                .build();
        ModelField postBlog = ModelField.builder()
                .isRequired(true)
                .targetType("Blog")
                .isModel(true)
                .build();
        Map<String, ModelField> postFields = new HashMap<>();
        postFields.put("id", postModelId);
        postFields.put("title", postTitle);
        postFields.put("blog", postBlog);

        Map<String, ModelAssociation> associations = new HashMap<>();
        associations.put("blog", ModelAssociation.builder()
                .name("BelongsTo")
                .targetName("blogId")
                .associatedType("Blog")
                .build());

        ModelSchema postSchema = ModelSchema.builder()
                .name("Post")
                .pluralName("Posts")
                .modelClass(SerializedModel.class)
                .fields(postFields)
                .associations(associations)
                .build();

        schemaRegistry.register("Blog", blogSchema);
        schemaRegistry.register("Post", postSchema);

        SelectionSet selectionSet = SelectionSet.builder()
                .modelClass(SerializedModel.class)
                .modelSchema(postSchema)
                .operation(QueryType.SYNC)
                .requestOptions(new JustIDGraphQLRequestOptions())
                .build();

        assertEquals(Resources.readAsString("selection-set-post-nested.txt"), selectionSet.toString() + "\n");
    }

    /**
     * Test that selection set serialization works as expected for lazy types.
     * @throws AmplifyException if a ModelSchema can't be derived from Post.class
     */
    @Test
    public void simpleLazyTypesSerializeToExpectedValue() throws AmplifyException {
        PostPath postPath = com.amplifyframework.testmodels.lazy.Post.rootPath;
        SelectionSet selectionSet = SelectionSet.builder()
                .modelClass(com.amplifyframework.testmodels.lazy.Post.class)
                .operation(QueryType.GET)
                .requestOptions(new ApiGraphQLRequestOptions(0))
                .includeRelationships(
                        includes(postPath.getBlog().getPosts(), postPath.getComments().getPost())
                )
                .build();
        assertEquals(Resources.readAsString("selection-set-lazy-with-includes.txt"), selectionSet.toString() + "\n");
    }

    /**
     * Test that selection set serialization works as expected for lazy types without includes.
     * @throws AmplifyException if a ModelSchema can't be derived from Post.class
     */
    @Test
    public void simpleLazyTypesSerializeToExpectedValueWithEmptyIncludes() throws AmplifyException {
        SelectionSet selectionSet = SelectionSet.builder()
                .modelClass(com.amplifyframework.testmodels.lazy.Post.class)
                .operation(QueryType.GET)
                .requestOptions(new ApiGraphQLRequestOptions(0))
                .includeRelationships(includes())
                .build();
        assertEquals(Resources.readAsString("selection-set-lazy-empty-includes.txt"), selectionSet.toString() + "\n");
    }
}
