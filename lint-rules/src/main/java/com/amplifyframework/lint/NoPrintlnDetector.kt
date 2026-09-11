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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * Reports calls that write directly to the console instead of going through a Logger.
 */
class NoPrintlnDetector : Detector(), SourceCodeScanner {

    // Lint consults its method name index for these, rather than visiting every call in the source.
    override fun getApplicableMethodNames() = listOf("print", "println")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Tests may print freely; this rule is about library code.
        if (context.isTestSource) return

        val evaluator = context.evaluator
        val printsToConsole = evaluator.isMemberInClass(method, KOTLIN_CONSOLE_CLASS) ||
            evaluator.isMemberInSubClassOf(method, PRINT_STREAM_CLASS, false)

        // A method merely named print/println is fine; only the console ones are reported.
        if (!printsToConsole) return

        context.report(ISSUE, node, context.getLocation(node), MESSAGE)
    }

    companion object {
        // kotlin.io.println and print compile to static members of this class.
        private const val KOTLIN_CONSOLE_CLASS = "kotlin.io.ConsoleKt"
        private const val PRINT_STREAM_CLASS = "java.io.PrintStream"

        private const val MESSAGE = "Do not print to the console in library code. Use a `Logger` instead."

        val ISSUE: Issue = Issue.create(
            id = "AmplifyPrintln",
            briefDescription = "Console printing in library code",
            // The trailing backslashes are Lint markup meaning "join with the next line". Without
            // them Lint treats each source newline as a hard break and wraps the text mid-sentence.
            explanation = """
                Writing to stdout or stderr bypasses the logging configuration, so consumers of \
                the SDK cannot filter, redirect, or disable the output. Emit the message through a \
                `Logger` instead, which routes it to the configured logging plugin.
            """,
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(NoPrintlnDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
