
import com.android.build.gradle.LibraryExtension
import java.net.URI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Apply the publishing plugins
        with(target.pluginManager) {
            apply("signing")
            apply("maven-publish")

            // Other conventions that are applied to published libraries
            apply("amplify.kover")
            apply("amplify.licenses")
            apply("amplify.api")
        }

        target.configureArtifacts()

        // Configure the signing and maven publish plugins
        target.afterEvaluate {
            configureMavenPublishing()
            configureSigning()
            registerPrintPublishedCoordinates()
        }
    }

    // Registers a task that prints the published Maven coordinates (group:artifact:version) for
    // every MavenPublication this project produces, one per line. Reading straight off the
    // publications is authoritative: it reflects the exact coordinates that get published,
    // including modules that override the group (e.g. com.amazonaws) or version, and KMP modules
    // that publish multiple coordinates.
    private fun Project.registerPrintPublishedCoordinates() {
        val publishing = extensions.findByType<PublishingExtension>() ?: return
        tasks.register("printPublishedCoordinates") {
            doLast {
                publishing.publications.withType<MavenPublication>().forEach {
                    println("${it.groupId}:${it.artifactId}:${it.version}")
                }
            }
        }
    }

    // Configure the artifacts that are published
    private fun Project.configureArtifacts() {
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> {
                publishing {
                    singleVariant("release") {
                        withSourcesJar()
                    }
                }
            }
        }
        pluginManager.withPlugin("java-library") {
            extensions.configure<JavaPluginExtension> {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }

    // Configure the publishing extension in the project
    @Suppress("LocalVariableName", "ktlint:standard:property-naming")
    private fun Project.configureMavenPublishing() {
        val POM_GROUP: String by project
        val POM_ARTIFACT_ID: String by project
        val VERSION_NAME: String by project

        group = POM_GROUP
        version = VERSION_NAME

        configure<PublishingExtension> {
            // For KMP projects, publications are created automatically by the KMP plugin
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                // Configure all KMP publications
                publications.withType<MavenPublication>().configureEach {
                    configurePom(this@configureMavenPublishing)
                }
            }

            // For non-KMP projects, create the maven publication manually
            if (!isKotlinMultiplatform) {
                publications {
                    create("maven", MavenPublication::class.java) {
                        artifactId = POM_ARTIFACT_ID

                        pluginManager.withPlugin("com.android.library") {
                            from(components["release"])
                        }
                        pluginManager.withPlugin("java-library") {
                            from(components["java"])
                        }

                        configurePom(this@configureMavenPublishing)
                    }
                }
            }

            repositories {
                maven {
                    name = "ossrh-staging-api"
                    url = if (isReleaseBuild) releaseRepositoryUrl else snapshotRepositoryUrl
                    credentials {
                        username = sonatypeUsername
                        password = sonatypePassword
                    }
                }
            }
        }
    }

    // Configure POM metadata for a publication
    @Suppress("LocalVariableName", "ktlint:standard:property-naming")
    private fun MavenPublication.configurePom(project: Project) {
        pom {
            val POM_NAME: String? by project
            val POM_PACKAGING: String? by project
            val POM_DESCRIPTION: String? by project
            val POM_URL: String? by project
            name.set(POM_NAME)
            packaging = POM_PACKAGING
            description.set(POM_DESCRIPTION)
            url.set(POM_URL)

            scm {
                val POM_SCM_URL: String? by project
                val POM_SCM_CONNECTION: String? by project
                val POM_SCM_DEV_CONNECTION: String? by project
                url.set(POM_SCM_URL)
                connection.set(POM_SCM_CONNECTION)
                developerConnection.set(POM_SCM_DEV_CONNECTION)
            }

            licenses {
                license {
                    val POM_LICENSE_NAME: String? by project
                    val POM_LICENSE_URL: String? by project
                    val POM_LICENSE_DIST: String? by project
                    name.set(POM_LICENSE_NAME)
                    url.set(POM_LICENSE_URL)
                    distribution.set(POM_LICENSE_DIST)
                }
            }

            developers {
                developer {
                    val POM_DEVELOPER_ID: String? by project
                    val POM_DEVELOPER_ORGANIZATION_URL: String? by project
                    id.set(POM_DEVELOPER_ID)
                    organizationUrl.set(POM_DEVELOPER_ORGANIZATION_URL)
                    roles.set(listOf("developer"))
                }
            }
        }
    }

    // Configure artifact signing
    private fun Project.configureSigning() {
        if (hasProperty("signingKeyId")) {
            println("Getting signing info from protected source.")
            extra["signing.keyId"] = findProperty("signingKeyId")
            extra["signing.password"] = findProperty("signingPassword")
            extra["signing.inMemoryKey"] = findProperty("signingInMemoryKey")
        }

        configure<SigningExtension> {
            isRequired = isReleaseBuild && gradle.taskGraph.hasTask("publish")
            if (hasProperty("signing.inMemoryKey")) {
                val signingKey = findProperty("signing.inMemoryKey").toString().replace("\\n", "\n")
                val signingPassword = findProperty("signing.password").toString()
                val keyId = findProperty("signing.keyId").toString()
                useInMemoryPgpKeys(keyId, signingKey, signingPassword)
            }

            // Sign all publications
            val publishingExtension = extensions.findByType(PublishingExtension::class.java)
            publishingExtension?.publications?.configureEach {
                if (this is MavenPublication) {
                    sign(this)
                }
            }
        }
    }

    private val Project.versionName: String
        get() = properties["VERSION_NAME"]!!.toString()

    private val Project.isReleaseBuild: Boolean
        get() = !versionName.contains("SNAPSHOT")

    private val Project.releaseRepositoryUrl: URI
        get() = URI.create(
            getPropertyOrDefault(
                "RELEASE_REPOSITORY_URL",
                "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            )
        )

    private val Project.snapshotRepositoryUrl: URI
        get() = URI.create(
            getPropertyOrDefault(
                "SNAPSHOT_REPOSITORY_URL",
                "https://ossrh-staging-api.central.sonatype.com/content/repositories/snapshots/"
            )
        )

    private val Project.sonatypeUsername: String
        get() = getPropertyOrDefault("SONATYPE_NEXUS_USERNAME", "")

    private val Project.sonatypePassword: String
        get() = getPropertyOrDefault("SONATYPE_NEXUS_PASSWORD", "")

    private fun Project.getPropertyOrDefault(property: String, default: String) = propertyString(property) ?: default

    private fun Project.propertyString(property: String) = properties[property]?.toString()

    private val Project.isKotlinMultiplatform: Boolean
        get() = pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")
}
