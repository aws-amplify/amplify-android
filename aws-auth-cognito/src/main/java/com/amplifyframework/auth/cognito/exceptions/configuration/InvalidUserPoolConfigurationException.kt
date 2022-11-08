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
package com.amplifyframework.auth.cognito.exceptions.configuration

import com.amplifyframework.auth.exceptions.ConfigurationException

/**
 * Could not perform the action because the user pool is not configured or
 * is configured incorrectly.
 */
class InvalidUserPoolConfigurationException : ConfigurationException(
    message = "The user pool configuration is missing or invalid.",
    recoverySuggestion = "Please check the user pool configuration in your amplifyconfiguration.json file."
)
