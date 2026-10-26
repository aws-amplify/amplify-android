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
package com.amplifyframework.core.model

import com.amplifyframework.AmplifyException
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.datastore.DataStoreException.IrRecoverableException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import java.io.File
import java.net.URI
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SchemaRegistryExceptionContractTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val registry = SchemaRegistry.instance()

    @After
    fun clearRegistry() {
        registry.clear()
    }

    @Test
    fun `Java can catch lazy rejection from name and schema overload`() {
        assertJavaCatchCompiles("registry.register(\"LazyModel\", schema)")
    }

    @Test
    fun `Java can catch lazy rejection from schema map overload`() {
        assertJavaCatchCompiles("registry.register(java.util.Collections.singletonMap(\"LazyModel\", schema))")
    }

    @Test
    fun `Java can catch lazy rejection from schema and custom type maps overload`() {
        assertJavaCatchCompiles(
            "registry.register(java.util.Collections.singletonMap(\"LazyModel\", schema), " +
                "java.util.Collections.emptyMap())"
        )
    }

    @Test
    fun `name and schema overload rejects real lazy model schema`() {
        shouldThrow<IrRecoverableException> { registry.register("LazyModel", schema(LazyModel::class.java)) }
    }

    @Test
    fun `schema map overload rejects real lazy model schema`() {
        shouldThrow<IrRecoverableException> { registry.register(mapOf("LazyModel" to schema(LazyModel::class.java))) }
    }

    @Test
    fun `schema and custom type maps overload rejects real lazy model schema`() {
        shouldThrow<IrRecoverableException> {
            registry.register(mapOf("LazyModel" to schema(LazyModel::class.java)), emptyMap())
        }
    }

    @Test
    fun `name and schema overload registers real eager model schema`() {
        val schema = schema(EagerModel::class.java)
        registry.register("EagerModel", schema)
        registry.getModelSchemaForModelClass("EagerModel") shouldBe schema
    }

    @Test
    fun `schema map overload registers real eager model schema`() {
        val schema = schema(EagerModel::class.java)
        registry.register(mapOf("EagerModel" to schema))
        registry.getModelSchemaForModelClass("EagerModel") shouldBe schema
    }

    @Test
    fun `schema and custom type maps overload registers real eager model schema`() {
        val schema = schema(EagerModel::class.java)
        registry.register(mapOf("EagerModel" to schema), emptyMap())
        registry.getModelSchemaForModelClass("EagerModel") shouldBe schema
    }

    private fun schema(modelClass: Class<out Model>): ModelSchema = ModelSchema.builder()
        .name(modelClass.simpleName)
        .modelClass(modelClass)
        .build()

    private fun assertJavaCatchCompiles(call: String) {
        val compiler = requireNotNull(ToolProvider.getSystemJavaCompiler())
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val source = object : SimpleJavaFileObject(
            URI.create("string:///RegistryCatchProbe.java"),
            JavaFileObject.Kind.SOURCE
        ) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = """
                import com.amplifyframework.core.model.SchemaRegistry;
                import com.amplifyframework.core.model.ModelSchema;
                import com.amplifyframework.datastore.DataStoreException.IrRecoverableException;
                public class RegistryCatchProbe {
                    public static void register(SchemaRegistry registry, ModelSchema schema) {
                        try { $call; } catch (IrRecoverableException error) { }
                    }
                }
            """.trimIndent()
        }
        val classpath = listOf(SchemaRegistry::class.java, AmplifyException::class.java)
            .map { File(requireNotNull(it.protectionDomain?.codeSource).location.toURI()).absolutePath }
            .distinct()
            .joinToString(File.pathSeparator)
        compiler.getStandardFileManager(diagnostics, null, null).use { fileManager ->
            val success = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                listOf("-proc:none", "-classpath", classpath, "-d", temporaryFolder.newFolder().absolutePath),
                null,
                listOf(source)
            ).call()
            withClue(diagnostics.diagnostics.joinToString("\n")) { success shouldBe true }
        }
    }

    @ModelConfig(hasLazySupport = true)
    private class LazyModel : Model

    @ModelConfig(hasLazySupport = false)
    private class EagerModel : Model
}
