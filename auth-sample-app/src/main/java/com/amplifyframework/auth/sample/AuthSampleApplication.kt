package com.amplifyframework.auth.sample

import android.app.Application
import com.amplifyframework.auth.AWSAuthPlugin
import com.amplifyframework.core.Resources

class AuthSampleApplication: Application() {
    private lateinit var plugin: AWSAuthPlugin

    override fun onCreate() {
        super.onCreate()
        plugin = AWSAuthPlugin()
        val configJson = Resources.readJsonResourceFromId(applicationContext, R.raw.amplifyconfiguration)
                .getJSONObject("auth")
                .getJSONObject("plugins")
                .getJSONObject("awsCognitoAuthPlugin")
        plugin.configure(configJson, applicationContext)
    }

    fun getPlugin(): AWSAuthPlugin {
        return plugin
    }
}
