import java.util.Properties

plugins {
    alias(libs.plugins.apollo)
    alias(libs.plugins.amplify.android.library)
    alias(libs.plugins.amplify.publishing)
}

fun readVersion() = Properties().run {
    file("../version.properties").inputStream().use { load(it) }
    get("VERSION_NAME").toString()
}

project.setProperty("VERSION_NAME", readVersion())

android {
    namespace = "com.amplifyframework.apollo.appsync"
}

dependencies {
    api(project(":apollo-appsync"))
    api(project(":core"))

    implementation(project(":aws-auth-cognito"))
    implementation(project(":aws-core"))
    implementation(libs.aws.signing)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.kotest.assertions)

    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.core)
    androidTestImplementation(libs.test.kotest.assertions)
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.kotlin.coroutines)
    androidTestImplementation(libs.test.turbine)
}
