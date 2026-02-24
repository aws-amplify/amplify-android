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

package com.amplifyframework.testutils.rules

import java.net.UnknownHostException
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * TestRule that repeats tests if they fail with an UnknownHostException. This exception occurs randomly on Device Farm
 * devices, and repeating the test may allow it to pass on a subsequent attempt.
 */
class CanaryTestRule : TestRule {
    override fun apply(statement: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                repeat(MAX_ATTEMPTS) { attempt ->
                    try {
                        statement.evaluate()
                        return // Success
                    } catch (e: Throwable) {
                        if (!e.isCausedByUnknownHost() || attempt + 1 == MAX_ATTEMPTS) {
                            throw e
                        }
                        val delay = INITIAL_DELAY_MS * 2.0.pow(attempt)
                        Thread.sleep(delay.inWholeMilliseconds)
                    }
                }
            }
        }
    }

    private tailrec fun Throwable.isCausedByUnknownHost(): Boolean {
        val cause = this.cause
        return when {
            this is UnknownHostException -> true
            cause == null || cause == this -> false
            else -> cause.isCausedByUnknownHost()
        }
    }

    companion object {
        // One initial attempt and up to 2 retries, for 3 total attempts.
        private const val MAX_ATTEMPTS = 3
        private val INITIAL_DELAY_MS = 2.seconds // Doubles on each attempt
    }
}
