/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.options;

/**
 * Enum to represent AuthFlowType in AWS SDK.
 */

public enum AuthFlowType {
    /**
     * type for USER_SRP_AUTH.
     */
    USER_SRP_AUTH("USER_SRP_AUTH"),
    /**
     * type for CUSTOM_AUTH.
     *
     * @deprecated Replaced by AuthFlowType.CUSTOM_AUTH_WITHOUT_SRP
     */
    @Deprecated
    CUSTOM_AUTH("CUSTOM_AUTH"),

    /**
     * type for CUSTOM_AUTH THAT STARTS WITH SRP.
     */
    CUSTOM_AUTH_WITH_SRP("CUSTOM_AUTH_WITH_SRP"),

    /**
     * type for CUSTOM_AUTH.
     */
    CUSTOM_AUTH_WITHOUT_SRP("CUSTOM_AUTH_WITHOUT_SRP"),
    /**
     * type for USER_PASSWORD_AUTH.
     */
    USER_PASSWORD_AUTH("USER_PASSWORD_AUTH");
    private final String value;

    AuthFlowType(String value) {
        this.value = value;
    }
}
