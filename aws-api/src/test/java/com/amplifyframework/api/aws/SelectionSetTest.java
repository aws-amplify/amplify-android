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
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.ownerauth.OwnerAuth;
import com.amplifyframework.testutils.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class SelectionSetTest {
    /**
     * Test that selection set serialization works as expected.
     * @throws AmplifyException if a ModelSchema can't be derived from Post.class
     */
    @Test
    public void selectionSetSerializesToExpectedValue() throws AmplifyException {
        SelectionSet.Node selectionSet = SelectionSet.fromModelClass(Post.class, 2);
        assertEquals(Resources.readAsString("selection-set-post.txt").trim(), selectionSet.toString().trim());
    }

    /**
     * Test that owner field is added to selection set when a model has an @{link AuthStrategy.OWNER} auth strategy.
     * @throws AmplifyException  if a ModelSchema can't be derived from OwnerAuth.class
     */
    @Test
    public void ownerFieldAddedToSelectionSet() throws AmplifyException {
        SelectionSet.Node selectionSet = SelectionSet.fromModelClass(OwnerAuth.class, 2);
        assertEquals(" {id owner title}", selectionSet.toString());
    }
}
