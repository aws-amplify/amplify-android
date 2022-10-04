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
package com.amplifyframework.auth.exceptions.service

import com.amplifyframework.auth.exceptions.ServiceException

/**
 * Could not perform the action because Amplify cannot find the requested resource.
 * @param cause The underlying cause of this exception
 */
open class ResourceNotFoundException(cause: Throwable?) :
    ServiceException(
        "Could not find the requested online resource.",
        "Retry with exponential back-off or check your config file to be sure the endpoint is valid.",
        cause
    )
