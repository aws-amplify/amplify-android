plugins {
    alias(libs.plugins.amplify.kmp)
    alias(libs.plugins.amplify.publishing)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":foundation"))
                api(project.dependencies.platform(libs.aws.bom))
                api(libs.aws.credentials)
                implementation(libs.aws.config)
                implementation(libs.aws.http)
                implementation(libs.aws.smithy.http.kmp)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotest.assertions)
                implementation(libs.test.kotlin.coroutines)
            }
        }
    }
}
