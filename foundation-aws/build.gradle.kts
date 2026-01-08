plugins {
    alias(libs.plugins.amplify.kmp)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":foundation"))
                api(libs.aws.credentials)
            }
        }
    }
}
