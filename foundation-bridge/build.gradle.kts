plugins {
    alias(libs.plugins.amplify.kmp)
    alias(libs.plugins.amplify.publishing)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":foundation"))
                api(libs.aws.credentials)
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
