package com.amplifyframework.auth.sample

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.amplifyframework.auth.sample.databinding.ActivityConfirmSignUpBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

class ConfirmSignUpActivity : AppCompatActivity() {
    private lateinit var view: ActivityConfirmSignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityConfirmSignUpBinding.inflate(layoutInflater)
        setContentView(view.root)
        val username = intent.getStringExtra("username")!!
        view.submitButton.setOnClickListener { submitConfirmationCode(username) }
    }

    private fun submitConfirmationCode(username: String) {
        runBlocking {
            val code = view.codeEntry.text.toString()
            val plugin = (application as AuthSampleApplication).getPlugin()
            val result = withContext(Dispatchers.IO) {
                Timber.tag("ConfirmSignUp").e("username = ${username}, code=${code}")
                plugin.confirmSignUp(username, code)
            }
            if (result.isSignUpComplete) {
                goToSignIn(this@ConfirmSignUpActivity, username)
            }
        }
    }
}

fun goToConfirmSignUp(source: Activity, username: String) {
    val confirmSignUpIntent = Intent(source, ConfirmSignUpActivity::class.java)
    confirmSignUpIntent.putExtra("username", username)
    source.startActivity(confirmSignUpIntent)
}
