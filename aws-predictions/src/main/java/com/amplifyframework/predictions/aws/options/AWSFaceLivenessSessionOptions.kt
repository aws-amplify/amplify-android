package com.amplifyframework.predictions.aws.options

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AWSCredentialsProvider
import com.amplifyframework.predictions.options.FaceLivenessSessionOptions

@InternalAmplifyApi
open class AWSFaceLivenessSessionOptions private constructor(
    val credentialsProvider: AWSCredentialsProvider<AWSCredentials>?
) : FaceLivenessSessionOptions() {
    companion object {
        @JvmStatic fun builder() = Builder()
        @JvmStatic fun defaults() = builder().build()
    }

    class Builder : FaceLivenessSessionOptions.Builder<Builder>() {
        var credentialsProvider: AWSCredentialsProvider<AWSCredentials>? = null
            private set

        fun credentialsProvider(credentialsProvider: AWSCredentialsProvider<AWSCredentials>) =
            apply { this.credentialsProvider = credentialsProvider }

        override fun getThis() = this

        override fun build() = AWSFaceLivenessSessionOptions(credentialsProvider)
    }
}
