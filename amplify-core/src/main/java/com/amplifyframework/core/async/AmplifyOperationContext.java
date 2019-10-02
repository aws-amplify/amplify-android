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

package com.amplifyframework.core.async;

import android.support.annotation.NonNull;

import java.util.UUID;

/**
 * A context object passed in the HubPayload of all events dispatched to the Hub by an
 * AmplifyOperation. This object can be used to filter on a particular operation.
 */
public class AmplifyOperationContext {
    /**
     * The id of the operation
     */
    private UUID operationId;

    /**
     * The Request used to instantiate the operation
     */
    private AmplifyOperationRequest request;

    public AmplifyOperationContext(@NonNull final UUID operationId,
                                   @NonNull final AmplifyOperationRequest request) {
        this.operationId = operationId;
        this.request = request;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public void setOperationId(@NonNull final UUID operationId) {
        this.operationId = operationId;
    }

    public AmplifyOperationRequest getRequest() {
        return request;
    }

    public void setRequest(@NonNull final AmplifyOperationRequest request) {
        this.request = request;
    }
}
