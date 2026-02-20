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

        commonTest {
            dependencies {
                implementation(libs.test.kotest.assertions)
                implementation(project(":testutils"))
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.test.mockk)
            }
        }
    }

    compilerOptions {
        optIn.add("kotlin.contracts.ExperimentalContracts")
    }
}
