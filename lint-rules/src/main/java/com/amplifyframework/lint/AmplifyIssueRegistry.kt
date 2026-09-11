/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/**
 * The custom checks this project enforces. Lint finds this class through the `Lint-Registry-v2`
 * manifest attribute declared in this module's build script; every new detector must be listed in
 * [issues] to take effect.
 */
class AmplifyIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(
        NoPrintlnDetector.ISSUE
    )

    override val api: Int = CURRENT_API

    override val vendor = Vendor(
        vendorName = "Amplify Android",
        identifier = "com.amplifyframework:lint-rules",
        feedbackUrl = "https://github.com/aws-amplify/amplify-android/issues"
    )
}
