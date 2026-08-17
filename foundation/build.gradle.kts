plugins {
    alias(libs.plugins.amplify.kmp)
    alias(libs.plugins.amplify.publishing)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":annotations"))
            }
        }

        getByName("androidMain") {
            dependencies {
                implementation(libs.androidx.security)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.test.kotest.assertions)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.test.mockk)
                implementation(project(":testutils"))
            }
        }
    }

    compilerOptions {
        optIn.add("kotlin.contracts.ExperimentalContracts")
    }
}
