package com.amplifyframework.auth.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.result.step.AuthSignInStep.DONE
import com.amplifyframework.auth.sample.databinding.ActivitySignInBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SignInActivity : AppCompatActivity() {
    private lateinit var view: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(view.root)
        view.username.setText(intent.getStringExtra("username"))
        view.submitButton.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        runBlocking {
            val username = view.username.text.toString()
            val password = view.password.text.toString()
            val plugin = (application as AuthSampleApplication).getPlugin()
            val result = withContext(Dispatchers.IO) {
                plugin.signIn(username, password, NoSignInOptions())
            }
            when (result.nextStep.signInStep) {
                DONE -> goToLandingPage(this@SignInActivity, "Sign in complete.")
                else -> goToLandingPage(this@SignInActivity, "Unhandled step: ${result.nextStep.signInStep}")
            }
        }
    }

    class NoSignInOptions: AuthSignInOptions()
}

fun goToSignIn(source: Activity, username: String) {
    val intent = Intent(source, SignInActivity::class.java)
    intent.putExtra("username", username)
    source.startActivity(intent)
}
