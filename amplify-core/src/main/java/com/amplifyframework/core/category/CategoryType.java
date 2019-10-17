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

package com.amplifyframework.core.category;

/**
 * Enum that declares the various categories
 * of APIs supported by Amplify System.
 */
public enum CategoryType {

    /**
     * Analytics track your app's operational status and customer
     * engagement, recording to an AWS backend service.
     */
    ANALYTICS,

    /**
     * API simplifies interactions with a remove AWS backend via REST
     * And GraphQL operations.
     */
    API,

    /**
     * Hub is an event bus style pub/sub system that is used to
     * communicate state inside and outside of the Amplify framework.
     * This category is expected to operate locally to the device,
     * without talking to AWS backend services, directly.
     */
    HUB,

    /**
     * Logging for troubleshooting of component behaviors during
     * development, or when deployed in production.  This category is
     * expected to operate locally to the device, without talking to AWS
     * backend services, directly.
     */
    LOGGING,

    /**
     * Storage is an interface to a remote repository to store and
     * retrieve instances of domain models. AWS provides several backend
     * systems that are suitable for storage of your data.
     */
    STORAGE
}

