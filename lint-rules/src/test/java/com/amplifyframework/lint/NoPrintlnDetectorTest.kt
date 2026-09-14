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
import org.junit.Assert.assertTrue
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
        ).expect(
            // The one golden assertion, locking the message and the reported location. The other
            // tests use expectErrorCount because exact report text is brittle across Lint versions.
            """
            src/com/amplifyframework/test/test.kt:4: Error: Do not print to the console in library code. Use a Logger instead. [AmplifyPrintln]
                println("hello")
                ~~~~~~~~~~~~~~~~
            1 error
            """
        )
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
    fun `flags System out println from kotlin`() {
        check(
            kotlin(
                """
                package com.amplifyframework.test

                fun doWork() {
                    System.out.println("hello")
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

    @Test
    fun `honors kotlin Suppress`() {
        check(
            kotlin(
                """
                package com.amplifyframework.test

                @Suppress("AmplifyPrintln")
                fun doWork() {
                    println("hello")
                }
                """
            ).indented()
        ).expectClean()
    }

    // This case cannot use the check() helper: reaching the detector's isTestSource guard needs the
    // driver configured, because otherwise Lint only hands test sources to detectors that declare
    // Scope.TEST_SOURCES, and this one declares JAVA_FILE_SCOPE. The build sets checkTestSources
    // false, so the guard is a safety net for the day that changes.
    @Test
    fun `allows println in test sources`() {
        lint()
            .files(
                // TestLintTask treats the project's top-level `test` directory as a unit test
                // source folder, which is what makes isTestSource true for this file. Java rather
                // than Kotlin because kotlin.io.println does not resolve outside src/.
                java(
                    "test/com/amplifyframework/test/WorkerTest.java",
                    """
                    package com.amplifyframework.test;

                    class WorkerTest {
                        void testDoWork() {
                            System.out.println("hello");
                        }
                    }
                    """
                ).indented()
            )
            .issues(NoPrintlnDetector.ISSUE)
            .allowMissingSdk()
            .configureDriver { driver -> driver.checkTestSources = true }
            .run()
            .expectClean()
    }

    // Catches the issue being dropped from the registry. It cannot catch a future detector being
    // left unregistered, since it only checks the issue it names.
    @Test
    fun `registry exposes the issue`() {
        assertTrue(AmplifyIssueRegistry().issues.contains(NoPrintlnDetector.ISSUE))
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
