package com.atlasv.android.amplifysyncsample

import android.app.Application
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.kotlin.core.Amplify
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.logging.LoggingCategory
import com.atlasv.android.amplify.simpleappsync.response.MergeResponseFactory
import com.atlasv.android.log.HyperLogger
import timber.log.Timber

/**
 * weiping@atlasv.com
 * 2022/12/2
 */
class App : Application() {
    fun currentLanguage(): String {
        return resources.configuration.locales[0].language ?: "en"
    }

    override fun onCreate() {
        super.onCreate()
        HyperLogger.config {
            it.enableLogcat = BuildConfig.DEBUG
        }
//        val AMPLIFY_ENV = "staging"
        val AMPLIFY_ENV = "dev"
        LoggingCategory.LOG_LEVEL = LogLevel.DEBUG
        Amplify.addPlugin(AWSCognitoAuthPlugin())
        Amplify.addPlugin(
            AWSApiPlugin.builder()
                .responseFactory(MergeResponseFactory())
            .configureClient("richman") { builder ->
                builder.addInterceptor { chain ->
                    val originRequest = chain.request()
                    val updatedRequest = originRequest.newBuilder().apply {
                        val prevQueryName = originRequest.headers["x-query-name"]
                        if (prevQueryName != null) {
                            removeHeader("x-query-name")
                            addHeader(
                                "x-query-name",
                                "$prevQueryName-$AMPLIFY_ENV-${currentLanguage()}"
                            )
                        }
                    }.addHeader("x-api-key", BuildConfig.AMPLIFY_CDN_KEY)
                        .addHeader("Host", "graphql.shotcut.app")
                        .url("https://graphql.shotcut.app" + if (AMPLIFY_ENV == "prod") "" else "/$AMPLIFY_ENV")
                        .build()
                    chain.proceed(updatedRequest)
                }
            }
            .build())
        Amplify.configure(applicationContext)
        Timber.d { "Initialized Amplify" }
    }
}