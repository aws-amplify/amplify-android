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
package com.amplifyframework.auth.cognito.exceptions.service

import com.amplifyframework.auth.AuthException

/**
 * Software Token MFA is not enabled for the user.
 * @param cause The underlying cause of this exception
 */
open class EnableSoftwareTokenMFAException(cause: Throwable?) :
    AuthException(
        "Unable to enable software token MFA",
        "Enable the software token MFA for the user.",
        cause
    )
