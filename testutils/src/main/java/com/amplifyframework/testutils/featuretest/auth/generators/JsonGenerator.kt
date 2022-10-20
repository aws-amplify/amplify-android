package com.amplifyframework.testutils.featuretest.auth.generators

import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.testutils.featuretest.FeatureTestCase
import com.amplifyframework.testutils.featuretest.auth.generators.authstategenerators.AuthStateJsonGenerator
import com.amplifyframework.testutils.featuretest.auth.generators.testcasegenerators.ResetPasswordTestCaseGenerator
import com.amplifyframework.testutils.featuretest.auth.generators.testcasegenerators.SignInTestCaseGenerator
import com.amplifyframework.testutils.featuretest.auth.generators.testcasegenerators.SignUpTestCaseGenerator

interface SerializableProvider {
    val serializables: List<Any>
}

/**
 * Top level generator for generating Json and writing to the destination directory
 */
object JsonGenerator {
    private val providers: List<SerializableProvider> = listOf(
        AuthStateJsonGenerator,
        ResetPasswordTestCaseGenerator,
        SignUpTestCaseGenerator,
        SignInTestCaseGenerator
    )

    fun generate() {
        providers.forEach { provider ->
            provider.serializables.forEach {
                when (it) {
                    is AuthState -> it.exportJson()
                    is FeatureTestCase -> it.exportJson()
                    else -> {
                        println("Generation of ${it.javaClass.kotlin} is not supported!")
                    }
                }
            }
        }
    }
}

fun main() {
    JsonGenerator.generate()
}
