package com.amplifyframework.auth.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthSignUpStep.CONFIRM_SIGN_UP_STEP
import com.amplifyframework.auth.result.step.AuthSignUpStep.DONE
import com.amplifyframework.auth.sample.databinding.ActivitySignUpBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private lateinit var view: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(view.root)
        view.submitButton.setOnClickListener { submitSignIn() }
    }

    private fun submitSignIn() {
        runBlocking {
            val username = view.username.text.toString()
            val password = view.password.text.toString()
            val options = AuthSignUpOptions.builder()
                .userAttribute(AuthUserAttributeKey.email(), view.email.text.toString())
                .build()
            val plugin = (application as AuthSampleApplication).getPlugin()
            val result: AuthSignUpResult = withContext(Dispatchers.IO) {
                plugin.signUp(username, password, options)
            }
            when (result.nextStep.signUpStep) {
                DONE -> goToSignIn(this@SignUpActivity, username)
                CONFIRM_SIGN_UP_STEP -> goToConfirmSignUp(this@SignUpActivity, username)
            }
        }
    }
}
