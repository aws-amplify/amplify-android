package com.amplifyframework.auth.sample

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.amplifyframework.auth.sample.databinding.ActivityLandingPageBinding

class LandingPageActivity : AppCompatActivity() {
    private lateinit var view: ActivityLandingPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityLandingPageBinding.inflate(layoutInflater)
        view.message.text = intent.getStringExtra("message")
        setContentView(view.root)
    }
}

fun goToLandingPage(origin: Activity, message: String) {
    val intent = Intent(origin, LandingPageActivity::class.java)
    intent.putExtra("message", message)
    origin.startActivity(intent)
}
