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

package com.amplifyframework.api.aws;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility to create a ISO 8601 compliant timestamps.
 * This utility only created US-locale timestamps. It is intended for
 * use as a protocol utility to talk between computer systems. The
 * timestamp returned by this utility should not be displayed to end
 * users in a UI, as it is not localized.
 */
final class Iso8601Timestamp {
    @SuppressWarnings("checkstyle:all") private Iso8601Timestamp() {}

    static String now() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
        return formatter.format(new Date());
    }
}
