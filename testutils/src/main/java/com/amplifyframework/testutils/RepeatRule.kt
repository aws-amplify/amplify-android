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

package com.amplifyframework.testutils

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule to repeatedly run a test
 * usage:
 * ```
 *   @get:Rule
 *   val repeatRule = RepeatRule()
 *
 *   @Test
 *   @Repeat(100)
 *   fun testToBeRepeated() {
 *     ...
 *   }
 * ```
 */
class RepeatRule : TestRule {
    private class RepeatStatement(
        private val statement: Statement,
        private val repeat: Int
    ) :
        Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            for (i in 0 until repeat) {
                statement.evaluate()
            }
        }
    }

    override fun apply(
        statement: Statement,
        description: Description
    ): Statement {
        var result = statement
        val repeat: Repeat = description.getAnnotation(Repeat::class.java) as Repeat
        val times: Int = repeat.value
        result = RepeatStatement(statement, times)
        return result
    }
}

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class Repeat(val value: Int = 1)
