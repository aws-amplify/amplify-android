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

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class NoPrintlnDetectorTest {

    private fun check(vararg files: TestFile): TestLintResult = lint()
        .files(*files)
        .issues(NoPrintlnDetector.ISSUE)
        .allowMissingSdk()
        .run()

    @Test
    fun `flags kotlin println`() {
        check(
            kotlin(
                """
                package com.amplifyframework.test

                fun doWork() {
                    println("hello")
                }
                """
            ).indented()
        ).expectErrorCount(1).expectContains("AmplifyPrintln")
    }

    @Test
    fun `flags kotlin print`() {
        check(
            kotlin(
                """
                package com.amplifyframework.test

                fun doWork() {
                    print("hello")
                }
                """
            ).indented()
        ).expectErrorCount(1)
    }

    @Test
    fun `flags java System out println`() {
        check(
            java(
                """
                package com.amplifyframework.test;

                class Worker {
                    void doWork() {
                        System.out.println("hello");
                    }
                }
                """
            ).indented()
        ).expectErrorCount(1)
    }

    @Test
    fun `allows a user-defined function named println`() {
        check(
            kotlin(
                """
                package com.amplifyframework.test

                private fun println(message: String) = Unit

                fun doWork() {
                    println("hello")
                }
                """
            ).indented()
        ).expectClean()
    }

    @Test
    fun `honors SuppressLint`() {
        check(
            SUPPRESS_LINT_STUB,
            java(
                """
                package com.amplifyframework.test;

                import android.annotation.SuppressLint;

                class Worker {
                    @SuppressLint("AmplifyPrintln")
                    void doWork() {
                        System.out.println("hello");
                    }
                }
                """
            ).indented()
        ).expectClean()
    }

    private companion object {
        // allowMissingSdk() means the real android.jar is absent, so @SuppressLint would not
        // resolve without a stub.
        private val SUPPRESS_LINT_STUB: TestFile = java(
            """
            package android.annotation;

            public @interface SuppressLint {
                String[] value();
            }
            """
        ).indented()
    }
}
