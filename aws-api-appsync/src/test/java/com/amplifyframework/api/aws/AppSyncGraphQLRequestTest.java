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
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.OperationType;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.ModelConfig;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppSyncGraphQLRequestTest {
    /**
     * Verify that owner argument is required for ON_CREATE subscription if ModelOperation.CREATE is specified.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedForOnCreate() throws AmplifyException {
        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_CREATE));
        assertTrue(isOwnerArgumentAdded(OwnerCreate.class, SubscriptionType.ON_CREATE));
    }

    /**
     * Verify that owner argument is required for ON_UPDATE subscription if ModelOperation.UPDATE is specified.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedForOnUpdate() throws AmplifyException {
        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_UPDATE));
        assertTrue(isOwnerArgumentAdded(OwnerUpdate.class, SubscriptionType.ON_UPDATE));
    }

    /**
     * Verify that owner argument is required for ON_DELETE subscription if ModelOperation.DELETE is specified.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentAddedForOnDelete() throws AmplifyException {
        assertTrue(isOwnerArgumentAdded(Owner.class, SubscriptionType.ON_DELETE));
        assertTrue(isOwnerArgumentAdded(OwnerDelete.class, SubscriptionType.ON_DELETE));
    }

    /**
     * Verify owner argument is NOT required if the subscription type is not one of the restricted operations.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedIfOperationNotRestricted() throws AmplifyException {
        assertFalse(isOwnerArgumentAdded(OwnerCreate.class, SubscriptionType.ON_UPDATE));
        assertFalse(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_UPDATE));
        assertFalse(isOwnerArgumentAdded(OwnerDelete.class, SubscriptionType.ON_UPDATE));

        assertFalse(isOwnerArgumentAdded(OwnerCreate.class, SubscriptionType.ON_DELETE));
        assertFalse(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_DELETE));
        assertFalse(isOwnerArgumentAdded(OwnerUpdate.class, SubscriptionType.ON_DELETE));

        assertFalse(isOwnerArgumentAdded(OwnerRead.class, SubscriptionType.ON_CREATE));
        assertFalse(isOwnerArgumentAdded(OwnerUpdate.class, SubscriptionType.ON_CREATE));
        assertFalse(isOwnerArgumentAdded(OwnerDelete.class, SubscriptionType.ON_CREATE));
    }

    /**
     * Verify owner argument is NOT added if authStrategy is not OWNER.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void ownerArgumentNotAddedIfNotOwnerStrategy() throws AmplifyException {
        assertFalse(isOwnerArgumentAdded(Group.class, SubscriptionType.ON_CREATE));
    }

    /**
     * Verify owner argument NOT added for Query or Mutation operations.
     * @throws AmplifyException if a ModelSchema can't be derived from the Model class.
     */
    @Test
    public void verifyOwnerArgumentNotAddedIfNotSubscriptionOperation() throws AmplifyException {
        assertFalse(isOwnerArgumentAdded(Owner.class, QueryType.GET));
        assertFalse(isOwnerArgumentAdded(Owner.class, MutationType.CREATE));
    }

    private boolean isOwnerArgumentAdded(Class<? extends Model> clazz, OperationType operationType)
            throws AmplifyException {
        AppSyncGraphQLRequest<Model> request = AppSyncGraphQLRequest.builder()
                .modelClass(clazz)
                .operationType(operationType)
                .requestOptions(new DefaultGraphQLRequestOptions())
                .responseType(clazz)
                .build();
        if (request.isOwnerArgumentRequired()) {
            request.setOwner("johndoe");
        }
        return "johndoe".equals(request.getVariables().get("owner"));
    }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER) })
    private abstract class Owner implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.CREATE)})
    private abstract class OwnerCreate implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.READ)})
    private abstract class OwnerRead implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.UPDATE)})
    private abstract class OwnerUpdate implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.OWNER, operations = ModelOperation.DELETE)})
    private abstract class OwnerDelete implements Model { }

    @ModelConfig(authRules = { @AuthRule(allow = AuthStrategy.GROUPS)})
    private abstract class Group implements Model { }
}
